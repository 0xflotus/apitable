import { IReduxState } from '../../exports/store';
import { FieldType, IEmailField, IField } from 'types/field_types';
import { DatasheetActions } from '../datasheet';
import { TextBaseField } from './text_base_field';

export class EmailField extends TextBaseField {
  constructor(public override field: IEmailField, public override state: IReduxState) {
    super(field, state);
  }

  override get apiMetaProperty() {
    return null;
  }

  static defaultProperty() {
    return null;
  }

  static createDefault(fieldMap: { [fieldId: string]: IField }): IEmailField {
    return {
      id: DatasheetActions.getNewFieldId(fieldMap),
      name: DatasheetActions.getDefaultFieldName(fieldMap),
      type: FieldType.Email,
      property: this.defaultProperty(),
    };
  }
}
