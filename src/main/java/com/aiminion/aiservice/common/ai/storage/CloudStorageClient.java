package com.aiminion.aiservice.common.ai.storage;

import com.aiminion.aiservice.common.enums.MediaCategory;
import com.aiminion.aiservice.common.enums.StorageProvider;

public interface CloudStorageClient {
    StorageProvider getProvider();
    StoredMedia store(byte[] bytes, MediaCategory category, String fileName);
}