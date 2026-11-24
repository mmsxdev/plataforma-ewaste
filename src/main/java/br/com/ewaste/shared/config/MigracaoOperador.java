package br.com.ewaste.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/** O runner só é executado depois do Flyway e da validação JPA. */
@Configuration
@Profile("migrate")
public class MigracaoOperador {
  @Bean
  @Order(100)
  ApplicationRunner encerrarAposMigracao(
      ConfigurableApplicationContext context,
      JdbcTemplate jdbc,
      @Value("${DB_APP_USER:ewaste_app}") String appUser) {
    return args -> {
      if (!appUser.matches("[a-zA-Z_][a-zA-Z0-9_]{0,62}")) {
        throw new IllegalArgumentException("DB_APP_USER inválido.");
      }
      jdbc.execute("REVOKE ALL ON TABLE flyway_schema_history FROM \"" + appUser + "\"");
      SpringApplication.exit(context);
    };
  }
}
