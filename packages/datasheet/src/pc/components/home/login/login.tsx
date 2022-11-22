import { LinkButton, useThemeColors } from '@apitable/components';
import { ApiInterface, AutoTestID, ConfigConstant, isPrivateDeployment, Navigation, Strings, t } from '@apitable/core';
import { DingdingFilled, FeishuFilled, QqFilled, WechatFilled } from '@apitable/icons';
import { configResponsive, useResponsive, useToggle } from 'ahooks';
import { ScreenSize } from 'pc/components/common/component_display';
import { Router } from 'pc/components/route_manager/router';
import { useResponsive as useCustomResponsive, useUserRequest } from 'pc/hooks';
import { getEnvVariables, isMobileApp } from 'pc/utils/env';
import Trigger from 'rc-trigger';
import * as React from 'react';
import { FC, useState } from 'react';
import { useSelector } from 'react-redux';
import { dingdingLogin, feishuLogin, OtherLogin, qqLogin, wechatLogin } from '../other_login';
import { isWecomFunc } from '../social_platform';
import { IdentifyingCodeLogin, ISubmitRequestParam } from './identifying_code_login';
import { PasswordLogin } from './password_login';
import { SSOLogin } from './sso_login';
import styles from './style.module.less';
import { WecomLoginBtn } from './wecom_login_btn';

export interface ILoginProps {
  afterLogin?(data: string, loginMode: ConfigConstant.LoginMode): void;
}

export const polyfillMode = (mode: string | null) => {
  if (mode == null || !isNaN(Number(mode))) {
    return null;
  }

  return mode;
};

export const Login: FC<ILoginProps> = ({ afterLogin }) => {
  const { ACCOUNT_RESET_PASSWORD_VISIBLE, LOGIN_SSO_VISIBLE, LOGIN_DEFAULT_ACCOUNT_TYPE, LOGIN_DEFAULT_VERIFY_TYPE } = getEnvVariables();
  const colors = useThemeColors();

  const toggleLoginModBtnVisible = !LOGIN_DEFAULT_ACCOUNT_TYPE || LOGIN_SSO_VISIBLE;

  const commonDefaultMod = polyfillMode(localStorage.getItem('vika_login_mod')) || LOGIN_DEFAULT_VERIFY_TYPE;
  const defaultMod = LOGIN_SSO_VISIBLE ? ConfigConstant.SSO_LOGIN : commonDefaultMod;
  // commonPrev Just save both password or captcha login
  const [mod, setMod] = useState(defaultMod);

  const { loginOrRegisterReq } = useUserRequest();
  configResponsive({
    large: 1023.98,
  });
  const responsive = useResponsive();
  const { screenIsAtMost } = useCustomResponsive();
  const isMobile = screenIsAtMost(ScreenSize.md);
  const [isPopupVisible, { toggle: popupVisibleToggle }] = useToggle(false);
  const isWecom = useSelector(state => state.space.envs?.weComEnv?.enabled || isWecomFunc());
  const changeLoginMod = () => {
    let currentMod = 'identifying_code';
    if (LOGIN_SSO_VISIBLE) {
      currentMod = mod === ConfigConstant.PASSWORD_LOGIN ? ConfigConstant.SSO_LOGIN : ConfigConstant.PASSWORD_LOGIN;
    } else {
      currentMod = mod === ConfigConstant.IDENTIFY_CODE_LOGIN ? ConfigConstant.PASSWORD_LOGIN : ConfigConstant.IDENTIFY_CODE_LOGIN;
    }
    setMod(currentMod);
    localStorage.setItem('vika_login_mod', currentMod.toString());
  };
  const submitRequest = React.useCallback(
    (data: ISubmitRequestParam) => {
      const loginData: ApiInterface.ISignIn = {
        areaCode: data.areaCode,
        username: data.account,
        type: data.type,
        credential: data.credential,
        data: data.nvcVal,
        mode: data.mode,
      };
      return loginOrRegisterReq(loginData, data.type);
    },
    [loginOrRegisterReq],
  );

  const goResetPwd = () => {
    Router.push(Navigation.RESET_PWD);
  };

  const modConfig = React.useMemo(() => {
    let modTitleText = '';
    let changeModText = '';
    let loginComponent: null | React.ReactNode = null;
    switch (mod) {
      case ConfigConstant.IDENTIFY_CODE_LOGIN:
        modTitleText = t(Strings.verification_code_login);
        loginComponent = <IdentifyingCodeLogin submitRequest={submitRequest} />;
        changeModText = t(Strings.password_login);
        break;
      case ConfigConstant.PASSWORD_LOGIN:
        modTitleText = t(Strings.password_login);
        loginComponent = <PasswordLogin submitRequest={submitRequest} />;
        changeModText = LOGIN_SSO_VISIBLE ? t(Strings.sso_login) : t(Strings.verification_code_login);
        break;
      case ConfigConstant.SSO_LOGIN:
        modTitleText = t(Strings.sso_login);
        loginComponent = <SSOLogin submitRequest={submitRequest} />;
        changeModText = t(Strings.password_login);
        break;
      default:
        break;
    }
    return { modTitleText, loginComponent, changeModText };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [submitRequest, mod]);

  const { modTitleText, loginComponent, changeModText } = modConfig;

  const loginOtherPopup = (
    <div className={styles.loginOtherPopup} onClick={() => popupVisibleToggle()}>
      <div className={styles.loginOtherItem} onClick={() => wechatLogin()}>
        <WechatFilled size={16} color={colors.rc04} />
        <span>{t(Strings.wechat)}</span>
      </div>
      <div className={styles.loginOtherItem} onClick={() => dingdingLogin()}>
        <DingdingFilled size={16} color={colors.rc02} />
        <span>{t(Strings.dingtalk)}</span>
      </div>
      <div className={styles.loginOtherItem} onClick={() => feishuLogin()}>
        <FeishuFilled size={16} />
        <span>{t(Strings.lark)}</span>
      </div>
      <div className={styles.loginOtherItem} onClick={() => qqLogin()}>
        <QqFilled size={16} color={colors.rc03} />
        <span>{t(Strings.qq)}</span>
      </div>
    </div>
  );

  return (
    <>
      {// Privatised and mobile not shown
        !isPrivateDeployment() && responsive.large && !isMobileApp() && (
          <>
            <div className={styles.otherLogin}>
              {isWecom ? (
                <WecomLoginBtn />
              ) : (
                <>
                  <div className={styles.otherDivider}>
                    <div className={styles.text}>{t(Strings.quick_login)}</div>
                  </div>
                  <OtherLogin afterLogin={afterLogin} />
                </>
              )}
            </div>
          </>
        )}
      {!isMobile && (
        <div className={styles.divider}>
          <div className={styles.text}>{modTitleText}</div>
        </div>
      )}

      {loginComponent}
      <div className={styles.buttonGroup}>
        {// Mobile or corporate WeChat exclusive domain name
          !responsive.large || !isWecom ? (
            <>
              <div>
                {toggleLoginModBtnVisible && (
                  <LinkButton
                    underline={false}
                    component='button'
                    id={AutoTestID.CHANGE_MODE_BTN}
                    className='toggleLoginModeBtn'
                    onClick={changeLoginMod}
                    style={{ paddingLeft: 0 }}
                  >
                    {changeModText}
                  </LinkButton>
                )}
              </div>
              {ACCOUNT_RESET_PASSWORD_VISIBLE && (
                <LinkButton underline={false} component='button' onClick={goResetPwd} style={{ paddingRight: 0 }}>
                  {t(Strings.retrieve_password)}
                </LinkButton>
              )}
            </>
          ) : (
            <div className={styles.buttonGroupDesktop}>
              {toggleLoginModBtnVisible && (
                <LinkButton
                  underline={false}
                  component='button'
                  id={AutoTestID.CHANGE_MODE_BTN}
                  className='toggleLoginModeBtn'
                  onClick={changeLoginMod}
                >
                  {changeModText}
                </LinkButton>
              )}
              <Trigger
                action={['click']}
                popup={loginOtherPopup}
                destroyPopupOnHide
                popupAlign={{ points: ['b', 'c'], offset: [0, -20], overflow: { adjustX: true, adjustY: true }}}
                popupStyle={{ width: 240 }}
                popupVisible={isPopupVisible}
                onPopupVisibleChange={() => popupVisibleToggle()}
                zIndex={10000}
              >
                <LinkButton underline={false} component='button'>
                  {t(Strings.other_login)}
                </LinkButton>
              </Trigger>
              {ACCOUNT_RESET_PASSWORD_VISIBLE && (
                <LinkButton underline={false} component='button' onClick={goResetPwd}>
                  {t(Strings.retrieve_password)}
                </LinkButton>
              )}
            </div>
          )}
      </div>
    </>
  );
};
