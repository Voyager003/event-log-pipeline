package com.eventlogpipeline.event;

import java.time.Instant;

public record GeneratedEvent(
        String eventId,
        EventType eventType,
        EventSource source,
        String schemaVersion,
        Instant occurredAt,
        String userId,
        String anonymousId,
        String sessionId,
        String deviceType,
        UserProperties userProperties,
        EventDetail detail
) {
}
