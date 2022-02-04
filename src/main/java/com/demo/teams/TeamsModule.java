package com.demo.teams;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;

@Configuration
public class TeamsModule {
    @Bean
    GraphServiceClient<Request> graphClient(
        @Value("${teams.client.client.id}") String clientId,
        @Value("${teams.client.client.secret}") String clientSecret,
        @Value("${teams.client.tenant.id}") String tenantId,
        @Value("${teams.client.scopes}") String rawScopes
    ) {
        //var scopes = Arrays.asList(rawScopes.split(" "));
        var scopes = List.of("https://graph.microsoft.com/User.Read/.default");
        var tokenCredential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .build();
        var authProvider = new TokenCredentialAuthProvider(scopes, tokenCredential);
        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    TeamsRepository teamsRepository() {
        return new TeamsRepository();
    }

    @Bean
    TeamsClientBuilder teamsClientBuilder(
        @Value("${teams.client.client.id}") String clientId,
        @Value("${teams.client.client.secret}") String clientSecret,
        @Value("${teams.client.tenant.id}") String tenantId
    ) {
        return new TeamsClientBuilder(clientId, clientSecret, tenantId);
    }
}
