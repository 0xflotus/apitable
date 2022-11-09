import produce from 'immer';
import * as actions from '../../../shared/store/action_constants';
import { IShareInfo, IShareInfoAction } from '../../../../exports/store/interfaces';

const defaultShareInfo = {};

export const share = produce((shareInfoDraft: IShareInfo = defaultShareInfo, action: IShareInfoAction) => {
  switch (action.type) {
    case actions.SET_SHARE_INFO: {
      return {
        ...shareInfoDraft,
        ...action.payload
      };
    }

    default:
      return shareInfoDraft;
  }
});
