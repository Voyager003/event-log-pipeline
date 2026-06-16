SELECT
    COUNT(*) AS total_requests,
    COUNT(*) FILTER (WHERE http_status >= 400) AS failed_requests,
    ROUND(
        COUNT(*) FILTER (WHERE http_status >= 400)::numeric / NULLIF(COUNT(*), 0) * 100,
        2
    ) AS failure_rate
FROM request_event_details;
