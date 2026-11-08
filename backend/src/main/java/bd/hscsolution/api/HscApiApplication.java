package bd.hscsolution.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "bd.hscsolution")
public class HscApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(HscApiApplication.class, args);
    }
}
