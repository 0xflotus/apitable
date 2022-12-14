package com.vikadata.api.auth.service;

import com.vikadata.api.auth.dto.UserLoginDTO;
import com.vikadata.api.auth.ro.LoginRo;

/**
 * Authorization related service interface
 */
public interface IAuthService {

    /**
     * Password login, only log in existing users, no need to automatically register an account
     * @param loginRo request parameters
     * @return user id
     */
    Long loginUsingPassword(LoginRo loginRo);

    /**
     * Login with mobile phone verification code, if it does not exist, the account will be registered automatically
     * @param loginRo request parameters
     * @return user id
     */
    UserLoginDTO loginUsingSmsCode(LoginRo loginRo);

    /**
     * Email login, only log in existing users, no need to automatically register an account
     * @param loginRo request parameters
     * @return user id
     */
    UserLoginDTO loginUsingEmailCode(LoginRo loginRo);
}
