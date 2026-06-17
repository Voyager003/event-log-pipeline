WITH viewed_sessions AS (
    SELECT DISTINCT
        e.traffic_source,
        e.user_id,
        e.session_id,
        v.course_id
    FROM event_logs e
    JOIN view_event_details v ON v.event_id = e.event_id
    WHERE e.event_type = 'course_detail_viewed'
),
completed_sessions AS (
    SELECT DISTINCT
        e.traffic_source,
        e.user_id,
        e.session_id,
        r.course_id
    FROM event_logs e
    JOIN request_event_details r ON r.event_id = e.event_id
    WHERE e.event_type = 'purchase_completed'
)
SELECT
    v.traffic_source,
    COUNT(*) AS viewed_sessions,
    COUNT(c.session_id) AS completed_sessions,
    ROUND(COUNT(c.session_id)::numeric / NULLIF(COUNT(*), 0) * 100, 2) AS purchase_conversion_rate
FROM viewed_sessions v
LEFT JOIN completed_sessions c
    ON c.traffic_source = v.traffic_source
    AND c.user_id = v.user_id
    AND c.session_id = v.session_id
    AND c.course_id = v.course_id
GROUP BY v.traffic_source
ORDER BY purchase_conversion_rate DESC;
