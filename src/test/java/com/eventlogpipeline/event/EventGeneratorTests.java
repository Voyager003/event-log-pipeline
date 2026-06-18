package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class EventGeneratorTests {

    private final EventGenerator eventGenerator = new EventGenerator();

    @Test
    void generatesRequestedNumberOfEvents() {
        var events = generateWithFixedRandom(1_000);

        assertThat(events).hasSize(1_000);
    }

    @Test
    void includesAllSupportedLiveklassEventTypes() {
        var eventTypes = generateWithFixedRandom(1_000).stream()
                .map(GeneratedEvent::eventType)
                .collect(Collectors.toSet());

        assertThat(eventTypes).isEqualTo(EnumSet.allOf(EventType.class));
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
        var firstRun = eventGenerator.generate(100);
        var secondRun = eventGenerator.generate(100);

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
    void exposesReadableSnakeCaseEventNamesForStorageAndAnalytics() {
        assertThat(EventType.LECTURE_STARTED.eventName()).isEqualTo("lecture_started");
        assertThat(EventType.LECTURE_PLAYED.eventName()).isEqualTo("lecture_played");
        assertThat(EventType.LECTURE_COMPLETED.eventName()).isEqualTo("lecture_completed");
        assertThat(EventType.VIDEO_ERROR_OCCURRED.eventName()).isEqualTo("video_error_occurred");
    }

    @Test
    void rejectsNegativeCount() {
        assertThatThrownBy(() -> eventGenerator.generate(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("count must be greater than or equal to 0");
    }

    private java.util.List<GeneratedEvent> generateWithFixedRandom(int count) {
        return eventGenerator.generate(count, new Random(20260615L));
    }

    private boolean isLectureEvent(EventType eventType) {
        return eventType == EventType.LECTURE_STARTED
                || eventType == EventType.LECTURE_PLAYED
                || eventType == EventType.LECTURE_COMPLETED;
    }
}
