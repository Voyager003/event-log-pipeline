SELECT
    event_type,
    COUNT(*) AS event_count
FROM event_logs
GROUP BY event_type
ORDER BY event_count DESC;
