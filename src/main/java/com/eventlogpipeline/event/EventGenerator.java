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

    private static final EventType[] EVENT_TYPES = EventType.values();
    private static final String[] DEVICE_TYPES = {"desktop", "mobile_web", "tablet"};
    private static final String[] TRAFFIC_SOURCES = {"organic", "paid_search", "creator_link", "email"};
    private static final String[] PAGE_NAMES = {"home", "course_detail", "creator_profile", "checkout"};
    private static final String[] COMPONENT_NAMES = {"sign_up", "start_preview", "open_checkout", "purchase_submit"};
    private static final String[] MEMBERSHIP_LEVELS = {"guest", "free", "starter", "pro"};
    private static final String[] AB_TEST_GROUPS = {"control", "variant_a", "variant_b"};
    private static final String SCHEMA_VERSION = "1.0";

    public List<GeneratedEvent> generate(int count, long seed) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be greater than or equal to 0");
        }

        Random random = new Random(seed);
        Instant baseTime = Instant.parse("2026-06-01T00:00:00Z");
        List<GeneratedEvent> events = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            EventType eventType = pickEventType(index, random);
            String userId = formatId("user", random.nextInt(240) + 1);
            String courseId = formatId("course", random.nextInt(30) + 1);
            String creatorId = formatId("creator", random.nextInt(12) + 1);
            EventSource source = eventType == EventType.REQUEST ? EventSource.SERVER : EventSource.WEB;

            events.add(new GeneratedEvent(
                    formatId("event", index + 1),
                    eventType,
                    source,
                    SCHEMA_VERSION,
                    baseTime.plus(random.nextInt(14 * 24 * 60), ChronoUnit.MINUTES),
                    userId,
                    "anon-" + Math.abs(Objects.hash(seed, userId, index)),
                    formatId("session", random.nextInt(500) + 1),
                    pick(DEVICE_TYPES, random),
                    pick(TRAFFIC_SOURCES, random),
                    createUserProperties(random),
                    createDetail(eventType, courseId, creatorId, random)
            ));
        }

        return events;
    }

    private EventType pickEventType(int index, Random random) {
        if (index < EVENT_TYPES.length) {
            return EVENT_TYPES[index];
        }

        int bucket = random.nextInt(100);
        if (bucket < 12) {
            return EventType.LOGIN;
        }
        if (bucket < 47) {
            return EventType.VIEW;
        }
        if (bucket < 72) {
            return EventType.CLICK;
        }
        if (bucket < 92) {
            return EventType.REQUEST;
        }
        return EventType.LOGOUT;
    }

    private EventDetail createDetail(EventType eventType, String courseId, String creatorId, Random random) {
        return switch (eventType) {
            case LOGIN -> new EventDetail.Login("email", "/login");
            case VIEW -> {
                String pageName = pick(PAGE_NAMES, random);
                yield new EventDetail.View(
                        pageName,
                        "/" + pageName.replace('_', '-'),
                        pickReferrer(random),
                        courseId,
                        creatorId
                );
            }
            case CLICK -> new EventDetail.Click(
                    "course_detail",
                    "/course-detail",
                    pick(COMPONENT_NAMES, random),
                    "button",
                    courseId
            );
            case REQUEST -> new EventDetail.Request(
                    "POST",
                    "/api/purchases",
                    "purchase_course",
                    random.nextInt(100) < 92 ? 201 : 400,
                    courseId,
                    randomPrice(random),
                    "KRW"
            );
            case LOGOUT -> new EventDetail.Logout("email", "/account");
        };
    }

    private UserProperties createUserProperties(Random random) {
        int purchaseCount = random.nextInt(24);
        BigDecimal purchaseAmount = BigDecimal.valueOf((long) purchaseCount * (19_000 + random.nextInt(80) * 1_000))
                .setScale(2, RoundingMode.UNNECESSARY);

        return new UserProperties(
                pick(MEMBERSHIP_LEVELS, random),
                purchaseCount,
                purchaseAmount,
                pick(AB_TEST_GROUPS, random)
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
