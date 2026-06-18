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
    private static final String[] MEMBERSHIP_LEVELS = {"guest", "free", "starter", "pro"};
    private static final String[] PAYMENT_METHODS = {"card", "kakao_pay", "naver_pay"};
    private static final String SCHEMA_VERSION = "1.0";
    private static final int PREVIEW_START_RATE = 60;
    private static final int PREVIEW_COMPLETE_RATE = 68;
    private static final int CHECKOUT_OPEN_AFTER_PREVIEW_COMPLETE_RATE = 72;
    private static final int PURCHASE_SUBMIT_RATE = 78;
    private static final int PURCHASE_SUCCESS_RATE = 85;
    private static final int PREVIEW_LENGTH_SECONDS = 180;

    public List<GeneratedEvent> generate(int count, long seed) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be greater than or equal to 0");
        }

        Random random = new Random(seed);
        Instant baseTime = Instant.parse("2026-06-01T00:00:00Z");
        List<GeneratedEvent> events = new ArrayList<>(count);
        int eventNumber = 1;
        int journeyNumber = 1;

        while (events.size() < count) {
            String userId = formatId("user", random.nextInt(240) + 1);
            String courseId = formatId("course", random.nextInt(30) + 1);
            String creatorId = formatId("creator", random.nextInt(12) + 1);
            String previewId = formatId("preview", random.nextInt(30) + 1);
            String sessionId = formatId("session", journeyNumber);
            String anonymousId = "anon-" + Math.abs(Objects.hash(seed, userId, sessionId));
            String deviceType = pick(DEVICE_TYPES, random);
            UserProperties userProperties = createUserProperties(random);
            BigDecimal coursePrice = randomPrice(random);
            Instant journeyStart = baseTime.plus(random.nextInt(14 * 24 * 60), ChronoUnit.MINUTES);

            eventNumber = appendJourneyEvents(
                    events,
                    count,
                    eventNumber,
                    random,
                    journeyStart,
                    userId,
                    anonymousId,
                    sessionId,
                    deviceType,
                    userProperties,
                    courseId,
                    creatorId,
                    previewId,
                    coursePrice
            );
            journeyNumber++;
        }

        return events;
    }

    private int appendJourneyEvents(
            List<GeneratedEvent> events,
            int maxCount,
            int eventNumber,
            Random random,
            Instant journeyStart,
            String userId,
            String anonymousId,
            String sessionId,
            String deviceType,
            UserProperties userProperties,
            String courseId,
            String creatorId,
            String previewId,
            BigDecimal coursePrice
    ) {
        Instant eventTime = journeyStart;
        eventNumber = addIfPossible(events, maxCount, eventNumber, EventType.COURSE_DETAIL_VIEWED, eventTime,
                userId, anonymousId, sessionId, deviceType, userProperties,
                new EventDetail.View(
                        "course_detail",
                        "/courses/" + courseId,
                        pickReferrer(random),
                        courseId,
                        creatorId,
                        coursePrice
                ));

        if (random.nextInt(100) < PREVIEW_START_RATE) {
            eventTime = eventTime.plus(1 + random.nextInt(4), ChronoUnit.MINUTES);
            eventNumber = addIfPossible(events, maxCount, eventNumber, EventType.PREVIEW_STARTED,
                    eventTime,
                    userId, anonymousId, sessionId, deviceType, userProperties,
                    new EventDetail.Preview(
                            "course_detail",
                            "/courses/" + courseId,
                            courseId,
                            creatorId,
                            previewId,
                            "intro_preview",
                            PREVIEW_LENGTH_SECONDS,
                            0,
                            BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY)
                    ));
        } else {
            return eventNumber;
        }

        if (random.nextInt(100) >= PREVIEW_COMPLETE_RATE) {
            return eventNumber;
        }

        eventTime = eventTime.plus(2 + random.nextInt(4), ChronoUnit.MINUTES);
        eventNumber = addIfPossible(events, maxCount, eventNumber, EventType.PREVIEW_COMPLETED,
                eventTime,
                userId, anonymousId, sessionId, deviceType, userProperties,
                new EventDetail.Preview(
                        "course_detail",
                        "/courses/" + courseId,
                        courseId,
                        creatorId,
                        previewId,
                        "intro_preview",
                        PREVIEW_LENGTH_SECONDS,
                        PREVIEW_LENGTH_SECONDS,
                        BigDecimal.valueOf(100).setScale(2, RoundingMode.UNNECESSARY)
                ));

        if (random.nextInt(100) >= CHECKOUT_OPEN_AFTER_PREVIEW_COMPLETE_RATE) {
            return eventNumber;
        }

        eventTime = eventTime.plus(1 + random.nextInt(4), ChronoUnit.MINUTES);
        eventNumber = addIfPossible(events, maxCount, eventNumber, EventType.CHECKOUT_OPENED,
                eventTime,
                userId, anonymousId, sessionId, deviceType, userProperties,
                new EventDetail.Click(
                        "course_detail",
                        "/courses/" + courseId,
                        "open_checkout",
                        "button",
                        courseId,
                        creatorId,
                        "",
                        BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY),
                        ""
                ));

        if (random.nextInt(100) >= PURCHASE_SUBMIT_RATE) {
            return eventNumber;
        }

        String paymentMethod = pick(PAYMENT_METHODS, random);
        eventTime = eventTime.plus(1 + random.nextInt(4), ChronoUnit.MINUTES);
        eventNumber = addIfPossible(events, maxCount, eventNumber, EventType.PURCHASE_SUBMITTED,
                eventTime,
                userId, anonymousId, sessionId, deviceType, userProperties,
                new EventDetail.Click(
                        "checkout",
                        "/courses/" + courseId + "/checkout",
                        "purchase_submit",
                        "button",
                        courseId,
                        creatorId,
                        paymentMethod,
                        coursePrice,
                        "KRW"
                ));

        boolean successfulPurchase = random.nextInt(100) < PURCHASE_SUCCESS_RATE;
        if (!successfulPurchase) {
            return eventNumber;
        }

        String requestId = formatId("request", eventNumber);
        String purchaseId = formatId("purchase", eventNumber);

        eventTime = eventTime.plus(1 + random.nextInt(3), ChronoUnit.MINUTES);
        return addIfPossible(events, maxCount, eventNumber, EventType.PURCHASE_COMPLETED,
                eventTime,
                userId, anonymousId, sessionId, deviceType, userProperties,
                new EventDetail.Request(
                        "POST",
                        "/api/purchases",
                        EventType.PURCHASE_COMPLETED.eventName(),
                        201,
                        requestId,
                        purchaseId,
                        courseId,
                        creatorId,
                        coursePrice,
                        "KRW",
                        paymentMethod
                ));
    }

    private int addIfPossible(
            List<GeneratedEvent> events,
            int maxCount,
            int eventNumber,
            EventType eventType,
            Instant occurredAt,
            String userId,
            String anonymousId,
            String sessionId,
            String deviceType,
            UserProperties userProperties,
            EventDetail detail
    ) {
        if (events.size() >= maxCount) {
            return eventNumber;
        }

        events.add(new GeneratedEvent(
                formatId("event", eventNumber),
                eventType,
                sourceFor(eventType),
                SCHEMA_VERSION,
                occurredAt,
                userId,
                anonymousId,
                sessionId,
                deviceType,
                userProperties,
                detail
        ));

        return eventNumber + 1;
    }

    private EventSource sourceFor(EventType eventType) {
        return switch (eventType) {
            case PURCHASE_COMPLETED -> EventSource.SERVER;
            case COURSE_DETAIL_VIEWED, PREVIEW_STARTED, PREVIEW_COMPLETED, CHECKOUT_OPENED, PURCHASE_SUBMITTED ->
                    EventSource.WEB;
        };
    }

    private UserProperties createUserProperties(Random random) {
        int purchaseCount = random.nextInt(24);
        BigDecimal purchaseAmount = BigDecimal.valueOf((long) purchaseCount * (19_000 + random.nextInt(80) * 1_000))
                .setScale(2, RoundingMode.UNNECESSARY);

        return new UserProperties(
                pick(MEMBERSHIP_LEVELS, random),
                purchaseCount,
                purchaseAmount
        );
    }

    private BigDecimal randomPrice(Random random) {
        int amount = 19_000 + random.nextInt(181) * 1_000;
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.UNNECESSARY);
    }

    private String pickReferrer(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> "https://www.google.com";
            case 1 -> "https://creator.example.com";
            case 2 -> "https://liveklass.example.com";
            default -> "";
        };
    }

    private String pick(String[] values, Random random) {
        return values[random.nextInt(values.length)];
    }

    private String formatId(String prefix, int number) {
        return "%s-%04d".formatted(prefix, number);
    }
}
