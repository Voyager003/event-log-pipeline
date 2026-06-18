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
                    membership_level,
                    lifetime_purchase_count,
                    lifetime_purchase_amount
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
                    :membershipLevel,
                    :lifetimePurchaseCount,
                    :lifetimePurchaseAmount
                )
                """, Map.ofEntries(
                Map.entry("eventId", event.eventId()),
                Map.entry("eventType", event.eventType().eventName()),
                Map.entry("source", event.source().name()),
                Map.entry("schemaVersion", event.schemaVersion()),
                Map.entry("occurredAt", Timestamp.from(event.occurredAt())),
                Map.entry("userId", event.userId()),
                Map.entry("anonymousId", event.anonymousId()),
                Map.entry("sessionId", event.sessionId()),
                Map.entry("deviceType", event.deviceType()),
                Map.entry("membershipLevel", event.userProperties().membershipLevel()),
                Map.entry("lifetimePurchaseCount", event.userProperties().lifetimePurchaseCount()),
                Map.entry("lifetimePurchaseAmount", event.userProperties().lifetimePurchaseAmount())
        ));
    }

    private void saveDetail(GeneratedEvent event) {
        switch (event.detail()) {
            case EventDetail.Login login -> saveAuthDetail(event.eventId(), login.authProvider(), login.pageUrl());
            case EventDetail.View view -> saveViewDetail(event.eventId(), view);
            case EventDetail.Preview preview -> savePreviewDetail(event.eventId(), preview);
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
                    creator_id,
                    course_price
                ) VALUES (
                    :eventId,
                    :pageName,
                    :pageUrl,
                    :referrer,
                    :courseId,
                    :creatorId,
                    :coursePrice
                )
                """, Map.of(
                "eventId", eventId,
                "pageName", detail.pageName(),
                "pageUrl", detail.pageUrl(),
                "referrer", detail.referrer(),
                "courseId", detail.courseId(),
                "creatorId", detail.creatorId(),
                "coursePrice", detail.coursePrice()
        ));
    }

    private void savePreviewDetail(String eventId, EventDetail.Preview detail) {
        jdbcTemplate.update("""
                INSERT INTO preview_event_details (
                    event_id,
                    page_name,
                    page_url,
                    course_id,
                    creator_id,
                    preview_id,
                    preview_title,
                    preview_length_seconds,
                    watched_seconds,
                    completion_rate
                ) VALUES (
                    :eventId,
                    :pageName,
                    :pageUrl,
                    :courseId,
                    :creatorId,
                    :previewId,
                    :previewTitle,
                    :previewLengthSeconds,
                    :watchedSeconds,
                    :completionRate
                )
                """, Map.of(
                "eventId", eventId,
                "pageName", detail.pageName(),
                "pageUrl", detail.pageUrl(),
                "courseId", detail.courseId(),
                "creatorId", detail.creatorId(),
                "previewId", detail.previewId(),
                "previewTitle", detail.previewTitle(),
                "previewLengthSeconds", detail.previewLengthSeconds(),
                "watchedSeconds", detail.watchedSeconds(),
                "completionRate", detail.completionRate()
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
                    course_id,
                    creator_id,
                    payment_method,
                    amount,
                    currency
                ) VALUES (
                    :eventId,
                    :pageName,
                    :pageUrl,
                    :componentName,
                    :componentType,
                    :courseId,
                    :creatorId,
                    :paymentMethod,
                    :amount,
                    :currency
                )
                """, Map.of(
                "eventId", eventId,
                "pageName", detail.pageName(),
                "pageUrl", detail.pageUrl(),
                "componentName", detail.componentName(),
                "componentType", detail.componentType(),
                "courseId", detail.courseId(),
                "creatorId", detail.creatorId(),
                "paymentMethod", detail.paymentMethod(),
                "amount", detail.amount(),
                "currency", detail.currency()
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
                    request_id,
                    purchase_id,
                    course_id,
                    creator_id,
                    amount,
                    currency,
                    payment_method
                ) VALUES (
                    :eventId,
                    :apiMethod,
                    :apiPath,
                    :requestName,
                    :httpStatus,
                    :requestId,
                    :purchaseId,
                    :courseId,
                    :creatorId,
                    :amount,
                    :currency,
                    :paymentMethod
                )
                """, Map.ofEntries(
                Map.entry("eventId", eventId),
                Map.entry("apiMethod", detail.apiMethod()),
                Map.entry("apiPath", detail.apiPath()),
                Map.entry("requestName", detail.requestName()),
                Map.entry("httpStatus", detail.httpStatus()),
                Map.entry("requestId", detail.requestId()),
                Map.entry("purchaseId", detail.purchaseId()),
                Map.entry("courseId", detail.courseId()),
                Map.entry("creatorId", detail.creatorId()),
                Map.entry("amount", detail.amount()),
                Map.entry("currency", detail.currency()),
                Map.entry("paymentMethod", detail.paymentMethod())
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
