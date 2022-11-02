import { IField } from '@apitable/core';
import { Injectable, OnApplicationBootstrap } from '@nestjs/common';
import { BaseUnEditableField } from 'fusion/field/base.un.editable.field';
import { IFieldValue } from 'shared/interfaces';
import { FieldManager } from '../field.manager';

@Injectable()
export class LookUpField extends BaseUnEditableField implements OnApplicationBootstrap {
  onApplicationBootstrap(): any {
    FieldManager.setService(LookUpField.name, this);
  }

  validate(fieldValue: IFieldValue, field: IField) {
    this.throwException(field, 'api_params_lookup_can_not_operate');
  }
}
