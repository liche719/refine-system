package com.achobeta.refine.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GatewayTokenFilterTest {
    private final GatewayTokenFilter filter = new GatewayTokenFilter("gateway-token-for-tests");

    @Test
    void rejectsDirectApiRequestsWithSpoofedUserIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/overview/get_overview");
        request.addHeader(IdentityHeaders.USER_ID, "spoofed-user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsBlankGatewayTokensAtStartup() {
        assertThatThrownBy(() -> new GatewayTokenFilter(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GATEWAY_TOKEN");
    }

    @Test
    void allowsTrustedGatewayRequestsAndInternalEndpoints() throws Exception {
        MockHttpServletRequest gatewayRequest = new MockHttpServletRequest("GET", "/api/v1/overview/get_overview");
        gatewayRequest.addHeader(IdentityHeaders.GATEWAY_TOKEN, "gateway-token-for-tests");
        MockHttpServletResponse gatewayResponse = new MockHttpServletResponse();
        var gatewayChain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(gatewayRequest, gatewayResponse, gatewayChain);

        verify(gatewayChain).doFilter(gatewayRequest, gatewayResponse);

        MockHttpServletRequest internalRequest = new MockHttpServletRequest("GET", "/internal/v1/mistakes/1/generation-context");
        MockHttpServletResponse internalResponse = new MockHttpServletResponse();
        var internalChain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(internalRequest, internalResponse, internalChain);

        verify(internalChain).doFilter(internalRequest, internalResponse);
    }
}
