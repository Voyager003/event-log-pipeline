WITH course_views AS (
    SELECT
        e.user_id,
        e.session_id,
        v.course_id,
        MIN(e.occurred_at) AS viewed_at
    FROM event_logs e
    JOIN view_event_details v ON v.event_id = e.event_id
    WHERE e.event_type = 'course_detail_viewed'
    GROUP BY e.user_id, e.session_id, v.course_id
),
checkout_opens AS (
    SELECT
        e.user_id,
        e.session_id,
        c.course_id,
        MIN(e.occurred_at) AS checkout_opened_at
    FROM event_logs e
    JOIN click_event_details c ON c.event_id = e.event_id
    WHERE e.event_type = 'checkout_opened'
    GROUP BY e.user_id, e.session_id, c.course_id
),
purchase_submits AS (
    SELECT
        e.user_id,
        e.session_id,
        c.course_id,
        MIN(e.occurred_at) AS purchase_submitted_at
    FROM event_logs e
    JOIN click_event_details c ON c.event_id = e.event_id
    WHERE e.event_type = 'purchase_submitted'
    GROUP BY e.user_id, e.session_id, c.course_id
),
purchase_completions AS (
    SELECT
        e.user_id,
        e.session_id,
        r.course_id,
        MIN(e.occurred_at) AS purchase_completed_at
    FROM event_logs e
    JOIN request_event_details r ON r.event_id = e.event_id
    WHERE e.event_type = 'purchase_completed'
    GROUP BY e.user_id, e.session_id, r.course_id
),
funnel AS (
    SELECT
        cv.user_id,
        cv.session_id,
        cv.course_id,
        cv.viewed_at,
        co.checkout_opened_at,
        ps.purchase_submitted_at,
        pc.purchase_completed_at
    FROM course_views cv
    LEFT JOIN checkout_opens co
        ON co.user_id = cv.user_id
        AND co.session_id = cv.session_id
        AND co.course_id = cv.course_id
        AND co.checkout_opened_at >= cv.viewed_at
    LEFT JOIN purchase_submits ps
        ON ps.user_id = cv.user_id
        AND ps.session_id = cv.session_id
        AND ps.course_id = cv.course_id
        AND ps.purchase_submitted_at >= co.checkout_opened_at
    LEFT JOIN purchase_completions pc
        ON pc.user_id = cv.user_id
        AND pc.session_id = cv.session_id
        AND pc.course_id = cv.course_id
        AND pc.purchase_completed_at >= ps.purchase_submitted_at
)
SELECT
    step_order,
    step_name,
    session_count,
    ROUND(session_count::numeric / NULLIF(MAX(session_count) OVER (), 0) * 100, 2) AS conversion_rate_from_view
FROM (
    SELECT 1 AS step_order, 'course_detail_viewed' AS step_name, COUNT(*) AS session_count
    FROM funnel
    UNION ALL
    SELECT 2, 'checkout_opened', COUNT(*) FILTER (WHERE checkout_opened_at IS NOT NULL)
    FROM funnel
    UNION ALL
    SELECT 3, 'purchase_submitted', COUNT(*) FILTER (WHERE purchase_submitted_at IS NOT NULL)
    FROM funnel
    UNION ALL
    SELECT 4, 'purchase_completed', COUNT(*) FILTER (WHERE purchase_completed_at IS NOT NULL)
    FROM funnel
) steps
ORDER BY step_order;
