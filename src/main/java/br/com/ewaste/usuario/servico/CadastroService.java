package br.com.ewaste.usuario.servico;

import br.com.ewaste.shared.events.EmpresaCadastradaEvent;
import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.dominio.*;
import br.com.ewaste.usuario.repositorio.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CadastroService {
  private final EmpresaRepository empresas;
  private final UsuarioRepository usuarios;
  private final PasswordEncoder encoder;
  private final ApplicationEventPublisher eventos;

  public CadastroService(
      EmpresaRepository empresas,
      UsuarioRepository usuarios,
      PasswordEncoder encoder,
      ApplicationEventPublisher eventos) {
    this.empresas = empresas;
    this.usuarios = usuarios;
    this.encoder = encoder;
    this.eventos = eventos;
  }

  @Transactional
  public Long cadastrar(@NotNull TipoEmpresa tipo, @NotNull @Valid CadastroDados dados) {
    if (tipo == TipoEmpresa.COLETORA
        && (dados.raioAtendimentoKm() == null || dados.raioAtendimentoKm() < 1)) {
      throw new IllegalArgumentException("Informe um raio de atendimento maior que zero.");
    }
    String cnpj = Cnpj.normalizar(dados.cnpj());
    String email = dados.email().strip().toLowerCase(Locale.ROOT);
    if (empresas.existsByCnpj(cnpj))
      throw new CadastroDuplicadoException("cnpj", "Este CNPJ já está cadastrado.");
    if (usuarios.existsByEmailIgnoreCase(email))
      throw new CadastroDuplicadoException("email", "Este e-mail já está cadastrado.");
    Empresa empresa =
        empresas.saveAndFlush(
            new Empresa(
                tipo,
                dados.razaoSocial().strip(),
                cnpj,
                dados.telefone().strip(),
                dados.endereco().strip(),
                dados.cidade().strip(),
                dados.uf(),
                dados.latitude(),
                dados.longitude(),
                dados.raioAtendimentoKm()));
    usuarios.saveAndFlush(
        new Usuario(
            empresa.getId(),
            dados.nome().strip(),
            email,
            encoder.encode(dados.senha()),
            PerfilUsuario.valueOf(tipo.name())));
    eventos.publishEvent(new EmpresaCadastradaEvent(empresa.getId(), tipo));
    return empresa.getId();
  }
}
