package br.com.ewaste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EwasteApplication {

  public static void main(String[] args) {
    SpringApplication.run(EwasteApplication.class, args);
  }
}
