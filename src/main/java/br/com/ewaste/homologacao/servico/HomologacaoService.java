package br.com.ewaste.homologacao.servico;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.armazenamento.api.ArquivoRef;
import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.dominio.DocumentoHomologacao;
import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao;
import br.com.ewaste.homologacao.repositorio.HomologacaoRepository;
import br.com.ewaste.shared.events.ColetoraHomologadaEvent;
import br.com.ewaste.shared.events.ColetoraReprovadaEvent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fluxo de homologação (Dev 2 — Felipe): a coletora envia os documentos e pede análise; o admin
 * aprova ou reprova. A aprovação publica {@link ColetoraHomologadaEvent}, que faz o roteamento
 * alertar a coletora sobre solicitações já abertas na região dela.
 */
@Service
@Transactional
public class HomologacaoService {

  private final HomologacaoRepository homologacoes;
  private final Armazenamento armazenamento;
  private final ApplicationEventPublisher eventos;

  HomologacaoService(
      HomologacaoRepository homologacoes,
      Armazenamento armazenamento,
      ApplicationEventPublisher eventos) {
    this.homologacoes = homologacoes;
    this.armazenamento = armazenamento;
    this.eventos = eventos;
  }

  /** Homologação da coletora; cria uma PENDENTE se a coletora ainda não tiver (dados antigos). */
  public Homologacao daColetora(Long coletoraId) {
    return homologacoes
        .findComDocumentosByEmpresaId(coletoraId)
        .orElseGet(() -> homologacoes.save(Homologacao.pendente(coletoraId)));
  }

  /** Documento só pode ser enviado enquanto a homologação aguarda a coletora (ou foi reprovada). */
  public void enviarDocumento(
      Long coletoraId, TipoDocumentoHomologacao tipo, LocalDate validade, MultipartFile arquivo) {
    if (tipo == null) {
      throw new IllegalArgumentException("Escolha o tipo de documento.");
    }
    if (validade != null && validade.isBefore(hoje())) {
      throw new IllegalArgumentException("A validade informada já passou.");
    }
    Homologacao h = daColetora(coletoraId);
    if (h.getStatus() != StatusHomologacao.PENDENTE
        && h.getStatus() != StatusHomologacao.REPROVADA) {
      throw new IllegalArgumentException(
          "Não é possível enviar documentos com a homologação " + rotulo(h.getStatus()) + ".");
    }
    ArquivoRef ref = armazenamento.salvar(arquivo, "homologacao");
    h.adicionarDocumento(tipo, ref.caminho(), ref.nomeOriginal(), validade);
  }

  /** Coletora pede a análise; exige os documentos obrigatórios em dia. */
  public void enviarParaAnalise(Long coletoraId) {
    Homologacao h = daColetora(coletoraId);
    List<TipoDocumentoHomologacao> faltando = obrigatoriosFaltando(h);
    if (!faltando.isEmpty()) {
      throw new IllegalArgumentException(
          "Envie antes: "
              + String.join(", ", faltando.stream().map(HomologacaoService::rotulo).toList())
              + ".");
    }
    try {
      h.enviarParaAnalise();
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException("A homologação já está " + rotulo(h.getStatus()) + ".", e);
    }
  }

  @Transactional(readOnly = true)
  public List<Homologacao> todas() {
    return homologacoes.findTodasComDocumentos();
  }

  public void aprovar(Long homologacaoId, Long adminId) {
    Homologacao h = buscar(homologacaoId);
    if (h.getStatus() != StatusHomologacao.EM_ANALISE) {
      throw new IllegalArgumentException("Só é possível aprovar homologações em análise.");
    }
    List<TipoDocumentoHomologacao> faltando = obrigatoriosFaltando(h);
    if (!faltando.isEmpty()) {
      throw new IllegalArgumentException(
          "Documentos obrigatórios ausentes ou vencidos: "
              + String.join(", ", faltando.stream().map(HomologacaoService::rotulo).toList())
              + ".");
    }
    h.aprovar(adminId);
    eventos.publishEvent(new ColetoraHomologadaEvent(h.getEmpresaId()));
  }

  public void reprovar(Long homologacaoId, Long adminId, String motivo) {
    Homologacao h = buscar(homologacaoId);
    if (h.getStatus() != StatusHomologacao.EM_ANALISE) {
      throw new IllegalArgumentException("Só é possível reprovar homologações em análise.");
    }
    h.reprovar(adminId, motivo);
    eventos.publishEvent(new ColetoraReprovadaEvent(h.getEmpresaId(), h.getMotivo()));
  }

  /** Arquivo de um documento da própria coletora. */
  @Transactional(readOnly = true)
  public Resource documentoDaColetora(Long coletoraId, Long documentoId) {
    return carregar(daColetora(coletoraId), documentoId);
  }

  /** Arquivo de um documento para o admin. */
  @Transactional(readOnly = true)
  public Resource documentoParaAdmin(Long homologacaoId, Long documentoId) {
    return carregar(buscar(homologacaoId), documentoId);
  }

  public static boolean isApta(Homologacao h) {
    return h.isApta(hoje());
  }

  public static List<TipoDocumentoHomologacao> obrigatoriosFaltando(Homologacao h) {
    LocalDate hoje = hoje();
    return Arrays.stream(TipoDocumentoHomologacao.values())
        .filter(TipoDocumentoHomologacao::isObrigatorio)
        .filter(t -> h.documentoMaisRecente(t).filter(d -> !d.isVencido(hoje)).isEmpty())
        .toList();
  }

  public static String rotulo(TipoDocumentoHomologacao tipo) {
    return switch (tipo) {
      case LICENCA_AMBIENTAL -> "Licença ambiental";
      case CADASTRO_IBAMA -> "Cadastro IBAMA";
      case MTR -> "MTR";
      case ISO_27001 -> "ISO 27001";
      case TISAX -> "TISAX";
      case LAUDO_IEC_21964 -> "Laudo IEC 21964";
      case OUTRO -> "Outro";
    };
  }

  public static String rotulo(StatusHomologacao status) {
    return switch (status) {
      case PENDENTE -> "pendente";
      case EM_ANALISE -> "em análise";
      case APROVADA -> "aprovada";
      case REPROVADA -> "reprovada";
      case SUSPENSA -> "suspensa";
    };
  }

  private Homologacao buscar(Long id) {
    return homologacoes
        .findComDocumentosById(id)
        .orElseThrow(() -> new IllegalArgumentException("Homologação não encontrada."));
  }

  private Resource carregar(Homologacao h, Long documentoId) {
    DocumentoHomologacao doc =
        h.getDocumentos().stream()
            .filter(d -> d.getId().equals(documentoId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado."));
    return armazenamento.carregar(doc.getArquivoRef());
  }

  private static LocalDate hoje() {
    return LocalDate.now(ColetorasAptasService.FUSO);
  }
}
