package br.com.ewaste.rastreio.api;

import java.util.List;

public interface RastreioConsulta {
  List<EventoResumo> listar(Long solicitacaoId);
}
