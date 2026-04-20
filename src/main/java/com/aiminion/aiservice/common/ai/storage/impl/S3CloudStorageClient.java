package com.aiminion.aiservice.common.ai.storage.impl;

import com.aiminion.aiservice.common.ai.storage.CloudStorageClient;
import com.aiminion.aiservice.common.ai.storage.StoredMedia;
import com.aiminion.aiservice.common.enums.MediaCategory;
import com.aiminion.aiservice.common.enums.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "S3")
public class S3CloudStorageClient implements CloudStorageClient {

    @Override
    public StorageProvider getProvider() {
        return StorageProvider.S3;
    }

    @Override
    public StoredMedia store(byte[] bytes, MediaCategory category, String fileName) {
        throw new UnsupportedOperationException("S3 storage not yet implemented.");
    }
}