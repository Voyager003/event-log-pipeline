package com.eventlogpipeline.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AnalyticsQueryTests {

    private static final List<String> ANALYTICS_QUERIES = List.of(
            "analytics/preview_usage_purchase_conversion.sql",
            "analytics/course_preview_purchase_conversion.sql"
    );

    @Test
    void providesAnalyticsQueriesForStepThree() {
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
    void previewUsageQueryComparesPreviewUsageGroups() throws IOException {
        String sql = readSql("analytics/preview_usage_purchase_conversion.sql");

        assertThat(sql)
                .contains("user_id")
                .contains("session_id")
                .contains("course_id")
                .contains("preview_usage_group")
                .contains("not_started")
                .contains("started_only")
                .contains("completed")
                .contains("preview_started")
                .contains("preview_completed")
                .contains("purchase_completed");
    }

    @Test
    void coursePreviewQueryComparesCourseLevelPreviewAndPurchaseConversion() throws IOException {
        String sql = readSql("analytics/course_preview_purchase_conversion.sql");

        assertThat(sql)
                .contains("course_id")
                .contains("viewed_sessions")
                .contains("preview_started_sessions")
                .contains("preview_completed_sessions")
                .contains("purchase_count")
                .contains("preview_completion_rate")
                .contains("purchase_conversion_rate")
                .contains("preview_completed")
                .contains("purchase_completed");
    }

    private String readSql(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
