package com.apitable.starter.idaas.core.api;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.apitable.starter.idaas.core.IdaasApiException;
import com.apitable.starter.idaas.core.IdaasConfig;
import com.apitable.starter.idaas.core.IdaasTemplate;
import com.apitable.starter.idaas.core.model.UsersRequest;
import com.apitable.starter.idaas.core.model.UsersResponse;
import com.apitable.starter.idaas.core.support.ServiceAccount;

/**
 * <p>
 * IDaaS user API test
 * </p>
 *
 */
class UserApiTest {

    private static final String TEST_TENANT_NAME = "test-20220606";

    private static UserApi userApi;

    private static ServiceAccount tenantServiceAccount;

    @BeforeAll
    static void init() {
        IdaasConfig idaasConfig = new IdaasConfig();
        idaasConfig.setSystemHost("https://demo-admin.cig.tencentcs.com");
        idaasConfig.setContactHost("https://{tenantName}-admin.cig.tencentcs.com");
        IdaasTemplate idaasTemplate = new IdaasTemplate(idaasConfig);
        userApi = idaasTemplate.getUserApi();
        InputStream inputStream = FileHelper.getInputStreamFromResource("tenant_service_account.json");
        String jsonString = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        tenantServiceAccount = JSONUtil.toBean(jsonString, ServiceAccount.class);
    }

    @Test
    void usersTest() throws IdaasApiException {
        UsersRequest usersRequest = new UsersRequest();
        usersRequest.setStatus("ACTIVE");
        usersRequest.setEndTime(Instant.now().toEpochMilli());
        usersRequest.setPageIndex(0);
        usersRequest.setPageSize(500);
        usersRequest.setOrderBy(Collections.singletonList("_createdOn"));
        UsersResponse usersResponse = userApi.users(usersRequest, tenantServiceAccount, TEST_TENANT_NAME);
        System.out.println(JSONUtil.toJsonStr(usersResponse));

        Assertions.assertNotNull(usersResponse);
    }

}
