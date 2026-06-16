package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class EventGeneratorTests {

    private final EventGenerator eventGenerator = new EventGenerator();

    @Test
    void generatesRequestedNumberOfEvents() {
        var events = eventGenerator.generate(1_000, 20260615L);

        assertThat(events).hasSize(1_000);
    }

    @Test
    void includesAllSupportedLiveklassEventTypes() {
        var eventTypes = eventGenerator.generate(1_000, 20260615L).stream()
                .map(GeneratedEvent::eventType)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(eventTypes).isEqualTo(EnumSet.allOf(EventType.class));
    }

    @Test
    void fillsRequiredEnvelopeFieldsAndTypeSpecificDetail() {
        var events = eventGenerator.generate(100, 20260615L);

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
            assertThat(event.trafficSource()).isNotBlank();
            assertThat(event.userProperties()).isNotNull();
            assertThat(event.userProperties().membershipLevel()).isNotBlank();
            assertThat(event.userProperties().lifetimePurchaseCount()).isGreaterThanOrEqualTo(0);
            assertThat(event.userProperties().lifetimePurchaseAmount()).isNotNull();
            assertThat(event.userProperties().abTestGroup()).isNotBlank();
            assertThat(event.detail()).isNotNull();
        });

        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.View.class));
        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.Click.class));
        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.Request.class));
    }

    @Test
    void mapsWebAndServerSourcesFromEventType() {
        var events = eventGenerator.generate(1_000, 20260615L);

        assertThat(events)
                .filteredOn(event -> event.eventType() == EventType.REQUEST)
                .allSatisfy(event -> assertThat(event.source()).isEqualTo(EventSource.SERVER));
        assertThat(events)
                .filteredOn(event -> event.eventType() != EventType.REQUEST)
                .allSatisfy(event -> assertThat(event.source()).isEqualTo(EventSource.WEB));
    }

    @Test
    void generatesDeterministicEventsForSameSeed() {
        var firstRun = eventGenerator.generate(1_000, 20260615L);
        var secondRun = eventGenerator.generate(1_000, 20260615L);

        assertThat(secondRun).isEqualTo(firstRun);
    }

    @Test
    void rejectsNegativeCount() {
        assertThatThrownBy(() -> eventGenerator.generate(-1, 20260615L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("count must be greater than or equal to 0");
    }
}
