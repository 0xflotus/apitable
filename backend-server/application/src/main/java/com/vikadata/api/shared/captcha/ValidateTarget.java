package com.vikadata.api.shared.captcha;

import cn.hutool.core.lang.Validator;
import lombok.Data;

import com.vikadata.api.shared.util.StringUtil;
import com.vikadata.core.util.ExceptionUtil;

import static com.vikadata.api.user.enums.UserException.MOBILE_ERROR_FORMAT;

/**
 * <p>
 * validate the target object
 * </p>
 *
 * @author Chambers
 */
@Data
public class ValidateTarget {

    private String target;

    private String areaCode;

    private static String mainlandAreaCode = "+86";

    private String lang;

    public ValidateTarget() {
    }

    public ValidateTarget(String target) {
        this.target = target;
    }

    public ValidateTarget(String target, String areaCode) {
        this.target = target;
        this.areaCode = areaCode;
    }

    public static ValidateTarget create(String target) {
        return new ValidateTarget(target);
    }

    public static ValidateTarget create(String target, String areaCode) {
        ExceptionUtil.isTrue(StringUtil.isPureNumber(target), MOBILE_ERROR_FORMAT);
        ExceptionUtil.isTrue(!mainlandAreaCode.equals(areaCode) ||
                (target.length() == 11 && Validator.isMobile(target)), MOBILE_ERROR_FORMAT);
        return new ValidateTarget(target, areaCode);
    }

    public String getRealTarget() {
        if (areaCode == null) {
            return target;
        }
        else {
            return areaCode + target;
        }
    }

    public String getIntactTarget() {
        if (Validator.isMobile(target)) {
            return mainlandAreaCode + target;
        }
        return target;
    }
}
