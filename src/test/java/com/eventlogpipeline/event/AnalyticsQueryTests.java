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
            "analytics/hourly_event_trends.sql",
            "analytics/purchase_click_to_request_funnel.sql",
            "analytics/request_failure_rate.sql"
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
    void funnelQueryUsesClickAndRequestDetailTables() throws IOException {
        String sql = readSql("analytics/purchase_click_to_request_funnel.sql");

        assertThat(sql)
                .contains("click_event_details")
                .contains("request_event_details")
                .contains("purchase_submit")
                .contains("purchase_course");
    }

    @Test
    void timeSeriesQueryUsesHourlyBuckets() throws IOException {
        String sql = readSql("analytics/hourly_event_trends.sql");

        assertThat(sql)
                .contains("date_trunc('hour', occurred_at)")
                .contains("GROUP BY event_hour, event_type");
    }

    private String readSql(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
