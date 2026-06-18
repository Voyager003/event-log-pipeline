package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class EventGeneratorTests {

    private final EventGenerator eventGenerator = new EventGenerator();

    @Test
    void generatesRequestedNumberOfLectureSessions() {
        var events = generateWithFixedRandom(100);

        assertThat(events)
                .filteredOn(event -> event.eventType() == EventType.LECTURE_STARTED)
                .hasSize(100);
        assertThat(events).hasSizeGreaterThan(100);
    }

    @Test
    void includesAllSupportedLiveklassEventTypes() {
        var eventTypes = generateWithFixedRandom(1_000).stream()
                .map(GeneratedEvent::eventType)
                .collect(Collectors.toSet());

        assertThat(eventTypes).isEqualTo(EnumSet.allOf(EventType.class));
    }

    @Test
    void createsEnoughPlaybackHeartbeatsForTrafficAggregation() {
        var events = generateWithFixedRandom(1_000);

        long heartbeatCount = events.stream()
                .filter(event -> event.eventType() == EventType.LECTURE_PLAYBACK_HEARTBEAT)
                .count();

        assertThat(heartbeatCount).isGreaterThan(1_000);
    }

    @Test
    void fillsRequiredEnvelopeFieldsAndTypeSpecificDetail() {
        var events = generateWithFixedRandom(100);

        assertThat(events).allSatisfy(event -> {
            assertThat(event.eventId()).isNotBlank();
            assertThat(event.eventType()).isNotNull();
            assertThat(event.source()).isNotNull();
            assertThat(event.schemaVersion()).isEqualTo("1.0");
            assertThat(event.occurredAt()).isNotNull();
            assertThat(event.userId()).isNotBlank();
            assertThat(event.anonymousId()).isNotBlank();
            assertThat(event.sessionId()).isNotBlank();
            assertThat(event.deviceType()).isNotBlank();
            assertThat(event.detail()).isNotNull();
        });

        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.Lecture.class));
        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.VideoError.class));
    }

    @Test
    void generatesDifferentEventsAcrossDefaultRuns() {
        var firstRun = eventGenerator.generate(100, 5);
        var secondRun = eventGenerator.generate(100, 5);

        assertThat(secondRun).isNotEqualTo(firstRun);
    }

    @Test
    void mapsLectureEventsToWebAndVideoErrorsToServer() {
        var events = generateWithFixedRandom(1_000);

        assertThat(events)
                .filteredOn(event -> event.eventType() == EventType.VIDEO_ERROR_OCCURRED)
                .allSatisfy(event -> assertThat(event.source()).isEqualTo(EventSource.SERVER));
        assertThat(events)
                .filteredOn(event -> isLectureEvent(event.eventType()))
                .allSatisfy(event -> assertThat(event.source()).isEqualTo(EventSource.WEB));
    }

    @Test
    void preservesEventOrderWithinSameSession() {
        var eventsBySession = generateWithFixedRandom(1_000).stream()
                .collect(Collectors.groupingBy(GeneratedEvent::sessionId));

        assertThat(eventsBySession.values()).allSatisfy(sessionEvents -> {
            assertThat(sessionEvents.getFirst().eventType()).isEqualTo(EventType.LECTURE_STARTED);
            assertThat(isChronological(sessionEvents)).isTrue();
            assertThat(hasNoEventsAfterTerminalEvent(sessionEvents)).isTrue();
        });
    }

    @Test
    void exposesReadableSnakeCaseEventNamesForStorageAndAnalytics() {
        assertThat(EventType.LECTURE_STARTED.eventName()).isEqualTo("lecture_started");
        assertThat(EventType.LECTURE_PLAYBACK_HEARTBEAT.eventName()).isEqualTo("lecture_playback_heartbeat");
        assertThat(EventType.LECTURE_COMPLETED.eventName()).isEqualTo("lecture_completed");
        assertThat(EventType.VIDEO_ERROR_OCCURRED.eventName()).isEqualTo("video_error_occurred");
    }

    @Test
    void rejectsNegativeCount() {
        assertThatThrownBy(() -> eventGenerator.generate(-1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sessionCount must be greater than or equal to 0");
    }

    @Test
    void rejectsInvalidHeartbeatInterval() {
        assertThatThrownBy(() -> eventGenerator.generate(100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("heartbeatIntervalMinutes must be greater than 0");
    }

    private java.util.List<GeneratedEvent> generateWithFixedRandom(int sessionCount) {
        return eventGenerator.generate(sessionCount, 5, new Random(20260615L));
    }

    private boolean isChronological(List<GeneratedEvent> events) {
        for (int index = 1; index < events.size(); index++) {
            if (events.get(index).occurredAt().isBefore(events.get(index - 1).occurredAt())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNoEventsAfterTerminalEvent(List<GeneratedEvent> events) {
        for (int index = 0; index < events.size() - 1; index++) {
            EventType eventType = events.get(index).eventType();
            if (eventType == EventType.LECTURE_COMPLETED || eventType == EventType.VIDEO_ERROR_OCCURRED) {
                return false;
            }
        }
        return true;
    }

    private boolean isLectureEvent(EventType eventType) {
        return eventType == EventType.LECTURE_STARTED
                || eventType == EventType.LECTURE_PLAYBACK_HEARTBEAT
                || eventType == EventType.LECTURE_COMPLETED;
    }
}
