import { Injectable } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { FieldType, IAlarmUser, truncateText } from '@apitable/core';
import { InjectLogger } from '../../common';
import { DatasheetRecordAlarmEntity } from '../../../database/entities/datasheet.record.alarm.entity';
import { RecordAlarmStatus } from 'shared/enums/record.alarm.enum';
import { isEmpty } from 'lodash';
import { RecordMap } from '../../../database/interfaces';
import { QueueSenderService } from 'shared/services/queue/queue.sender.service';
import { CommandService } from 'database/services/command/command.service';
import { DatasheetMetaService } from 'database/services/datasheet/datasheet.meta.service';
import { DatasheetRecordService } from 'database/services/datasheet/datasheet.record.service';
import { DatasheetService } from 'database/services/datasheet/datasheet.service';
import { Logger } from 'winston';
import { DatasheetRecordAlarmBaseService } from 'database/services/alarm/datasheet.record.alarm.base.service';

@Injectable()
export class SchedTaskService {
  constructor(
    @InjectLogger() private readonly logger: Logger,
    private readonly queueSenderService: QueueSenderService,
    private readonly commandService: CommandService,
    private readonly datasheetService: DatasheetService,
    private readonly datasheetMetaService: DatasheetMetaService,
    private readonly recordService: DatasheetRecordService,
    private readonly recordAlarmService: DatasheetRecordAlarmBaseService,
  ) { }

  @Cron('0,30 * * * * *')
  async scheduleScanActivatedRecordAlarmJob() {
    const profiler = this.logger.startTimer();
    this.logger.info('Start scanning activated record alarms');

    const activatedRecordAlarms = await this.recordAlarmService.getCurrentActivatedRecordAlarms(35);
    if (isEmpty(activatedRecordAlarms)) {
      profiler.done({ message: 'Finished scanning activated record alarms, no alarms found' });
      return;
    }

    const involvedAlarmIds = activatedRecordAlarms.map(alarm => alarm.alarmId);
    this.recordAlarmService.batchUpdateStatusOfRecordAlarms(involvedAlarmIds, RecordAlarmStatus.PROCESSING);

    const involvedRecordIdsMap = activatedRecordAlarms.reduce<Map<string, string[]>>((acc, cur: DatasheetRecordAlarmEntity) => {
      if (!acc.has(cur.dstId)) {
        acc.set(cur.dstId, []);
      }
      acc.get(cur.dstId).push(cur.recordId);
      return acc;
    }, new Map<string, string[]>());

    const involvedDstIds = Array.from(involvedRecordIdsMap.keys());
    const recordMaps = await Promise.all(involvedDstIds.map((dstId: string) => {
      const involvedRecordIds = involvedRecordIdsMap.get(dstId);
      return this.recordService.getRecordsByDstIdAndRecordIds(dstId, involvedRecordIds);
    }));

    const mergedRecordMaps = recordMaps.reduce<RecordMap>((acc, cur: RecordMap) => {
      Object.keys(cur).forEach((recordId: string) => {
        acc[recordId] = cur[recordId];
      });
      return acc;
    }, {});

    const metaMap = await this.datasheetMetaService.getMetaMapByDstIds(involvedDstIds, true);
    const datasheetPacks = await this.datasheetService.getTinyBasePacks(involvedRecordIdsMap);
    const store = this.commandService.fillTinyStore(datasheetPacks);

    const enqueuedAlarmIds = [];
    activatedRecordAlarms.forEach((alarm: DatasheetRecordAlarmEntity) => {
      const dstMeta = metaMap[alarm.dstId];
      const relatedRecord = mergedRecordMaps[alarm.recordId];
      if (!relatedRecord || !dstMeta) return;

      if (!relatedRecord.recordMeta || !relatedRecord.recordMeta.fieldExtraMap) return;

      const fieldExtraMap = relatedRecord.recordMeta.fieldExtraMap[alarm.fieldId];
      if (!fieldExtraMap) return;

      const alarmUsers = fieldExtraMap['alarm']['alarmUsers'];
      if (isEmpty(alarmUsers)) return;

      const receiverUnitIds = alarmUsers.reduce<string[]>((acc, cur: IAlarmUser) => {
        if (cur.type === 'field') {
          const memberField = dstMeta.fieldMap[cur.data];
          if (memberField.type !== FieldType.Member) {
            return acc;
          }

          const memberFieldValue = relatedRecord.data[memberField.id] as string[];
          if (isEmpty(memberFieldValue)) {
            return acc;
          }

          memberFieldValue.forEach((memberId: string) => acc.push(memberId));
        } else if (cur.type === 'member') {
          acc.push(cur.data);
        }
        return acc;
      }, []);

      if (isEmpty(receiverUnitIds)) return;

      const recordTitle = this.recordService.getRecordTitle(relatedRecord, dstMeta, store);
      const viewId = dstMeta.views[0].id;

      const message = {
        nodeId: alarm.dstId,
        spaceId: alarm.spaceId,
        body: {
          extras: {
            recordTitle: truncateText(recordTitle),
            taskExpireAt: relatedRecord.data[alarm.fieldId],
            recordId: alarm.recordId,
            viewId: viewId,
          }
        },
        templateId: 'task_reminder',
        toUnitId: receiverUnitIds,
        fromUserId: 0, // means from system
      };

      enqueuedAlarmIds.push(alarm.alarmId);
      this.queueSenderService.sendMessage('vikadata.api.notification.exchange', 'notification.message', message);
      this.logger.info(`Alarm ${alarm.alarmId} is enqueued`);
    });
    this.recordAlarmService.batchUpdateStatusOfRecordAlarms(enqueuedAlarmIds, RecordAlarmStatus.DONE);

    profiler.done({ message: `Finished scanning activated record alarms, enqueued ${enqueuedAlarmIds.length}/${involvedAlarmIds.length}` });
  }
}
