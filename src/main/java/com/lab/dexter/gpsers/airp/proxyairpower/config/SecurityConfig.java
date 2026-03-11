package com.lab.dexter.gpsers.airp.proxyairpower.config;

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita CSRF para permitir que o Android faça POST/DELETE na API sem bloqueios
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Libera TOTALMENTE as rotas da API para os aplicativos Mobile
                        .requestMatchers("/api/**").permitAll()
                        // 2. Libera a página de login e os arquivos de design (css/js)
                        .requestMatchers("/login.html", "/css/**", "/js/**").permitAll()
                        // 3. Bloqueia qualquer outra coisa (ex: index.html do painel) exigindo login
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html") // Diz ao Spring para usar a nossa página customizada
                        .loginProcessingUrl("/perform_login") // URL invisível que o Spring usa para validar a senha
                        .defaultSuccessUrl("/index.html", true) // Para onde ir ao acertar a senha
                        .failureUrl("/login.html?error=true") // Para onde ir ao errar a senha
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/perform_logout")
                        .logoutSuccessUrl("/login.html?logout=true")
                        .permitAll()
                );

        return http.build();
    }

    // Cria o utilizador padrão (Super Admin) em memória
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123")) // <-- SENHA PADRÃO: admin123
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    // Motor de Criptografia BCrypt (Vamos usar muito isto no Passo 2 também!)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}