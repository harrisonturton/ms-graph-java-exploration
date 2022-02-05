package com.demo.teams;

import com.microsoft.graph.models.ChatMessage;
import com.microsoft.graph.models.ItemBody;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Nullable;

@RestController
public class TeamsController {
    private final Logger logger = LoggerFactory.getLogger(TeamsController.class);
    private static final String REDIRECT_URL = "http://localhost:8080/oauth/redirect";

    private final String clientId;
    private String clientSecret;
    private String tenantId;
    private TeamsClientBuilder teamsClientBuilder;

    public TeamsController(
        @Value("${teams.client.client.id}") String clientId,
        @Value("${teams.client.client.secret}") String clientSecret,
        @Value("${teams.client.tenant.id}") String tenantId,
        TeamsClientBuilder teamsClientBuilder
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
        this.teamsClientBuilder = teamsClientBuilder;
    }

    // ---------------------------------------------------------------------------------------------
    // Authorization on behalf of a tenant
    // ---------------------------------------------------------------------------------------------

    /**
     * Used to acquire the following permissions:
     *   - Application permissions for an entire tenant (can only be visited by an admin user)
     */
    @GetMapping("/authorize/admin")
    public RedirectView adminConsent() {
        var adminConsentUrl = getAdminConsentUrl(clientId);
        logger.info("Generated admin consent url: {}", adminConsentUrl);
        return new RedirectView(adminConsentUrl);
    }

    private String getAdminConsentUrl(String clientId) {
        var uri = new HttpUrl.Builder()
            .scheme("https")
            .host("login.microsoftonline.com")
            .addPathSegment("common")
            .addPathSegment("adminconsent")
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("redirect_uri", REDIRECT_URL)
            .addQueryParameter("state", "1234")
            .build();
        return uri.toString();
    }

    @GetMapping("/oauth/redirect")
    public String adminConsentCompleted(
        @RequestParam(value = "admin_consent") String adminConsent,
        @RequestParam(value = "tenantId") String tenantId,
        @RequestParam(value = "state") String state
    ) {
        // Request includes
        logger.info("GET /oauth/redirect");
        return String.format("admin_consent:%s, tenant:%s, state:%s", adminConsent, tenantId, state);
    }

    // ---------------------------------------------------------------------------------------------
    // Authorization of behalf of a user
    // ---------------------------------------------------------------------------------------------

    /**
     * Used to acquire the following permissions:
     *   - Delegated permissions on behalf of single user (when visited by a non-admin user)
     *   - Delegated permissions on behalf of all users in a tenant (when visited by an admin user)
     */
    @GetMapping("/authorize")
    public RedirectView authorize() {
        var authorizeUrl = getAuthorizeUrl(clientId, tenantId);
        logger.info("Generated authorize url: {}", authorizeUrl);
        return new RedirectView(authorizeUrl);
    }

    private String getAuthorizeUrl(String clientId, String tenantId) {
        var uri = new HttpUrl.Builder()
            .scheme("https")
            .host("login.microsoftonline.com")
            .addPathSegment(tenantId)
            .addPathSegment("oauth2/v2.0/authorize")
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("response_type", "code id_token")
            .addQueryParameter("redirect_uri", REDIRECT_URL)
            // We cannot use the "query" response mode when requesting an ID token. Instead, we must
            // use "fragment" or "form_post". Since browsers don't send fragment-encoded parameters
            // to the server, we can only use the "form_post" response mode.
            // https://docs.microsoft.com/en-gb/azure/active-directory/develop/v2-oauth2-auth-code-flow
            .addQueryParameter("response_mode", "form_post")
            .addQueryParameter("scope", "ChatMessage.Send Channel.ReadBasic.All Group.Read.All openid profile email")
            .addQueryParameter("state", "1234")
            .addQueryParameter("nonce", "1234")
            .build();
        return uri.toString();
    }

    /**
     * Where the user or admin is redirected to after granting the permissions. For both authorize
     * and admin consent endpoints.
     */
    @PostMapping("/oauth/redirect")
    public String authorizeCompleted(WebRequest authorizeReq) {
        logger.info("POST /oauth/redirect");

        var error = authorizeReq.getParameter("error");
        var errorDescription = authorizeReq.getParameter("errorDescription");
        if (error != null) {
            logger.error("failed to authorize with error code: {} and description: {}", error, errorDescription);
            return "Failed to authorize :(";
        }
        //  Response also has an "id_token" and "state" parameters
        var code = authorizeReq.getParameter("code");

        var client = teamsClientBuilder.fromAuthorizationCodeCredential(code);

        var message = new ChatMessage();
        var body = new ItemBody();
        body.content = "Test message sent through API";
        message.body = body;

        var teamId = "bfbc3c4f-4c31-4593-aec5-c4b959b0ffa4";
        var channelId = "19:2jc-5kmq0-tXqhU3UPRgIBgsSGh5Vk-I1sADf6fCDNM1@thread.tacv2";
        var res = client.teams(teamId).channels(channelId).messages().buildRequest().post(message);
        return res.chatId;
    }

    // ---------------------------------------------------------------------------------------------
    // Invoke specific API calls
    // ---------------------------------------------------------------------------------------------

    @GetMapping("/actions")
    public String listActions() {
        var res = new StringBuilder();
        res.append("<ul>");
        res.append("<li>").append("/actions/send-test-message").append("</li>");
        res.append("<li>").append("/actions/list-chats").append("</li>");
        res.append("<li>").append("/actions/list-channels/{teamId}").append("</li>");
        res.append("<li>").append("/actions/list-channel-messages/{teamId}/{channelId}").append("</li>");
        res.append("</ul>");
        return res.toString();
    }

    @GetMapping("/actions/send-test-message")
    public String sendMessage() {
        var client = teamsClientBuilder.fromClientSecretCredentials();
        var message = new ChatMessage();
        var body = new ItemBody();
        body.content = "Test message sent through API";
        message.body = body;
        var teamId = "bfbc3c4f-4c31-4593-aec5-c4b959b0ffa4";
        var channelId = "19:2jc-5kmq0-tXqhU3UPRgIBgsSGh5Vk-I1sADf6fCDNM1@thread.tacv2";
        var res = client.teams(teamId).channels(channelId).messages().buildRequest().post(message);
        return res.body.content;
    }

    @GetMapping("/actions/list-chats")
    public String listChats() {
        var client = teamsClientBuilder.fromClientSecretCredentials();
        var res = client.chats().buildRequest().get();
        for (var chat : res.getCurrentPage()) {
            logger.info("{}: {} {}", chat.id, chat.chatType.toString(), chat.topic);
        }
        return "Got chats!";
    }

    @GetMapping("/actions/list-channels/{teamId}")
    public String listChannels(
        @PathVariable(value = "teamId") @Nullable String teamId
    ) {
        // [19:2jc-5kmq0-tXqhU3UPRgIBgsSGh5Vk-I1sADf6fCDNM1@thread.tacv2]: General
        // [19:add4100499d347748de7f8fe6d3606a5@thread.tacv2]: sarah team
        if (teamId == null) {
            return "Usage: /actions/list-channels/{teamId}";
        }
        var client = teamsClientBuilder.fromClientSecretCredentials();
        var channels = client.teams(teamId).channels().buildRequest().get();

        var res = new StringBuilder();
        for (var channel : channels.getCurrentPage()) {
            res.append(String.format("[%s]: %s</br>", channel.id, channel.displayName));
        }
        return res.toString();
    }

    @GetMapping("actions/list-channel-messages/{teamId}/{channelId}")
    public String listChannelMessages(
        @PathVariable(value = "teamId") @Nullable String teamId,
        @PathVariable(value = "channelId") @Nullable String channelId
    ) {
        if (teamId == null || channelId == null) {
            return "Usage: /actions/list-channel-messages/{teamId}/{channelId}";
        }
        var client = teamsClientBuilder.fromClientSecretCredentials();
        var messages = client.teams(teamId).channels(channelId).messages().buildRequest().get();

        var res = new StringBuilder();
        for (var message : messages.getCurrentPage()) {
            res.append(String.format("[%s]: %s</br>", message.id, message.body));
        }
        return res.toString();
    }
}

//        var res = client.groups().buildRequest().get();
//        var groups = res.getCurrentPage();
//        var resp = new StringBuilder();
//        for (var group : groups) {
//            var name = group.displayName;
//            resp.append(name).append(" ").append(group.id).append("\n");
//        }

//        var channels = client.teams("bfbc3c4f-4c31-4593-aec5-c4b959b0ffa4").channels().buildRequest().get();
//        var resp = new StringBuilder();
//        for (var channel : channels.getCurrentPage()) {
//            logger.info("Channel: {} {} {}", channel.id, channel.displayName, channel.description);
//            resp.append(channel.id).append(" ").append(channel.displayName).append("\n");
//        }
//        return resp.toString();

// TEAMS
// All Company  - 44fdf39d-819f-4ac5-b753-38afbf937b6b
// Test Team    - bfbc3c4f-4c31-4593-aec5-c4b959b0ffa4
// Kool Kidz    - d3340e2a-c619-4aef-9d7e-809852df7a48

// CHANNELS IN TEST TEAM
// General (Test Team) – 19:2jc-5kmq0-tXqhU3UPRgIBgsSGh5Vk-I1sADf6fCDNM1@thread.tacv2
// sarah team (null) – 19:add4100499d347748de7f8fe6d3606a5@thread.tacv2


//        graphClient.teams("fbe2bf47-16c8-47cf-b4a5-4b9b187c508b").channels("19:4a95f7d8db4c4e7fae857bcebe0623e6@thread.tacv2").messages()
//                .buildRequest()
//                .post(chatMessage);
