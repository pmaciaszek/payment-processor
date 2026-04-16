package com.zilch.interview.service.check;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zilch.interview.config.properties.ServicesProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
class GlobalRequestCounter {

    private final Cache<UUID, List<Long>> requests;
    private final long windowSeconds;

    GlobalRequestCounter(ServicesProperties servicesProperties) {
        this.windowSeconds = servicesProperties.velocityCheck().counterWindowSeconds();
        this.requests = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(windowSeconds))
                .build();
    }

    int increment(UUID userId) {
        long now = Instant.now().getEpochSecond();

        var timestamps = requests.get(userId, key -> new ArrayList<>());

        synchronized (timestamps) {
            timestamps.add(now);
            timestamps.removeIf(requestTimeStamp -> requestTimeStamp < now - windowSeconds);
            return timestamps.size();
        }
    }
}
