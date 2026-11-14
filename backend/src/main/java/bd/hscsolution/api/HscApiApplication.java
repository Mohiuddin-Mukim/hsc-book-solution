package bd.hscsolution.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "bd.hscsolution")
@ComponentScan(basePackages = "bd.hscsolution")
@EnableJpaRepositories(basePackages = {
        "bd.hscsolution.catalog.repository", // 👈 আপনার নতুন ক্যাটালগ রিপোজিটরি স্ক্যান করবে
        "bd.hscsolution.media",
        "bd.hscsolution.auth",               // (যদি থাকে) আপনার অথ রিপোজিটরি স্ক্যান করবে
        "bd.hscsolution.security"            // (যদি থাকে) আপনার সিকিউরিটি রিপোজিটরি স্ক্যান করবে
})
@EntityScan(basePackages = "bd.hscsolution")
public class HscApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(HscApiApplication.class, args);
    }
}
