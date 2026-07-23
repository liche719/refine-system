package com.achobeta.refine.contracts.event;

import java.time.Instant;

public record UserLoggedInPayload(Instant loginAt) {
}
