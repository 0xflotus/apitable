package com.vikadata.api.user.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import com.vikadata.api.base.enums.ParameterException;
import com.vikadata.api.base.enums.TrackEventType;
import com.vikadata.api.base.enums.ValidateType;
import com.vikadata.api.base.service.ParamVerificationService;
import com.vikadata.api.base.service.SensorsService;
import com.vikadata.api.interfaces.social.facade.SocialServiceFacade;
import com.vikadata.api.organization.ro.CheckUserEmailRo;
import com.vikadata.api.organization.ro.UserLinkEmailRo;
import com.vikadata.api.organization.service.IMemberService;
import com.vikadata.api.shared.cache.bean.LoginUserDto;
import com.vikadata.api.shared.cache.bean.UserSpaceDto;
import com.vikadata.api.shared.cache.service.UserActiveSpaceCacheService;
import com.vikadata.api.shared.cache.service.UserSpaceCacheService;
import com.vikadata.api.shared.component.TaskManager;
import com.vikadata.api.shared.component.scanner.annotation.ApiResource;
import com.vikadata.api.shared.component.scanner.annotation.GetResource;
import com.vikadata.api.shared.component.scanner.annotation.PostResource;
import com.vikadata.api.shared.config.properties.ConstProperties;
import com.vikadata.api.shared.context.LoginContext;
import com.vikadata.api.shared.context.SessionContext;
import com.vikadata.api.shared.captcha.CodeValidateScope;
import com.vikadata.api.shared.captcha.ValidateCodeProcessorManage;
import com.vikadata.api.shared.captcha.ValidateCodeType;
import com.vikadata.api.shared.captcha.ValidateTarget;
import com.vikadata.api.shared.captcha.sms.ISmsService;
import com.vikadata.api.shared.captcha.sms.TencentConstants;
import com.vikadata.api.shared.util.StringUtil;
import com.vikadata.api.shared.util.information.ClientOriginInfo;
import com.vikadata.api.shared.util.information.InformationUtil;
import com.vikadata.api.space.enums.LabsApplicantTypeEnum;
import com.vikadata.api.space.service.ILabsApplicantService;
import com.vikadata.api.space.vo.LabsFeatureVo;
import com.vikadata.api.user.entity.UserEntity;
import com.vikadata.api.user.ro.CodeValidateRo;
import com.vikadata.api.user.ro.EmailCodeValidateRo;
import com.vikadata.api.user.ro.RetrievePwdOpRo;
import com.vikadata.api.user.ro.SmsCodeValidateRo;
import com.vikadata.api.user.ro.UpdatePwdOpRo;
import com.vikadata.api.user.ro.UserLabsFeatureRo;
import com.vikadata.api.user.ro.UserOpRo;
import com.vikadata.api.user.service.IUserHistoryService;
import com.vikadata.api.user.service.IUserService;
import com.vikadata.api.user.vo.UserInfoVo;
import com.vikadata.api.workspace.service.INodeService;
import com.vikadata.core.support.ResponseData;
import com.vikadata.core.util.ExceptionUtil;
import com.vikadata.core.util.HttpContextUtil;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.vikadata.api.space.enums.LabsApplicantTypeEnum.SPACE_LEVEL_FEATURE;
import static com.vikadata.api.space.enums.LabsFeatureEnum.ofLabsFeature;
import static com.vikadata.api.space.enums.LabsFeatureException.SPACE_ID_NOT_EMPTY;
import static com.vikadata.api.space.enums.SpaceException.NOT_IN_SPACE;
import static com.vikadata.api.user.enums.UserClosingException.USER_APPLIED_FOR_CLOSING;
import static com.vikadata.api.user.enums.UserClosingException.USER_CANCELED_CLOSING;
import static com.vikadata.api.user.enums.UserClosingException.USER_NOT_ALLOWED_CANCEL_CLOSING;
import static com.vikadata.api.user.enums.UserClosingException.USER_NOT_ALLOWED_TO_CLOSE;
import static com.vikadata.api.user.enums.UserException.EMAIL_HAS_BIND;
import static com.vikadata.api.user.enums.UserException.EMAIL_NO_EXIST;
import static com.vikadata.api.user.enums.UserException.MOBILE_HAS_REGISTER;
import static com.vikadata.api.user.enums.UserException.MOBILE_NO_EXIST;
import static com.vikadata.api.user.enums.UserException.MODIFY_PASSWORD_ERROR;
import static com.vikadata.api.user.enums.UserException.PASSWORD_HAS_SETTING;
import static com.vikadata.api.user.enums.UserException.USER_NOT_BIND_EMAIL;
import static com.vikadata.api.user.enums.UserException.USER_NOT_BIND_PHONE;
import static com.vikadata.api.user.enums.UserException.USER_NOT_EXIST;
import static com.vikadata.core.constants.RedisConstants.ERROR_PWD_NUM_DIR;

/**
 * <p>
 * user interface
 * </p>
 *
 * @author Benson Cheung
 */
@Slf4j
@RestController
@Api(tags = "Account Center Module_User Management Interface")
@ApiResource(path = "/user")
public class UserController {

    @Resource
    private ConstProperties constProperties;

    @Resource
    private IUserService iUserService;

    @Resource
    private ISmsService iSmsService;

    @Resource
    private RedisTemplate<String, Integer> redisTemplate;

    @Resource
    private ParamVerificationService verificationService;

    @Resource
    private SensorsService sensorsService;

    @Resource
    private ILabsApplicantService iLabsApplicantService;

    @Resource
    private UserSpaceCacheService userSpaceCacheService;

    @Resource
    private UserActiveSpaceCacheService userActiveSpaceCacheService;

    @Resource
    private IUserHistoryService userHistoryService;

    @Resource
    private INodeService iNodeService;

    @Resource
    private SocialServiceFacade socialServiceFacade;

    @Resource
    private IMemberService iMemberService;

    @GetResource(name = "get personal information", path = "/me", requiredPermission = false)
    @ApiOperation(value = "get personal information", notes = "get personal information", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "spaceId", value = "space id", dataTypeClass = String.class, paramType = "query", example = "spc8mXUeiXyVo"),
            @ApiImplicitParam(name = "nodeId", value = "node id", dataTypeClass = String.class, paramType = "query", example = "dstS94qPZFXjC1LKns"),
            @ApiImplicitParam(name = "filter", value = "whether to filter space related information", defaultValue = "false", dataTypeClass = Boolean.class, paramType = "query", example = "true")
    })
    public ResponseData<UserInfoVo> userInfo(@RequestParam(name = "spaceId", required = false) String spaceId,
            @RequestParam(name = "nodeId", required = false) String nodeId,
            @RequestParam(name = "filter", required = false, defaultValue = "false") Boolean filter,
            HttpServletRequest request) {
        Long userId = SessionContext.getUserId();

        // try to return SpaceId
        spaceId = tryReturnSpaceId(nodeId, spaceId, userId, request);

        // Get user information
        UserInfoVo userInfo = iUserService.getCurrentUserInfo(userId, spaceId, filter);

        // Returns the domain name bound to the space station
        String spaceDomain = returnSpaceDomain(spaceId, userInfo.getSpaceId());
        userInfo.setSpaceDomain(spaceDomain);
        return ResponseData.success(userInfo);
    }

    // Before getting the user information, try to return the Space id first
    private String tryReturnSpaceId(String nodeId, String spaceId, Long userId, HttpServletRequest request) {
        if (StrUtil.isNotBlank(nodeId)) {
            // 1.Use url - NodeId to locate the space and return the bound domain name
            return iNodeService.getSpaceIdByNodeIdIncludeDeleted(nodeId);
        }
        if (StrUtil.isBlank(spaceId)) {
            // 2.The user does not actively locate the space station behavior - use the current access domain name to locate the space station
            String remoteHost = HttpContextUtil.getRemoteHost(request);
            String domainBindSpaceId = socialServiceFacade.getSpaceIdByDomainName(remoteHost);
            if (StrUtil.isNotBlank(domainBindSpaceId)) {
                // Exception: The registrant uses an exclusive domain name, and then logs in with an account and password;
                // Return to the exclusive domain name space station The current registrant does not have permission to operate the space;
                // At that time, return the last active space ID of the user
                Long memberId = iMemberService.getMemberIdByUserIdAndSpaceId(userId, domainBindSpaceId);
                if (ObjectUtil.isNull(memberId)) {
                    // No operation permission, get the last active node of active users
                    return userActiveSpaceCacheService.getLastActiveSpace(userId);
                }
                else {
                    return domainBindSpaceId;
                }
            }
        }
        return spaceId;
    }

    // Return the space station domain name
    private String returnSpaceDomain(String spaceId, String userSpaceId) {
        // Returns the domain name information, and returns the public domain name if there is no credential acquisition or search
        if (StrUtil.isNotBlank(spaceId)) {
            // 3.If the precondition space id is not empty, return the bound domain name directly
            return socialServiceFacade.getDomainNameBySpaceId(spaceId, false);
        }
        else {
            // 4.If there is none, operate according to the last active space
            if (StrUtil.isBlank(userSpaceId)) {
                return constProperties.defaultServerDomain();
            }
            else {
                return socialServiceFacade.getDomainNameBySpaceId(userSpaceId, false);
            }
        }
    }

    @GetResource(name = "Query whether users bind mail", path = "/email/bind", requiredPermission = false)
    @ApiOperation(value = "Query whether users bind mail", notes = "Query whether users bind mail")
    public ResponseData<Boolean> validBindEmail() {
        Long userId = SessionContext.getUserId();
        Boolean exist = iUserService.checkUserHasBindEmail(userId);
        return ResponseData.success(exist);
    }

    @PostResource(name = "Query whether the user is consistent with the specified mail", path = "/validate/email", requiredPermission = false)
    @ApiOperation(value = "Query whether the user is consistent with the specified mail", notes = "Query whether the user is consistent with the specified mail. It can only be determined if the user has bound the mail")
    public ResponseData<Boolean> validSameEmail(@RequestBody @Valid CheckUserEmailRo data) {
        Long userId = SessionContext.getUserId();
        UserEntity user = iUserService.getById(userId);
        ExceptionUtil.isNotNull(user, USER_NOT_EXIST);
        ExceptionUtil.isNotNull(user.getEmail(), USER_NOT_BIND_EMAIL);
        return ResponseData.success(user.getEmail().equals(data.getEmail()));
    }

    @PostResource(name = "Associate the invited mail", path = "/link/inviteEmail", requiredPermission = false)
    @ApiOperation(value = "Associate the invited mail", notes = "Users can only associate with invited mail when they have no other mail", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> bindEmail(@RequestBody @Valid UserLinkEmailRo data) {
        String email = data.getEmail();
        String spaceId = data.getSpaceId();
        Long userId = SessionContext.getUserId();
        iUserService.bindMemberByEmail(userId, spaceId, email);
        return ResponseData.success();
    }

    @PostResource(name = "Bind mail", path = "/bindEmail", requiredPermission = false)
    @ApiOperation(value = "Bind mail", notes = "Bind mail and modify mail", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> verifyEmail(@RequestBody @Valid EmailCodeValidateRo param) {
        ValidateTarget target = ValidateTarget.create(param.getEmail());
        ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.EMAIL).validate(target, param.getCode(), true, CodeValidateScope.BOUND_EMAIL);
        // Judge whether it exists
        boolean exist = iUserService.checkByEmail(param.getEmail());
        ExceptionUtil.isFalse(exist, EMAIL_HAS_BIND);
        Long userId = SessionContext.getUserId();
        iUserService.updateEmailByUserId(userId, param.getEmail());
        return ResponseData.success();
    }

    @PostResource(name = "Unbind mail", path = "/unbindEmail", requiredPermission = false)
    @ApiOperation(value = "Unbind mail", notes = "Bind mail and modify mail", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> unbindEmail(@RequestBody @Valid CodeValidateRo param) {
        LoginUserDto loginUser = LoginContext.me().getLoginUser();
        // Judge whether users bind mail
        ExceptionUtil.isNotBlank(loginUser.getEmail(), USER_NOT_BIND_EMAIL);
        ValidateTarget target = ValidateTarget.create(loginUser.getEmail());
        ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.EMAIL).validate(target, param.getCode(), true, CodeValidateScope.COMMON_VERIFICATION);
        iUserService.unbindEmailByUserId(loginUser.getUserId());
        return ResponseData.success();
    }

    @PostResource(name = "Bind a new phone", path = "/bindPhone", requiredPermission = false)
    @ApiOperation(value = "Bind a new phone", notes = "Bind a new phone", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> verifyPhone(@RequestBody @Valid SmsCodeValidateRo param) {
        ValidateTarget target = ValidateTarget.create(param.getPhone(), param.getAreaCode());
        ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.SMS).validate(target, param.getCode(), true, CodeValidateScope.BOUND_MOBILE);
        // Judge whether it exists
        boolean exist = iUserService.checkByCodeAndMobile(param.getAreaCode(), param.getPhone());
        ExceptionUtil.isFalse(exist, MOBILE_HAS_REGISTER);
        Long userId = SessionContext.getUserId();
        iUserService.updateMobileByUserId(userId, param.getAreaCode(), param.getPhone());
        return ResponseData.success();
    }

    @PostResource(name = "Unbind mobile phone", path = "/unbindPhone", requiredPermission = false)
    @ApiOperation(value = "Unbind mobile phone", notes = "Unbind mobile phone", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> unbindPhone(@RequestBody @Valid CodeValidateRo param) {
        LoginUserDto loginUser = LoginContext.me().getLoginUser();
        // Judge whether the user binds the mobile phone number
        ExceptionUtil.isNotBlank(loginUser.getMobile(), USER_NOT_BIND_PHONE);
        ValidateTarget target = ValidateTarget.create(loginUser.getMobile(), loginUser.getAreaCode());
        ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.SMS).validate(target, param.getCode(), true, CodeValidateScope.UN_BOUND_MOBILE);
        iUserService.unbindMobileByUserId(loginUser.getUserId());
        return ResponseData.success();
    }

    @PostResource(name = "Edit user information", path = "/update", requiredPermission = false)
    @ApiOperation(value = "Edit user information", notes = "Request parameters cannot be all empty", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<String> update(@RequestBody @Valid UserOpRo param) {
        ExceptionUtil.isTrue(StrUtil.isNotBlank(param.getAvatar()) || StrUtil.isNotBlank(param.getNickName())
                || StrUtil.isNotBlank(param.getLocale()), ParameterException.NO_ARG);
        Long userId = SessionContext.getUserId();
        iUserService.edit(userId, param);
        if (StrUtil.isNotBlank(param.getAvatar())) {
            return ResponseData.success(StringUtil.trimSlash(constProperties.getOssBucketByAsset().getResourceUrl()) + param.getAvatar());
        }
        if (BooleanUtil.isTrue(param.getInit()) && StrUtil.isNotBlank(param.getNickName())) {
            // Shence buried point - initialize nickname
            ClientOriginInfo origin = InformationUtil.getClientOriginInfo(false, true);
            TaskManager.me().execute(() -> sensorsService.track(userId, TrackEventType.SET_NICKNAME, null, origin));
        }
        return ResponseData.success(null);
    }

    @PostResource(name = "Change Password", path = "/updatePwd", requiredPermission = false)
    @ApiOperation(value = "Change Password", notes = "Scene: 1. Personal setting and password modification; 2. Initialize after login for accounts without password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> updatePwd(@RequestBody UpdatePwdOpRo param) {
        verificationService.verifyPassword(param.getPassword());
        Long userId = SessionContext.getUserId();
        if (StrUtil.isNotBlank(param.getCode())) {
            // Scene 1: Verification of verification code
            LoginUserDto loginUserDto = LoginContext.me().getLoginUser();
            if (param.getType() == ValidateType.EMAIL_CODE) {
                ValidateTarget target = ValidateTarget.create(loginUserDto.getEmail());
                ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.EMAIL)
                        .validate(target, param.getCode(), true, CodeValidateScope.COMMON_VERIFICATION);
            }
            else if (param.getType() == ValidateType.SMS_CODE) {
                ValidateTarget target = ValidateTarget.create(loginUserDto.getMobile(), loginUserDto.getAreaCode());
                ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.SMS)
                        .validate(target, param.getCode(), true, CodeValidateScope.UPDATE_PWD);
            }
            else {
                // The account is not bound to the mobile phone and email at the same time, so the verification code verification can be skipped
                ExceptionUtil.isTrue(StrUtil.isBlank(loginUserDto.getEmail())
                        && StrUtil.isBlank(loginUserDto.getMobile()), MODIFY_PASSWORD_ERROR);
            }
            // Change Password
            iUserService.updatePwd(userId, param.getPassword());
            // Send SMS notification asynchronously
            if (StrUtil.isNotBlank(loginUserDto.getMobile())) {
                ValidateTarget target = ValidateTarget.create(loginUserDto.getMobile(), loginUserDto.getAreaCode());
                TaskManager.me().execute(() -> iSmsService.sendMessage(target, TencentConstants.SmsTemplate.UPDATE_PASSWORD_SUCCESS_NOTICE));
            }
            // Close the login session of other end of the account
            iUserService.closeMultiSession(userId, true);
        }
        else {
            // Judge whether the user account is set with a password. The account with a password cannot be initialized
            boolean needPwd = LoginContext.me().getLoginUser().getNeedPwd();
            ExceptionUtil.isTrue(needPwd, PASSWORD_HAS_SETTING);
            iUserService.updatePwd(userId, param.getPassword());
        }
        return ResponseData.success();
    }

    @PostResource(name = "Retrieve password", path = "/retrievePwd", requiredLogin = false)
    @ApiOperation(value = "Retrieve password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> retrievePwd(@RequestBody @Valid RetrievePwdOpRo param) {
        // Verify password format
        verificationService.verifyPassword(param.getPassword());
        UserEntity user;
        if (param.getType() == ValidateType.EMAIL_CODE) {
            // Check the email verification code
            ValidateTarget target = ValidateTarget.create(param.getUsername());
            ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.EMAIL)
                    .validate(target, param.getCode(), true, CodeValidateScope.COMMON_VERIFICATION);
            // Determine whether the account exists
            user = iUserService.getByEmail(param.getUsername());
            ExceptionUtil.isNotNull(user, EMAIL_NO_EXIST);
        }
        else {
            // Check SMS verification code
            ValidateTarget target = ValidateTarget.create(param.getUsername(), param.getAreaCode());
            ValidateCodeProcessorManage.me().findValidateCodeProcessor(ValidateCodeType.SMS)
                    .validate(target, param.getCode(), true, CodeValidateScope.UPDATE_PWD);
            // Determine whether the account exists
            user = iUserService.getByCodeAndMobilePhone(param.getAreaCode(), param.getUsername());
            ExceptionUtil.isNotNull(user, MOBILE_NO_EXIST);
        }
        Long id = user.getId();
        // Change Password
        iUserService.updatePwd(id, param.getPassword());
        // Send SMS notification asynchronously
        if (user.getMobilePhone() != null) {
            TaskManager.me().execute(() -> iSmsService.sendMessage(ValidateTarget.create(user.getMobilePhone(), user.getCode()),
                    TencentConstants.SmsTemplate.UPDATE_PASSWORD_SUCCESS_NOTICE));
        }
        // Close the login session of other end of the account
        iUserService.closeMultiSession(id, false);
        // Unlock the account caused by frequent password errors
        redisTemplate.delete(ERROR_PWD_NUM_DIR + id);
        return ResponseData.success();
    }

    @PostResource(name = "Apply for cancellation of user account", path = "/applyForClosing", requiredPermission = false)
    @ApiOperation(value = "Apply for cancellation of user account", notes = "Registered login user applies for account cancellation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> applyForClosing() {
        // Get the current login user
        Long userId = SessionContext.getUserId();
        UserEntity user = iUserService.getById(userId);
        // Judge whether the user has applied for account cancellation
        ExceptionUtil.isFalse(user.getIsPaused(), USER_APPLIED_FOR_CLOSING);
        // Judge whether the current user meets the logout conditions
        boolean allowedToBeClosed = userHistoryService.checkAccountAllowedToBeClosed(userId);
        ExceptionUtil.isTrue(allowedToBeClosed, USER_NOT_ALLOWED_TO_CLOSE);
        // Cancel the account and enter the calm period
        iUserService.applyForClosingAccount(user);
        // Destroy user cookies and maintain sessions
        iUserService.closeMultiSession(userId, true);
        return ResponseData.success();
    }

    @GetResource(name = "Verify whether the account can be cancelled", path = "/checkForClosing", requiredPermission = false)
    @ApiOperation(value = "Verify whether the account can be cancelled", notes = "Unregistered users verify whether the account meets the cancellation conditions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> checkForClosing() {
        // Get the current login user
        Long userId = SessionContext.getUserId();
        // Judge whether the current user meets the account cancellation conditions
        boolean isAllowedToBeClosed = userHistoryService.checkAccountAllowedToBeClosed(userId);
        ExceptionUtil.isTrue(isAllowedToBeClosed, USER_NOT_ALLOWED_TO_CLOSE);
        return ResponseData.success();
    }

    @PostResource(name = "Apply for account restoration", path = "/cancelClosing", requiredPermission = false)
    @ApiOperation(value = "Apply for account restoration", notes = "User recovery account has been applied for cancellation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> cancelClosing() {
        // Get the current login user
        Long userId = SessionContext.getUserId();
        UserEntity user = iUserService.getById(userId);
        // The account cancellation application has not been submitted and cannot be withdrawn
        ExceptionUtil.isTrue(user.getIsPaused(), USER_NOT_ALLOWED_CANCEL_CLOSING);
        // The account has passed the calm period and cannot be recovered
        ExceptionUtil.isFalse(user.getIsDeleted(), USER_CANCELED_CLOSING);
        // Cancel account cancellation
        iUserService.cancelClosingAccount(user);
        return ResponseData.success();
    }

    @GetResource(name = "Get the enabled experimental functions", path = "/labs/features", requiredPermission = false)
    @ApiOperation(value = "Get the enabled experimental functions", notes = "Get the enabled experimental functions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<LabsFeatureVo> getEnabledLabFeatures(@RequestParam String spaceId) {
        Long userId = SessionContext.getUserId();
        List<String> applicants = new ArrayList<>();
        if (StrUtil.isNotBlank(spaceId)) {
            applicants.add(spaceId);
        }
        applicants.add(Long.toString(userId));
        return ResponseData.success(iLabsApplicantService.getUserCurrentFeatureApplicants(applicants));
    }

    @PostResource(path = "/labs/features", requiredPermission = false)
    @ApiOperation(value = "Update the usage status of laboratory functions", notes = "Update the usage status of laboratory functions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseData<Void> updateLabsFeatureStatus(@RequestBody @Valid UserLabsFeatureRo userLabsFeatureRo) {
        // Get the user ID of the current user
        Long userId = SessionContext.getUserId();
        // Get the space ID of the space station level function to be operated
        String spaceId = userLabsFeatureRo.getSpaceId();
        // Get the feature key of the function to be operated
        String featureKey = userLabsFeatureRo.getKey();
        // The space level function must be operated with the space ID, otherwise it is not allowed to operate
        LabsApplicantTypeEnum applicantType = ofLabsFeature(featureKey).getApplicantType();
        ExceptionUtil.isFalse(SPACE_LEVEL_FEATURE.equals(applicantType) && StrUtil.isBlank(spaceId), SPACE_ID_NOT_EMPTY);
        // When the operating user does not belong to the space, operation is not allowed
        UserSpaceDto userSpace = userSpaceCacheService.getUserSpace(userId, spaceId);
        ExceptionUtil.isNotNull(userSpace, NOT_IN_SPACE);
        String applicant = StrUtil.isNotBlank(spaceId) ? spaceId : Long.toString(userId);
        if (userLabsFeatureRo.getIsEnabled()) {
            // Enable experimental features
            iLabsApplicantService.enableLabsFeature(applicant, applicantType, featureKey, userId);
        }
        else {
            // Disable experimental functions
            iLabsApplicantService.disableLabsFeature(applicant, featureKey);
        }
        return ResponseData.success();
    }

    @PostResource(path = "/delActiveSpaceCache", method = { RequestMethod.GET }, requiredPermission = false)
    @ApiOperation(value = "Delete Active Space Cache")
    public ResponseData<Void> delActiveSpaceCache() {
        // Fill in the invitation code and reward integral
        Long userId = SessionContext.getUserId();
        userActiveSpaceCacheService.delete(userId);
        return ResponseData.success();
    }
}
