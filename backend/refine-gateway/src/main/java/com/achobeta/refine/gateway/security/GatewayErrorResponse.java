package com.achobeta.refine.gateway.security;

record GatewayErrorResponse(String traceId, int code, String info, Object data) { }

