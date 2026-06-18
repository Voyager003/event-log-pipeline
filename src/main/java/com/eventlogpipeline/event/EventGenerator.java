package com.eventlogpipeline.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.springframework.stereotype.Component;

@Component
public class EventGenerator {

    private static final String[] DEVICE_TYPES = {"desktop", "mobile_web", "tablet"};
    private static final String[] ERROR_TYPES = {"video_streaming", "network", "api"};
    private static final String SCHEMA_VERSION = "1.0";
    private static final int LECTURE_LENGTH_SECONDS = 1_800;

    public List<GeneratedEvent> generate(int count) {
        return generate(count, new Random());
    }

    List<GeneratedEvent> generate(int count, Random random) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be greater than or equal to 0");
        }

        Instant baseTime = Instant.parse("2026-06-01T00:00:00Z");
        List<GeneratedEvent> events = new ArrayList<>(count);

        for (int eventNumber = 1; eventNumber <= count; eventNumber++) {
            events.add(createEvent(eventNumber, random, baseTime));
        }

        return events;
    }

    private GeneratedEvent createEvent(int eventNumber, Random random, Instant baseTime) {
        EventType eventType = pickEventType(random);
        String userId = formatId("user", random.nextInt(500) + 1);
        String sessionId = formatId("session", random.nextInt(700) + 1);
        String anonymousId = "anon-" + Math.abs(Objects.hash(userId, sessionId));
        String deviceType = pick(DEVICE_TYPES, random);
        String courseId = formatId("course", random.nextInt(40) + 1);
        String lectureId = formatId("lecture", random.nextInt(160) + 1);
        Instant occurredAt = randomOccurredAt(baseTime, random);
        EventDetail detail = createDetail(eventType, random, courseId, lectureId);

        return new GeneratedEvent(
                formatId("event", eventNumber),
                eventType,
                sourceFor(eventType),
                SCHEMA_VERSION,
                occurredAt,
                userId,
                anonymousId,
                sessionId,
                deviceType,
                detail
        );
    }

    private EventType pickEventType(Random random) {
        EventType[] eventTypes = EventType.values();
        return eventTypes[random.nextInt(eventTypes.length)];
    }

    private Instant randomOccurredAt(Instant baseTime, Random random) {
        int dayOffset = random.nextInt(14);
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        int second = random.nextInt(60);

        return baseTime
                .plus(dayOffset, ChronoUnit.DAYS)
                .plus(hour, ChronoUnit.HOURS)
                .plus(minute, ChronoUnit.MINUTES)
                .plus(second, ChronoUnit.SECONDS);
    }

    private EventDetail createDetail(EventType eventType, Random random, String courseId, String lectureId) {
        return switch (eventType) {
            case LECTURE_STARTED -> lectureDetail(random, courseId, lectureId, 0, 0);
            case LECTURE_PLAYED -> {
                int watchDuration = 30 + random.nextInt(271);
                int playbackPosition = random.nextInt(LECTURE_LENGTH_SECONDS - watchDuration);
                yield lectureDetail(random, courseId, lectureId, playbackPosition, watchDuration);
            }
            case LECTURE_COMPLETED -> lectureDetail(random, courseId, lectureId, LECTURE_LENGTH_SECONDS,
                    LECTURE_LENGTH_SECONDS);
            case VIDEO_ERROR_OCCURRED -> videoErrorDetail(random, courseId, lectureId);
        };
    }

    private EventDetail.Lecture lectureDetail(Random random, String courseId, String lectureId,
                                              int playbackPositionSeconds, int watchDurationSeconds) {
        BigDecimal completionRate = BigDecimal.valueOf(playbackPositionSeconds)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(LECTURE_LENGTH_SECONDS), 2, RoundingMode.HALF_UP);

        return new EventDetail.Lecture(
                courseId,
                lectureId,
                "lecture_" + (random.nextInt(20) + 1),
                playbackPositionSeconds,
                watchDurationSeconds,
                completionRate
        );
    }

    private EventDetail.VideoError videoErrorDetail(Random random, String courseId, String lectureId) {
        String errorType = pick(ERROR_TYPES, random);
        String errorCode = switch (errorType) {
            case "video_streaming" -> "VIDEO_BUFFER_TIMEOUT";
            case "network" -> "NETWORK_UNSTABLE";
            default -> "API_RESPONSE_TIMEOUT";
        };

        return new EventDetail.VideoError(
                courseId,
                lectureId,
                errorType,
                errorCode,
                "lecture playback failed"
        );
    }

    private EventSource sourceFor(EventType eventType) {
        return switch (eventType) {
            case VIDEO_ERROR_OCCURRED -> EventSource.SERVER;
            case LECTURE_STARTED, LECTURE_PLAYED, LECTURE_COMPLETED -> EventSource.WEB;
        };
    }

    private String pick(String[] values, Random random) {
        return values[random.nextInt(values.length)];
    }

    private String formatId(String prefix, int number) {
        return "%s-%04d".formatted(prefix, number);
    }
}
