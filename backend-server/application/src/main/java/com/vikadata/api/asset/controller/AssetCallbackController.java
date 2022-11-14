package com.vikadata.api.asset.controller;

import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.apitable.starter.oss.autoconfigure.OssProperties;
import com.apitable.starter.oss.autoconfigure.OssProperties.Callback;
import com.apitable.starter.oss.autoconfigure.OssProperties.Qiniu;
import com.apitable.starter.oss.core.qiniu.QiniuTemporaryClientTemplate;
import com.vikadata.api.asset.enums.AssetType;
import com.vikadata.api.asset.ro.AssetQiniuUploadCallbackBody;
import com.vikadata.api.asset.vo.AssetUploadResult;
import com.vikadata.api.base.model.AssetUploadNotifyRO;
import com.vikadata.api.base.model.WidgetUploadNotifyRO;
import com.vikadata.api.shared.component.scanner.annotation.ApiResource;
import com.vikadata.api.shared.component.scanner.annotation.PostResource;
import com.vikadata.api.asset.service.IAssetCallbackService;
import com.vikadata.core.support.ResponseData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static com.vikadata.core.constants.ResponseExceptionConstants.DEFAULT_ERROR_CODE;

/**
 * Attachment callback interface
 */
@RestController
@Api(tags = "Basic module - accessory callback interface")
@ApiResource(path = "/asset")
public class AssetCallbackController {

    @Resource
    private IAssetCallbackService iAssetCallbackService;

    @Autowired(required = false)
    private QiniuTemporaryClientTemplate qiniuTemporaryClientTemplate;

    @Autowired(required = false)
    private OssProperties ossProperties;

    @Deprecated
    @PostResource(name = "Qiniu cloud upload callback", path = "/qiniu/uploadCallback", requiredLogin = false, requiredPermission = false)
    @ApiOperation(value = "Qiniu cloud upload callback", hidden = true)
    public ResponseData<AssetUploadResult> qiniuCallback(@RequestHeader("Authorization") String authorization, @RequestBody String body) {
        Qiniu qiniu = ossProperties.getQiniu();
        if (null == ossProperties.getQiniu()) {
            return ResponseData.status(false, DEFAULT_ERROR_CODE, "not enabled qiniu callback").data(null);
        }
        Callback callback = Optional.ofNullable(qiniu.getCallback()).orElseGet(Callback::new);
        boolean validCallback = qiniuTemporaryClientTemplate.isValidCallback(authorization, callback.getUrl(), StrUtil.bytes(body), callback.getBodyType());
        if (!validCallback) {
            return ResponseData.status(false, DEFAULT_ERROR_CODE, "invalid content").data(null);
        }

        AssetQiniuUploadCallbackBody callbackBody = JSONUtil.toBean(body, AssetQiniuUploadCallbackBody.class);
        return ResponseData.success(iAssetCallbackService.qiniuCallback(callbackBody));
    }

    @PostResource(name = "Resource upload completion notification callback", path = "/upload/callback", requiredLogin = false)
    @ApiOperation(value = "Resource upload completion notification callback", notes = "After S3 completes the client upload, it actively reaches the notification server")
    public ResponseData<List<AssetUploadResult>> notifyCallback(@RequestBody AssetUploadNotifyRO body) {
        return ResponseData.success(iAssetCallbackService.loadAssetUploadResult(AssetType.of(body.getType()), body.getResourceKeys()));
    }

    @PostResource(name = "widget upload callback", path = "/widget/uploadCallback", requiredLogin = false)
    @ApiOperation(value = "widget upload callback")
    public ResponseData<Void> widgetCallback(@RequestBody WidgetUploadNotifyRO body) {
        iAssetCallbackService.widgetCallback(body.getResourceKeys());
        return ResponseData.success();
    }
}
