package br.com.ewaste;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EwasteApplicationIT {

  @Autowired MockMvc mvc;

  @Test
  void healthRespondeUp() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void paginaInicialEhPublica() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk());
  }

  @Test
  void areaRestritaExigeLogin() throws Exception {
    mvc.perform(get("/admin/qualquer-coisa")).andExpect(status().is3xxRedirection());
  }
}
