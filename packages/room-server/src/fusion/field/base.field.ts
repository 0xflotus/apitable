import { CellFormatEnum, Field, ICellValue, IField } from '@apitable/core';
import { IFieldValidatorInterface } from 'fusion/i.field.validator.interface';
import { ApiException, ApiTipId } from 'shared/exception';
import { IFieldRoTransformOptions, IFieldValue, IFieldVoTransformOptions } from 'shared/interfaces';
import { IFieldTransformInterface } from '../i.field.transform.interface';

export abstract class BaseField implements IFieldTransformInterface, IFieldValidatorInterface {

  validate(fieldValue: IFieldValue, field: IField, extra?: { [key: string]: string }) {
    if (fieldValue === null) return;
  }

  // eslint-disable-next-line require-await
  async roTransform(fieldValue: IFieldValue, field: IField, options?: IFieldRoTransformOptions): Promise<ICellValue> {
    return fieldValue as ICellValue;
  }

  voTransform(cellValue: ICellValue, field: IField, {
    cellFormat,
    store
  }: IFieldVoTransformOptions): IFieldValue {
    if (cellFormat === CellFormatEnum.STRING) {
      return Field.bindContext(field, store.getState()).cellValueToApiStringValue(cellValue);
    }
    return Field.bindContext(field, store.getState()).cellValueToApiStandardValue(cellValue);
  }

  throwException(field: IField, tipId: ApiTipId, extra?: { [key: string]: string }) {
    throw ApiException.tipError(tipId, extra);
  }

  getSetFieldAttrChangesets(datasheetId: string, field: IField, store: any, extras?: { deleteBrotherField?: boolean }) {
    return null;
  }

}
