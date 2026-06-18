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
    private static final int MIN_WATCH_DURATION_SECONDS = 5 * 60;
    private static final int MAX_WATCH_DURATION_SECONDS = 90 * 60;
    private static final int ERROR_RATE_PERCENT = 8;

    public List<GeneratedEvent> generate(int sessionCount, int heartbeatIntervalMinutes) {
        return generate(sessionCount, heartbeatIntervalMinutes, new Random());
    }

    List<GeneratedEvent> generate(int sessionCount, int heartbeatIntervalMinutes, Random random) {
        if (sessionCount < 0) {
            throw new IllegalArgumentException("sessionCount must be greater than or equal to 0");
        }
        if (heartbeatIntervalMinutes <= 0) {
            throw new IllegalArgumentException("heartbeatIntervalMinutes must be greater than 0");
        }

        Instant baseTime = Instant.parse("2026-06-01T00:00:00Z");
        List<GeneratedEvent> events = new ArrayList<>(sessionCount * 8);
        int heartbeatIntervalSeconds = Math.toIntExact(ChronoUnit.MINUTES.getDuration().getSeconds()
                * heartbeatIntervalMinutes);

        for (int sessionNumber = 1; sessionNumber <= sessionCount; sessionNumber++) {
            addSessionEvents(events, sessionNumber, heartbeatIntervalSeconds, random, baseTime);
        }

        return events;
    }

    private void addSessionEvents(List<GeneratedEvent> events, int sessionNumber, int heartbeatIntervalSeconds,
                                  Random random, Instant baseTime) {
        String userId = formatId("user", random.nextInt(500) + 1);
        String sessionId = formatId("session", sessionNumber);
        String anonymousId = "anon-" + Math.abs(Objects.hash(userId, sessionId));
        String deviceType = pick(DEVICE_TYPES, random);
        String courseId = formatId("course", random.nextInt(40) + 1);
        String lectureId = formatId("lecture", random.nextInt(160) + 1);
        String lectureTitle = "lecture_" + (random.nextInt(20) + 1);
        Instant startedAt = randomOccurredAt(baseTime, random);

        addEvent(events, EventType.LECTURE_STARTED, startedAt, userId, anonymousId, sessionId, deviceType,
                lectureDetail(courseId, lectureId, lectureTitle, 0, 0));

        int watchDurationSeconds = randomWatchDurationSeconds(random);
        boolean failed = random.nextInt(100) < ERROR_RATE_PERCENT;
        int finalPlaybackPosition = failed
                ? Math.max(heartbeatIntervalSeconds, watchDurationSeconds - random.nextInt(heartbeatIntervalSeconds))
                : Math.min(watchDurationSeconds, LECTURE_LENGTH_SECONDS);

        Instant lastHeartbeatAt = startedAt;
        for (int playbackPosition = heartbeatIntervalSeconds; playbackPosition <= finalPlaybackPosition;
             playbackPosition += heartbeatIntervalSeconds) {
            lastHeartbeatAt = startedAt.plus(playbackPosition, ChronoUnit.SECONDS);

            addEvent(events, EventType.LECTURE_PLAYBACK_HEARTBEAT, lastHeartbeatAt, userId, anonymousId, sessionId,
                    deviceType, lectureDetail(courseId, lectureId, lectureTitle, playbackPosition,
                            heartbeatIntervalSeconds));
        }

        if (!failed) {
            addEvent(events, EventType.LECTURE_COMPLETED, startedAt.plus(finalPlaybackPosition, ChronoUnit.SECONDS),
                    userId, anonymousId, sessionId, deviceType,
                    lectureDetail(courseId, lectureId, lectureTitle, finalPlaybackPosition, finalPlaybackPosition));
            return;
        }

        addEvent(events, EventType.VIDEO_ERROR_OCCURRED,
                lastHeartbeatAt.plus(10 + random.nextInt(111), ChronoUnit.SECONDS), userId, anonymousId, sessionId,
                deviceType, videoErrorDetail(random, courseId, lectureId));
    }

    private void addEvent(List<GeneratedEvent> events, EventType eventType, Instant occurredAt, String userId,
                          String anonymousId, String sessionId, String deviceType, EventDetail detail) {
        events.add(new GeneratedEvent(
                formatId("event", events.size() + 1),
                eventType,
                sourceFor(eventType),
                SCHEMA_VERSION,
                occurredAt,
                userId,
                anonymousId,
                sessionId,
                deviceType,
                detail
        ));
    }

    private int randomWatchDurationSeconds(Random random) {
        return MIN_WATCH_DURATION_SECONDS
                + random.nextInt(MAX_WATCH_DURATION_SECONDS - MIN_WATCH_DURATION_SECONDS + 1);
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

    private EventDetail.Lecture lectureDetail(String courseId, String lectureId, String lectureTitle,
                                              int playbackPositionSeconds, int watchDurationSeconds) {
        BigDecimal completionRate = BigDecimal.valueOf(playbackPositionSeconds)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(LECTURE_LENGTH_SECONDS), 2, RoundingMode.HALF_UP);

        return new EventDetail.Lecture(
                courseId,
                lectureId,
                lectureTitle,
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
            case LECTURE_STARTED, LECTURE_PLAYBACK_HEARTBEAT, LECTURE_COMPLETED -> EventSource.WEB;
        };
    }

    private String pick(String[] values, Random random) {
        return values[random.nextInt(values.length)];
    }

    private String formatId(String prefix, int number) {
        return "%s-%04d".formatted(prefix, number);
    }
}
