package com.example.hotelreservationsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * Implementation of AuditorAware to provide the current auditor.
     * For now, returns "SYSTEM" as the default user.
     * TODO: Integrate with Spring Security to return authenticated user's username
     */
    static class AuditorAwareImpl implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            // TODO: Replace with actual authentication logic

            return Optional.of("SYSTEM");
        }
    }
}
