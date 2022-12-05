import { IAttachmentValue, Strings, t } from '@apitable/core';
import { DownloadOutlined } from '@apitable/icons';
import { message } from 'antd';
import classNames from 'classnames';
// @ts-ignore
import { SubscribeGrade, SubscribeLabel } from 'enterprise';
import { Message } from 'pc/components/common';
import { FC, useState } from 'react';
import styles from './style.module.less';
import { bulkDownload } from './util';

interface IBulkDownloadProps {
  files: IAttachmentValue[];
  datasheetId: string;
  className?: string;
}

export const BulkDownload: FC<IBulkDownloadProps> = ({ files, className, datasheetId }) => {
  const [loading, setLoading] = useState(false);

  return (
    <div className={classNames(styles.download, className)}>
      <div
        className={styles.btn}
        onClick={async() => {
          if (loading) {
            return;
          }
          setLoading(true);
          Message.loading({
            content: t(Strings.downloading_attachments),
            duration: 0
          });

          try {
            await bulkDownload(files);
          } catch (error: any) {
            message.error({ content: error.toString() });
          }
          Message.destroy();
          setLoading(false);
        }}
      >
        <DownloadOutlined currentColor size={14} />
        <div style={{ marginRight: 2 }} />
        {t(Strings.download_all)}
      </div>
      <div className={styles.suffix}>
        <SubscribeLabel grade={SubscribeGrade.Silver} />
      </div>
    </div>
  );
};
