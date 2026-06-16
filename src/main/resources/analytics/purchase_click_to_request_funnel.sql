WITH purchase_clicks AS (
    SELECT
        e.user_id,
        e.session_id,
        c.course_id,
        MIN(e.occurred_at) AS first_clicked_at
    FROM event_logs e
    JOIN click_event_details c ON c.event_id = e.event_id
    WHERE c.component_name = 'purchase_submit'
    GROUP BY e.user_id, e.session_id, c.course_id
),
purchase_requests AS (
    SELECT
        e.user_id,
        e.session_id,
        r.course_id,
        MIN(e.occurred_at) AS first_requested_at
    FROM event_logs e
    JOIN request_event_details r ON r.event_id = e.event_id
    WHERE r.request_name = 'purchase_course'
    GROUP BY e.user_id, e.session_id, r.course_id
)
SELECT
    COUNT(*) AS purchase_button_clicks,
    COUNT(pr.first_requested_at) AS purchase_requests,
    ROUND(
        COUNT(pr.first_requested_at)::numeric / NULLIF(COUNT(*), 0) * 100,
        2
    ) AS click_to_request_rate
FROM purchase_clicks pc
LEFT JOIN purchase_requests pr
    ON pr.user_id = pc.user_id
    AND pr.session_id = pc.session_id
    AND pr.course_id = pc.course_id
    AND pr.first_requested_at >= pc.first_clicked_at;
