package com.apitable.starter.oss.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public interface OssClientRequest {

    UrlFetchResponse uploadRemoteUrl(String bucketName, String remoteSrcUrl, String keyPath) throws IOException;

    void uploadStreamForObject(String bucketName, InputStream in, String keyPath) throws IOException;

    void uploadStreamForObject(String bucketName, InputStream in, String path, String mimeType, String digest) throws IOException;

    OssObject getObject(String bucketName, String path);

    OssStatObject getStatObject(String bucketName, String key);

    void executeStreamFunction(String bucketName, String key, Consumer<InputStream> function);

    boolean deleteObject(String bucketName, String key);

    void refreshCdn(String bucketName, String[] url);

    OssUploadAuth uploadToken(String bucket, String key, long expires, OssUploadPolicy uploadPolicy);

    boolean isValidCallback(String originAuthorization, String url, byte[] body, String contentType);

}
