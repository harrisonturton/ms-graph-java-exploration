package com.demo.teams;

import com.azure.identity.AuthorizationCodeCredentialBuilder;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * It's proving difficult to authenticate the SDK. This class makes it easy to mix and match the
 * different authentication methods so we can find what works.
 *
 * We probably want fromClientSecretCredentials or fromAuthorizationCodeCredential or something
 * with the OBO flow.
 *
 * See also:
 *   - [MSAL Java wiki](https://github.com/AzureAD/microsoft-authentication-library-for-java/wiki)
 *   - [List of auth providers](https://docs.microsoft.com/en-gb/graph/sdks/choose-authentication-providers?tabs=Java#authorization-code-provider)
 */
public class TeamsClientBuilder {
    private final Logger logger = LoggerFactory.getLogger(TeamsClientBuilder.class);
    public static final List<String> SCOPES = List.of(
        // This isn't the correct configuration of scopes. I think they are special-case ones used
        // purely during the auth flow, and cannot be mixed and matched.
        "https://graph.microsoft.com/.default",
        "email",
        "openid",
        "profile",
        "offline_access"
    );

    private String clientId;
    private String clientSecret;
    private String tenantId;

    public TeamsClientBuilder(String clientId, String clientSecret, String tenantId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
    }

    /**
     * Enables *native and web apps* to obtain tokens in the name of the user.
     * This seems like what we want, but I can't seem to get it to work. I think we need
     * the scopes in a different format?
     *
     * See the following:
     *   - https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow
     *   - https://docs.microsoft.com/en-us/azure/active-directory/develop/scenario-web-app-call-api-overview
     */
    GraphServiceClient<Request> fromClientSecretCredentials() {
        // The .default scope refers to the permissions we request in the app registration portal
        var scopes = List.of("https://graph.microsoft.com/.default");
        var tokenCredential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .build();
        var authProvider = new TokenCredentialAuthProvider(scopes, tokenCredential);
        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    /**
     * Enables service applications to run without user interaction. Access is based on the
     * identity of the application. Commonly used for server-to-server interactions that must
     * run in the background, i.e. daemons / service accounts.
     *
     * Enables an application to use its own credentials, rather than impersonating a user.
     *
     * See the following:
     *   - https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-client-creds-grant-flow
     *   - https://docs.microsoft.com/en-us/azure/active-directory/develop/scenario-daemon-app-registration
     */
    public GraphServiceClient<Request> fromAuthorizationCodeCredential(String authorizationCode) {
        // Sends messages as the user
        var authCodeCredential = new AuthorizationCodeCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret) //required for web apps, do not set for native apps
            .authorizationCode(authorizationCode)
            .redirectUrl(getRedirectUri())
            .build();
        var authProvider = new TokenCredentialAuthProvider(SCOPES, authCodeCredential);
        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    /**
     * I think this is intended for desktop/native applications to redirect to the OS browser?
     */
    public GraphServiceClient<Request> fromInteractiveBrowserCredential() {
        var interactiveBrowserCredentials = new InteractiveBrowserCredentialBuilder()
            .clientId(clientId)
            .redirectUrl(getRedirectUri())
            .build();
        var authProvider = new TokenCredentialAuthProvider(SCOPES, interactiveBrowserCredentials);
        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    /**
     * Also probably not what we want. The "challenge" is intended to be checked on the user's device.
     */
    public GraphServiceClient<Request> fromDeviceCredential() {
        var deviceCodeCredential = new DeviceCodeCredentialBuilder()
            .clientId(clientId)
            .challengeConsumer(challenge -> logger.info("Got challenge: {}", challenge.getMessage()))
            .build();
        var authProvider = new TokenCredentialAuthProvider(SCOPES, deviceCodeCredential);
        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    private String getRedirectUri() {
        return "http://localhost:8080/oauth/redirect";
    }
}
