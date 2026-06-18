package com.eventlogpipeline.event;

import java.math.BigDecimal;

public record UserProperties(
        String membershipLevel,
        int lifetimePurchaseCount,
        BigDecimal lifetimePurchaseAmount
) {
}
