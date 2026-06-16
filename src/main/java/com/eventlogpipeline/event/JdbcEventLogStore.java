package com.eventlogpipeline.event;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Profile("db")
@Repository
public class JdbcEventLogStore implements EventLogStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcEventLogStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public int saveAll(List<GeneratedEvent> events) {
        int saved = 0;

        for (GeneratedEvent event : events) {
            saveEnvelope(event);
            saveDetail(event);
            saved++;
        }

        return saved;
    }

    private void saveEnvelope(GeneratedEvent event) {
        jdbcTemplate.update("""
                INSERT INTO event_logs (
                    event_id,
                    event_type,
                    source,
                    schema_version,
                    occurred_at,
                    user_id,
                    anonymous_id,
                    session_id,
                    device_type,
                    traffic_source,
                    membership_level,
                    lifetime_purchase_count,
                    lifetime_purchase_amount,
                    ab_test_group
                ) VALUES (
                    :eventId,
                    :eventType,
                    :source,
                    :schemaVersion,
                    :occurredAt,
                    :userId,
                    :anonymousId,
                    :sessionId,
                    :deviceType,
                    :trafficSource,
                    :membershipLevel,
                    :lifetimePurchaseCount,
                    :lifetimePurchaseAmount,
                    :abTestGroup
                )
                """, Map.ofEntries(
                Map.entry("eventId", event.eventId()),
                Map.entry("eventType", event.eventType().name()),
                Map.entry("source", event.source().name()),
                Map.entry("schemaVersion", event.schemaVersion()),
                Map.entry("occurredAt", Timestamp.from(event.occurredAt())),
                Map.entry("userId", event.userId()),
                Map.entry("anonymousId", event.anonymousId()),
                Map.entry("sessionId", event.sessionId()),
                Map.entry("deviceType", event.deviceType()),
                Map.entry("trafficSource", event.trafficSource()),
                Map.entry("membershipLevel", event.userProperties().membershipLevel()),
                Map.entry("lifetimePurchaseCount", event.userProperties().lifetimePurchaseCount()),
                Map.entry("lifetimePurchaseAmount", event.userProperties().lifetimePurchaseAmount()),
                Map.entry("abTestGroup", event.userProperties().abTestGroup())
        ));
    }

    private void saveDetail(GeneratedEvent event) {
        switch (event.detail()) {
            case EventDetail.Login login -> saveAuthDetail(event.eventId(), login.authProvider(), login.pageUrl());
            case EventDetail.View view -> saveViewDetail(event.eventId(), view);
            case EventDetail.Click click -> saveClickDetail(event.eventId(), click);
            case EventDetail.Request request -> saveRequestDetail(event.eventId(), request);
            case EventDetail.Logout logout -> saveAuthDetail(event.eventId(), logout.authProvider(), logout.pageUrl());
        }
    }

    private void saveViewDetail(String eventId, EventDetail.View detail) {
        jdbcTemplate.update("""
                INSERT INTO view_event_details (
                    event_id,
                    page_name,
                    page_url,
                    referrer,
                    course_id,
                    creator_id
                ) VALUES (
                    :eventId,
                    :pageName,
                    :pageUrl,
                    :referrer,
                    :courseId,
                    :creatorId
                )
                """, Map.of(
                "eventId", eventId,
                "pageName", detail.pageName(),
                "pageUrl", detail.pageUrl(),
                "referrer", detail.referrer(),
                "courseId", detail.courseId(),
                "creatorId", detail.creatorId()
        ));
    }

    private void saveClickDetail(String eventId, EventDetail.Click detail) {
        jdbcTemplate.update("""
                INSERT INTO click_event_details (
                    event_id,
                    page_name,
                    page_url,
                    component_name,
                    component_type,
                    course_id
                ) VALUES (
                    :eventId,
                    :pageName,
                    :pageUrl,
                    :componentName,
                    :componentType,
                    :courseId
                )
                """, Map.of(
                "eventId", eventId,
                "pageName", detail.pageName(),
                "pageUrl", detail.pageUrl(),
                "componentName", detail.componentName(),
                "componentType", detail.componentType(),
                "courseId", detail.courseId()
        ));
    }

    private void saveRequestDetail(String eventId, EventDetail.Request detail) {
        jdbcTemplate.update("""
                INSERT INTO request_event_details (
                    event_id,
                    api_method,
                    api_path,
                    request_name,
                    http_status,
                    course_id,
                    amount,
                    currency
                ) VALUES (
                    :eventId,
                    :apiMethod,
                    :apiPath,
                    :requestName,
                    :httpStatus,
                    :courseId,
                    :amount,
                    :currency
                )
                """, Map.of(
                "eventId", eventId,
                "apiMethod", detail.apiMethod(),
                "apiPath", detail.apiPath(),
                "requestName", detail.requestName(),
                "httpStatus", detail.httpStatus(),
                "courseId", detail.courseId(),
                "amount", detail.amount(),
                "currency", detail.currency()
        ));
    }

    private void saveAuthDetail(String eventId, String authProvider, String pageUrl) {
        jdbcTemplate.update("""
                INSERT INTO auth_event_details (
                    event_id,
                    auth_provider,
                    page_url
                ) VALUES (
                    :eventId,
                    :authProvider,
                    :pageUrl
                )
                """, Map.of(
                "eventId", eventId,
                "authProvider", authProvider,
                "pageUrl", pageUrl
        ));
    }
}
