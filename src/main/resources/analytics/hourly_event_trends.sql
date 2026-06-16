SELECT
    date_trunc('hour', occurred_at) AS event_hour,
    event_type,
    COUNT(*) AS event_count
FROM event_logs
GROUP BY event_hour, event_type
ORDER BY event_hour, event_type;
