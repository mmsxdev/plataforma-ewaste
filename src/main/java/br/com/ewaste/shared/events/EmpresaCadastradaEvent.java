package br.com.ewaste.shared.events;

import br.com.ewaste.usuario.api.TipoEmpresa;

/**
 * Uma empresa terminou o cadastro.
 *
 * <p>Publicado por: Dev 1 (cadastro). Ouvido por: Dev 2 (cria homologação PENDENTE para coletoras).
 */
public record EmpresaCadastradaEvent(Long empresaId, TipoEmpresa tipo) {}
