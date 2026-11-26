package br.com.ewaste.homologacao.web;

import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.dominio.DocumentoHomologacao;
import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao;
import br.com.ewaste.homologacao.servico.HomologacaoService;
import br.com.ewaste.usuario.api.EmpresaResumo;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Dados prontos para as telas de homologação (sem expor a entidade JPA ao template). */
public record HomologacaoView(
    Long id,
    Long empresaId,
    String empresa,
    String local,
    StatusHomologacao status,
    String statusRotulo,
    String motivo,
    String analisadoEm,
    boolean apta,
    List<String> faltando,
    List<Documento> documentos) {

  public record Documento(Long id, String tipo, String nome, String validade, String enviadoEm) {}

  public record TipoOpcao(TipoDocumentoHomologacao valor, String rotulo, boolean obrigatorio) {}

  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATA_HORA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo"));

  public boolean podeEnviarDocumentos() {
    return status == StatusHomologacao.PENDENTE || status == StatusHomologacao.REPROVADA;
  }

  static HomologacaoView de(Homologacao h, EmpresaResumo empresa) {
    return new HomologacaoView(
        h.getId(),
        h.getEmpresaId(),
        empresa == null ? "Coletora nº " + h.getEmpresaId() : empresa.razaoSocial(),
        empresa == null ? "" : empresa.cidade() + "/" + empresa.uf(),
        h.getStatus(),
        HomologacaoService.rotulo(h.getStatus()),
        h.getMotivo(),
        h.getAnalisadoEm() == null ? null : DATA_HORA.format(h.getAnalisadoEm()),
        HomologacaoService.isApta(h),
        HomologacaoService.obrigatoriosFaltando(h).stream()
            .map(HomologacaoService::rotulo)
            .toList(),
        h.getDocumentos().stream()
            .sorted(Comparator.comparing(DocumentoHomologacao::getEnviadoEm).reversed())
            .map(
                d ->
                    new Documento(
                        d.getId(),
                        HomologacaoService.rotulo(d.getTipo()),
                        d.getNomeOriginal(),
                        d.getValidade() == null ? "Sem validade" : DATA.format(d.getValidade()),
                        DATA_HORA.format(d.getEnviadoEm())))
            .toList());
  }
}
