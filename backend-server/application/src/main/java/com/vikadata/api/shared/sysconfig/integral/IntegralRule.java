package com.vikadata.api.shared.sysconfig.integral;

import lombok.Data;

/**
 * <p>
 * Integral Rule
 * </p>
 */
@Data
public class IntegralRule {

    private String id;

    private String actionCode;

    private String actionName;

    private int integralValue;

    private int dayMaxIntegralValue;

    private boolean online;

    private boolean notify;
}
