package com.demo.teams;

import com.demo.teams.TeamsRepository.AccessToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;

@RestController
public class TeamsController {
    private final Logger logger = LoggerFactory.getLogger(TeamsController.class);

    private final String clientId;
    private String clientSecret;
    private TeamsClientBuilder teamsClientBuilder;

    public TeamsController(
        @Value("${teams.client.client.id}") String clientId,
        @Value("${teams.client.client.secret}") String clientSecret,
        TeamsClientBuilder teamsClientBuilder
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.teamsClientBuilder = teamsClientBuilder;
    }

    // Redirects the user to start the oAuth flow
    @GetMapping("/connect")
    public RedirectView connect() {
        var redirectUrl = getAuthorizeRedirectUrl(clientId);
        logger.info("Generated oAuth URI: {}", redirectUrl);
        return new RedirectView(redirectUrl);
    }

    @PostMapping("/oauth/redirect")
    public String authorizeRedirect(WebRequest authorizeReq) throws JsonProcessingException {
        var error = authorizeReq.getParameter("error");
        var errorDescription = authorizeReq.getParameter("errorDescription");
        if (error != null) {
            logger.error("failed to authorize with error code: {} and description: {}", error, errorDescription);
            return "Failed to authorize :(";
        }
        //  Response also has an "id_token" and "state" parameters
        var code = authorizeReq.getParameter("code");

        var client = new OkHttpClient();
        var getTokenReq = getAccessTokenRequest(clientId, clientSecret, code);
        String tokenJson;
        try {
            var getTokenRes = client.newCall(getTokenReq).execute();
            if (!getTokenRes.isSuccessful()) {
                return getTokenRes.body().string();
            }
            tokenJson = getTokenRes.body().string();
        } catch (IOException err) {
            return String.format("Failed with error: %s", err);
        }

        // TODO: do something with the access token
        var token = new ObjectMapper().readValue(tokenJson, AccessToken.class);
        return tokenJson;
    }

    private String getAuthorizeRedirectUrl(String clientId) {
        var uri = new HttpUrl.Builder()
            .scheme("https")
            .host("login.microsoftonline.com")
            .addPathSegment("organizations/oauth2/v2.0/authorize")
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("response_type", "code id_token")
            .addQueryParameter("redirect_uri", getRedirectUri())
            // We cannot use the "query" response mode when requesting an ID token. Instead, we must
            // use "fragment" or "form_post". Since browsers don't send fragment-encoded parameters
            // to the server, we can only use the "form_post" response mode.
            // https://docs.microsoft.com/en-gb/azure/active-directory/develop/v2-oauth2-auth-code-flow
            .addQueryParameter("response_mode", "form_post")
            .addQueryParameter("scope", "openid")
            .addQueryParameter("state", "1234")
            .addQueryParameter("nonce", "1234")
            .build();
        return uri.toString();
    }

    private Request getAccessTokenRequest(String clientId, String clientSecret, String code) {
        var url = "https://login.microsoftonline.com/organizations/oauth2/v2.0/token";
        var body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("client_id", clientId)
            .addFormDataPart("client_secret", clientSecret)
            .addFormDataPart("scope", "openid profile email")
            .addFormDataPart("code", code)
            .addFormDataPart("redirect_uri", getRedirectUri())
            .addFormDataPart("grant_type", "authorization_code")
            .build();
        return new Request.Builder()
            .url(url)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(body)
            .build();
    }

    private String getRedirectUri() {
        return "http://localhost:8080/oauth/redirect";
    }
}