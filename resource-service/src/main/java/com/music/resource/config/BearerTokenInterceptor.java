package com.music.resource.config;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/** Attaches a Keycloak service-account bearer token to every outgoing request. */
@Component
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private final KeycloakClientCredentialsTokenSupplier tokenSupplier;

    public BearerTokenInterceptor(KeycloakClientCredentialsTokenSupplier tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().setBearerAuth(tokenSupplier.getAccessToken());
        return execution.execute(request, body);
    }
}
