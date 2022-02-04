package com.demo.teams;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * This is configured as a singleton to act like an in-memory database for local testing.
 * Currently unused.
 */
public class TeamsRepository {
    private Map<String, AccessToken> db;

    public TeamsRepository() {
        db = new HashMap<>();
    }

    public void setAccessToken(String key, AccessToken token) {
        db.put(key, token);
    }

    public AccessToken getAccessToken(String key) {
        return db.get(key);
    }

    public static class AccessToken {
        @JsonProperty("access_token")
        public String accessToken;
        @JsonProperty("token_type")
        public String tokenType;
        @JsonProperty("expires_in")
        public int expiresIn;
        @JsonProperty("ext_expires_in")
        public int extExpiresIn;
        @JsonProperty("scope")
        public String scope;
        @JsonProperty("refresh_token")
        public String refreshToken;
        @JsonProperty("id_token")
        public String idToken;
    }
}
