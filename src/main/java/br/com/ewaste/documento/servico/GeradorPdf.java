package br.com.ewaste.documento.servico;

import br.com.ewaste.documento.api.DadosEmissao;
import br.com.ewaste.documento.api.TipoDocumento;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
class GeradorPdf {
  byte[] gerar(DadosEmissao dados, TipoDocumento tipo, String numero) {
    ByteArrayOutputStream saida = new ByteArrayOutputStream();
    Document pdf = new Document(PageSize.A4, 48, 48, 48, 48);
    try {
      PdfWriter escritor = PdfWriter.getInstance(pdf, saida);
      pdf.open();
      marcaDagua(escritor);
      logo(pdf);
      titulo(pdf, titulo(tipo));
      linha(pdf, "Número: " + numero);
      linha(pdf, "Solicitação: " + dados.solicitacaoId());
      linha(pdf, "Peso real coletado: " + dados.pesoRealKg() + " kg");
      linha(pdf, "Destinação final: " + dados.destinacao());
      linha(pdf, modelo(tipo));
      if (tipo == TipoDocumento.CERTIFICADO_DESTRUICAO) {
        for (DadosEmissao.Midia midia : dados.midias()) {
          linha(
              pdf,
              midia.numeroSerie()
                  + " — "
                  + midia.tipo()
                  + " — "
                  + midia.metodo()
                  + " — nível "
                  + midia.nivel());
        }
        linha(pdf, "Referências: ISO 27001, TISAX, LGPD e IEC 21964.");
      }
      linha(pdf, "Documento simulado para fins acadêmicos. Sem validade legal.");
      pdf.close();
      return saida.toByteArray();
    } catch (DocumentException | IOException e) {
      throw new IllegalStateException("Não foi possível gerar o PDF.", e);
    } finally {
      if (pdf.isOpen()) {
        pdf.close();
      }
    }
  }

  private static String titulo(TipoDocumento tipo) {
    return switch (tipo) {
      case CDF -> "Certificado de Destinação Final";
      case LAUDO_TECNICO -> "Laudo Técnico";
      case CERTIFICADO_DESTRUICAO -> "Certificado de Destruição de Mídias";
    };
  }

  private static void titulo(Document pdf, String texto) throws DocumentException {
    pdf.add(new Paragraph(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
    pdf.add(new Paragraph(" "));
  }

  private static void logo(Document pdf) throws IOException, DocumentException {
    Image imagem = Image.getInstance(GeradorPdf.class.getResource("/pdf/logo.png"));
    imagem.scaleToFit(150, 42);
    pdf.add(imagem);
    pdf.add(new Paragraph(" "));
  }

  private static String modelo(TipoDocumento tipo) throws IOException {
    String arquivo = "/pdf/" + tipo.name().toLowerCase() + ".txt";
    try (InputStream entrada = GeradorPdf.class.getResourceAsStream(arquivo)) {
      if (entrada == null) {
        throw new IllegalStateException("Modelo de PDF não encontrado: " + arquivo);
      }
      return new String(entrada.readAllBytes(), StandardCharsets.UTF_8).strip();
    }
  }

  private static void linha(Document pdf, String texto) throws DocumentException {
    pdf.add(new Paragraph(texto, FontFactory.getFont(FontFactory.HELVETICA, 11)));
    pdf.add(new Paragraph(" "));
  }

  private static void marcaDagua(PdfWriter escritor) throws IOException, DocumentException {
    PdfContentByte fundo = escritor.getDirectContentUnder();
    PdfGState transparencia = new PdfGState();
    transparencia.setFillOpacity(0.15f);
    fundo.saveState();
    fundo.setGState(transparencia);
    fundo.beginText();
    fundo.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false), 42);
    fundo.showTextAligned(Element.ALIGN_CENTER, "DOCUMENTO SIMULADO", 297, 420, 45);
    fundo.endText();
    fundo.restoreState();
  }
}
