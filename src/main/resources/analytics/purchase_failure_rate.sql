SELECT
    COALESCE(NULLIF(failure_reason, ''), 'none') AS failure_reason,
    COUNT(*) AS failed_requests,
    ROUND(
        COUNT(*)::numeric / NULLIF(SUM(COUNT(*)) OVER (), 0) * 100,
        2
    ) AS failure_reason_share
FROM event_logs e
JOIN request_event_details r ON r.event_id = e.event_id
WHERE e.event_type = 'purchase_failed'
GROUP BY COALESCE(NULLIF(failure_reason, ''), 'none')
ORDER BY failed_requests DESC;
