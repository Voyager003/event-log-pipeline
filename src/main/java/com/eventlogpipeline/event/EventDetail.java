package com.eventlogpipeline.event;

import java.math.BigDecimal;

public sealed interface EventDetail permits
        EventDetail.Login,
        EventDetail.View,
        EventDetail.Click,
        EventDetail.Request,
        EventDetail.Logout {

    record Login(String authProvider, String pageUrl) implements EventDetail {
    }

    record View(String pageName, String pageUrl, String referrer, String courseId, String creatorId)
            implements EventDetail {
    }

    record Click(String pageName, String pageUrl, String componentName, String componentType, String courseId)
            implements EventDetail {
    }

    record Request(String apiMethod, String apiPath, String requestName, int httpStatus, String courseId,
                   BigDecimal amount, String currency) implements EventDetail {
    }

    record Logout(String authProvider, String pageUrl) implements EventDetail {
    }
}
