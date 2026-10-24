package br.com.ewaste;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import java.util.Arrays;

/**
 * Regra de ouro dos módulos: um módulo só usa de outro módulo o que está em {@code <modulo>.api}.
 * Se este teste falhar, use (ou peça ao dono para criar) uma interface no pacote api do outro
 * módulo.
 */
@AnalyzeClasses(packages = "br.com.ewaste", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

  static final String RAIZ = "br.com.ewaste.";

  static final String[] MODULOS = {
    "usuario",
    "homologacao",
    "armazenamento",
    "solicitacao",
    "roteamento",
    "rastreio",
    "documento",
    "notificacao",
    "mapa",
    "monetizacao"
  };

  @ArchTest
  static final ArchRule modulosSoSeFalamPelaApi =
      CompositeArchRule.of(Arrays.stream(MODULOS).map(ArquiteturaTest::regraDoModulo).toList())
          .as("módulos só acessam outros módulos pelo pacote api");

  static ArchRule regraDoModulo(String modulo) {
    String pacote = RAIZ + modulo + "..";
    String api = RAIZ + modulo + ".api..";
    return noClasses()
        .that()
        .resideOutsideOfPackage(pacote)
        .should()
        .dependOnClassesThat(
            JavaClass.Predicates.resideInAPackage(pacote)
                .and(DescribedPredicate.not(JavaClass.Predicates.resideInAPackage(api))))
        .as("de fora de '" + modulo + "' só se acessa '" + modulo + ".api'")
        .allowEmptyShould(true);
  }
}
