package com.aiminion.aiservice.common.ai.router;
import com.aiminion.aiservice.common.ai.storage.CloudStorageClient;
import com.aiminion.aiservice.common.ai.storage.StoredMedia;
import com.aiminion.aiservice.common.enums.MediaCategory;
import com.aiminion.aiservice.common.enums.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CloudStorageRouter {

    private final Map<StorageProvider, CloudStorageClient> clients;

    @Value("${storage.provider:GCP}")
    private StorageProvider defaultProvider;

    public CloudStorageRouter(List<CloudStorageClient> clients) {
        this.clients = clients.stream()
                .collect(Collectors.toMap(CloudStorageClient::getProvider, Function.identity()));
        log.info("[CloudStorageRouter] Registered storage providers: {}", this.clients.keySet());
    }

    public StoredMedia store(byte[] bytes, MediaCategory category, String fileName) {
        return store(bytes, category, fileName, null);
    }

    public StoredMedia store(byte[] bytes, MediaCategory category, String fileName, StorageProvider provider) {
        StorageProvider resolved = provider != null ? provider : defaultProvider;
        CloudStorageClient client = clients.get(resolved);

        if (client == null) {
            throw new IllegalArgumentException("No storage client registered for provider: " + resolved);
        }

        log.info("[CloudStorageRouter] Storing {} via {} fileName={}", category, resolved, fileName);
        return client.store(bytes, category, fileName);
    }
}