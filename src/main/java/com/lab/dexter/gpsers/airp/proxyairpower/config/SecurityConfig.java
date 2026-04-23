package com.lab.dexter.gpsers.airp.proxyairpower.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Recebendo os valores injetados com segurança
    @Value("${airpower.admin.username}")
    private String adminUsername;

    @Value("${airpower.admin.password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/ws/telemetry", "/ws/telemetry/**").permitAll()
                        .requestMatchers("/login.html", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/index.html", true)
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/perform_logout")
                        .logoutSuccessUrl("/login.html?logout=true")
                        .permitAll()
                );

        return http.build();
    }

    // Agora o utilizador padrão vem das Variáveis de Ambiente
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {

        // Proteção contra inicialização com senhas nulas
        if (adminUsername == null || adminPassword == null || adminUsername.isEmpty() || adminPassword.isEmpty()) {
            throw new IllegalStateException("CRÍTICO: Variáveis do Super Admin (ADMIN_PANEL_USER e ADMIN_PANEL_PASS) não configuradas!");
        }

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}