package com.canteen;

import com.canteen.dao.UserDAO;
import com.canteen.db.DBConnection;
import com.canteen.service.TokenQueue;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebApp {

    public static void main(String[] args) {
        SpringApplication.run(WebApp.class, args);
    }

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            DBConnection.ensureSchemaCompatibility();
            UserDAO.ensureAdmin();
            TokenQueue.get().reload();
        };
    }
}
