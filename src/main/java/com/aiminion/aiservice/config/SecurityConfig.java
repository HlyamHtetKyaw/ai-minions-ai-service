package com.aiminion.aiservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public WorkerTokenFilter workerTokenFilter(InternalWorkerProperties internalWorkerProperties) {
		return new WorkerTokenFilter(internalWorkerProperties);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, WorkerTokenFilter workerTokenFilter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(workerTokenFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.build();
	}
}
