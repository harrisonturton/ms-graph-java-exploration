package com.demo.teams;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class TeamsModule {
    @Bean
    TeamsClientBuilder teamsClientBuilder(
        @Value("${teams.client.client.id}") String clientId,
        @Value("${teams.client.client.secret}") String clientSecret,
        @Value("${teams.client.tenant.id}") String tenantId
    ) {
        return new TeamsClientBuilder(clientId, clientSecret, tenantId);
    }

    @Bean
    // Configure as singleton to mimic an injected database handle
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    TeamsRepository teamsRepository() {
        return new TeamsRepository();
    }
}
