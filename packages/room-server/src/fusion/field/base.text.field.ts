import { ICellValue, IField, SegmentType } from '@apitable/core';
import { isString } from 'class-validator';
import { BaseField } from 'fusion/field/base.field';
import { IFieldValue } from 'shared/interfaces';

export abstract class BaseTextField extends BaseField {
  validate(fieldValue: IFieldValue, field: IField, extra?: { [key: string]: string }) {
    if (fieldValue === null) return;
    if (!isString(fieldValue)) {
      this.throwException(field, 'api_param_singletext_field_type_error', extra);
    }
  }

  // eslint-disable-next-line require-await
  async roTransform(fieldValue: IFieldValue, field: IField): Promise<ICellValue> {
    return [
      {
        text: fieldValue.toString(),
        type: SegmentType.Text,
      },
    ];
  }
}
