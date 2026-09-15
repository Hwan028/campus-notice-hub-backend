package com.kkamar.cnh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CampusNoticeHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusNoticeHubApplication.class, args);
    }

}
