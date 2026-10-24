package br.com.ewaste.usuario.servico;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Fallback;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

/**
 * Login PROVISÓRIO (PR 0): autentica pela tabela usuario (e-mail + senha BCrypt). Com o seed do
 * perfil dev, já dá para entrar como geradora, coletora ou admin (senha "senha123"). O Dev 1
 * substitui por um UserDetailsService próprio; aí esta classe deve ser apagada.
 */
@Configuration
class LoginProvisorioConfig {

  @Bean
  @Fallback
  UserDetailsService usuariosProvisorios(DataSource dataSource) {
    JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
    manager.setUsersByUsernameQuery("select email, senha_hash, ativo from usuario where email = ?");
    manager.setAuthoritiesByUsernameQuery(
        "select email, 'ROLE_' || perfil from usuario where email = ?");
    return manager;
  }
}
