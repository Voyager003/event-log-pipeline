package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Comparator;
import java.util.stream.Collectors;

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
            assertThat(event.userProperties()).isNotNull();
            assertThat(event.userProperties().membershipLevel()).isNotBlank();
            assertThat(event.userProperties().lifetimePurchaseCount()).isGreaterThanOrEqualTo(0);
            assertThat(event.userProperties().lifetimePurchaseAmount()).isNotNull();
            assertThat(event.detail()).isNotNull();
        });

        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.View.class));
        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.Click.class));
        assertThat(events).anySatisfy(event -> assertThat(event.detail()).isInstanceOf(EventDetail.Request.class));
    }

    @Test
    void mapsWebAndServerSourcesFromBusinessEventType() {
        var events = eventGenerator.generate(1_000, 20260615L);

        assertThat(events)
                .filteredOn(event -> event.eventType() == EventType.PURCHASE_COMPLETED)
                .allSatisfy(event -> assertThat(event.source()).isEqualTo(EventSource.SERVER));
        assertThat(events)
                .filteredOn(event -> event.eventType() != EventType.PURCHASE_COMPLETED)
                .allSatisfy(event -> assertThat(event.source()).isEqualTo(EventSource.WEB));
    }

    @Test
    void createsPreviewDrivenPurchaseJourneyWithinSameSessionAndCourse() {
        var events = eventGenerator.generate(1_000, 20260615L);

        var completedPurchase = events.stream()
                .filter(event -> event.eventType() == EventType.PURCHASE_COMPLETED)
                .findFirst()
                .orElseThrow();
        var requestDetail = (EventDetail.Request) completedPurchase.detail();

        var journeyEvents = events.stream()
                .filter(event -> event.userId().equals(completedPurchase.userId()))
                .filter(event -> event.sessionId().equals(completedPurchase.sessionId()))
                .filter(event -> hasCourseId(event, requestDetail.courseId()))
                .sorted(Comparator.comparing(GeneratedEvent::occurredAt))
                .toList();

        assertThat(journeyEvents)
                .extracting(GeneratedEvent::eventType)
                .containsSubsequence(
                        EventType.COURSE_DETAIL_VIEWED,
                        EventType.PREVIEW_STARTED,
                        EventType.PREVIEW_COMPLETED,
                        EventType.CHECKOUT_OPENED,
                        EventType.PURCHASE_SUBMITTED,
                        EventType.PURCHASE_COMPLETED
                );
    }

    @Test
    void generatesRealisticPurchaseFunnelDistribution() {
        var counts = eventGenerator.generate(1_000, 20260615L).stream()
                .collect(Collectors.groupingBy(GeneratedEvent::eventType, Collectors.counting()));

        long courseViews = counts.getOrDefault(EventType.COURSE_DETAIL_VIEWED, 0L);
        long previewStarts = counts.getOrDefault(EventType.PREVIEW_STARTED, 0L);
        long previewCompletions = counts.getOrDefault(EventType.PREVIEW_COMPLETED, 0L);
        long checkoutOpens = counts.getOrDefault(EventType.CHECKOUT_OPENED, 0L);
        long purchaseSubmits = counts.getOrDefault(EventType.PURCHASE_SUBMITTED, 0L);
        long purchaseCompletions = counts.getOrDefault(EventType.PURCHASE_COMPLETED, 0L);

        assertThat(courseViews).isGreaterThan(previewStarts);
        assertThat(previewStarts).isGreaterThan(previewCompletions);
        assertThat(previewCompletions).isGreaterThan(checkoutOpens);
        assertThat(checkoutOpens).isGreaterThan(purchaseSubmits);
        assertThat(purchaseSubmits).isGreaterThan(purchaseCompletions);
    }

    @Test
    void generatesPurchaseCompletedOnlyAfterSubmittedEvent() {
        var events = eventGenerator.generate(1_000, 20260615L);

        var purchaseResults = events.stream()
                .filter(event -> event.eventType() == EventType.PURCHASE_COMPLETED)
                .toList();

        assertThat(purchaseResults).allSatisfy(result -> {
            var resultDetail = (EventDetail.Request) result.detail();
            assertThat(events)
                    .filteredOn(event -> event.userId().equals(result.userId()))
                    .filteredOn(event -> event.sessionId().equals(result.sessionId()))
                    .filteredOn(event -> hasCourseId(event, resultDetail.courseId()))
                    .filteredOn(event -> event.eventType() == EventType.PURCHASE_SUBMITTED)
                    .filteredOn(event -> event.occurredAt().isBefore(result.occurredAt())
                            || event.occurredAt().equals(result.occurredAt()))
                    .hasSize(1);
        });
    }

    @Test
    void exposesReadableSnakeCaseEventNamesForStorageAndAnalytics() {
        assertThat(EventType.COURSE_DETAIL_VIEWED.eventName()).isEqualTo("course_detail_viewed");
        assertThat(EventType.PREVIEW_COMPLETED.eventName()).isEqualTo("preview_completed");
        assertThat(EventType.PURCHASE_COMPLETED.eventName()).isEqualTo("purchase_completed");
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

    private boolean hasCourseId(GeneratedEvent event, String courseId) {
        return switch (event.detail()) {
            case EventDetail.View view -> courseId.equals(view.courseId());
            case EventDetail.Click click -> courseId.equals(click.courseId());
            case EventDetail.Preview preview -> courseId.equals(preview.courseId());
            case EventDetail.Request request -> courseId.equals(request.courseId());
            case EventDetail.Login ignored -> false;
            case EventDetail.Logout ignored -> false;
        };
    }
}
