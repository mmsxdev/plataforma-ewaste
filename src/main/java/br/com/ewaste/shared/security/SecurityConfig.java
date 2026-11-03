package br.com.ewaste.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança por prefixo de rota. Dono: Dev 1 (Miguel).
 *
 * <p>Cada módulo usa o prefixo do seu perfil (/geradora/**, /coletora/**, /admin/**), então ninguém
 * além do Dev 1 precisa alterar esta classe.
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/login",
                        "/cadastro/**",
                        "/mapa",
                        "/api/public/**",
                        "/verificar/**",
                        "/actuator/health",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/error")
                    .permitAll()
                    .requestMatchers("/geradora/**")
                    .hasRole("GERADORA")
                    .requestMatchers("/coletora/**")
                    .hasRole("COLETORA")
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            login -> login.loginPage("/login").defaultSuccessUrl("/painel", true).permitAll())
        .logout(logout -> logout.logoutSuccessUrl("/"));
    return http.build();
  }

  /** Senhas sempre com BCrypt (coluna usuario.senha_hash). */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
