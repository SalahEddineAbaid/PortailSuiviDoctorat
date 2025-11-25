package ma.emsi.defenseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DefenseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DefenseServiceApplication.class, args);
    }

}
