package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AnalyticsQueryTests {

    private static final List<String> ANALYTICS_QUERIES = List.of(
            "analytics/event_type_counts.sql",
            "analytics/course_purchase_funnel.sql",
            "analytics/ab_test_purchase_conversion.sql",
            "analytics/traffic_source_purchase_conversion.sql",
            "analytics/purchase_failure_rate.sql"
    );

    @Test
    void providesFourAnalyticsQueriesForStepThree() {
        assertThat(ANALYTICS_QUERIES)
                .allSatisfy(path -> assertThat(new ClassPathResource(path).exists()).isTrue());
    }

    @Test
    void queryFilesUseStoredColumnsInsteadOfJsonPayloads() throws IOException {
        for (String path : ANALYTICS_QUERIES) {
            String sql = readSql(path);

            assertThat(sql).contains("SELECT");
            assertThat(sql).doesNotContainIgnoringCase("json");
            assertThat(sql).doesNotContainIgnoringCase("payload");
        }
    }

    @Test
    void funnelQueryUsesJourneyIdentifiersAndBusinessEvents() throws IOException {
        String sql = readSql("analytics/course_purchase_funnel.sql");

        assertThat(sql)
                .contains("user_id")
                .contains("session_id")
                .contains("course_id")
                .contains("course_detail_viewed")
                .contains("checkout_opened")
                .contains("purchase_submitted")
                .contains("purchase_completed");
    }

    @Test
    void segmentQueriesUseUserPropertiesAndAcquisitionContext() throws IOException {
        String abTestSql = readSql("analytics/ab_test_purchase_conversion.sql");
        String trafficSourceSql = readSql("analytics/traffic_source_purchase_conversion.sql");

        assertThat(abTestSql)
                .contains("ab_test_group")
                .contains("purchase_completed");
        assertThat(trafficSourceSql)
                .contains("traffic_source")
                .contains("purchase_completed");
    }

    @Test
    void failureQueryUsesPurchaseFailureDetails() throws IOException {
        String sql = readSql("analytics/purchase_failure_rate.sql");

        assertThat(sql)
                .contains("request_event_details")
                .contains("purchase_failed")
                .contains("failure_reason");
    }

    private String readSql(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
