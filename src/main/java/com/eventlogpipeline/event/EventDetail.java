package com.eventlogpipeline.event;

import java.math.BigDecimal;

public sealed interface EventDetail permits
        EventDetail.Login,
        EventDetail.View,
        EventDetail.Preview,
        EventDetail.Click,
        EventDetail.Request,
        EventDetail.Logout {

    record Login(String authProvider, String pageUrl) implements EventDetail {
    }

    record View(String pageName, String pageUrl, String referrer, String courseId, String creatorId,
                BigDecimal coursePrice)
            implements EventDetail {
    }

    record Preview(String pageName, String pageUrl, String courseId, String creatorId, String previewId,
                   String previewTitle, int previewLengthSeconds, int watchedSeconds, BigDecimal completionRate)
            implements EventDetail {
    }

    record Click(String pageName, String pageUrl, String componentName, String componentType, String courseId,
                 String creatorId, String paymentMethod, BigDecimal amount, String currency)
            implements EventDetail {
    }

    record Request(String apiMethod, String apiPath, String requestName, int httpStatus, String requestId,
                   String purchaseId, String courseId, String creatorId, BigDecimal amount, String currency,
                   String paymentMethod) implements EventDetail {
    }

    record Logout(String authProvider, String pageUrl) implements EventDetail {
    }
}
