import { AutomationTriggerTypeEntity } from '../entities/automation.trigger.type.entity';
import { EntityRepository, Repository } from 'typeorm';

@EntityRepository(AutomationTriggerTypeEntity)
export class AutomationTriggerTypeRepository extends Repository<AutomationTriggerTypeEntity> {
  /**
   * Query the only trigger type with service slug and trigger endpoint
   */
  getTriggerTypeByServiceSlugAndEndpoints(endpoints: string[], serviceSlug: string): Promise<{
    triggerTypeId: string,
    endpoint: string,
    serviceSlug: string,
  }[]> {

    return this.query(
      `
    SELECT
      trigger_type_id triggerTypeId,
      endpoint,
      vas.slug serviceSlug
    FROM
      ${this.manager.connection.options.entityPrefix}automation_trigger_type att
      JOIN ${this.manager.connection.options.entityPrefix}automation_service vas ON vas.service_id = att.service_id
        AND vas.slug = ?
    WHERE
      att.is_deleted = 0
      AND att.endpoint IN (?)
    `,
      [serviceSlug, endpoints]);
  }
}
