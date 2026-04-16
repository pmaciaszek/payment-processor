package com.zilch.interview.service.check;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GlobalRequestCounter {

    private static final Map<UUID, List<Long>> REQUESTS = new ConcurrentHashMap<>();

    static int increment(UUID userId) {
        long now = Instant.now().getEpochSecond();

        REQUESTS.computeIfAbsent(userId, key -> new ArrayList<>());
        List<Long> timestamps = REQUESTS.get(userId);

        synchronized (timestamps) {
            timestamps.add(now);

            timestamps.removeIf(requestTimeStamp -> requestTimeStamp < now - 10);

            return timestamps.size();
        }
    }

    static void reset() {
        REQUESTS.clear();
    }
}
