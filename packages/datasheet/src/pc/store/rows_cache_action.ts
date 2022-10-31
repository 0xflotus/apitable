import { ActionConstants, ICacheTemporaryView, IJOTActionPayload, Selectors, StoreActions, visibleRowsBaseCacheManage } from '@apitable/core';

type IUpdateCacheAction = StoreActions.IUpdateFieldPermissionMapAction | ICacheTemporaryView | IJOTActionPayload;
export function rowsCacheAction({ getState }) {
  return next => (action: IUpdateCacheAction) => {
    const state = getState();
    switch(action.type) {
      // Update fieldPermission
      case ActionConstants.UPDATE_FIELD_PERMISSION_MAP: {
        visibleRowsBaseCacheManage.updateVisibleRowsBaseCacheData = {
          datasheetId: action.datasheetId,
        };
      } break;
      // Update filter information for mirror
      case ActionConstants.CACHE_TEMPORARY_VIEW: {
        const mirrorInfo = Selectors.getMirrorSourceInfo(state, action.mirrorId);
        if (!mirrorInfo) {
          return;
        }
        visibleRowsBaseCacheManage.updateVisibleRowsBaseCacheData = {
          datasheetId: mirrorInfo.datasheetId,
          viewIds: [mirrorInfo.viewId],
          mirrorId: action.mirrorId
        };
      } break;
      // jot action apply
      case ActionConstants.DATASHEET_JOT_ACTION: {
        visibleRowsBaseCacheManage.updateVisibleRowsBaseCache(action, state);
      } break;
    }
    return next(action);
  };
}
