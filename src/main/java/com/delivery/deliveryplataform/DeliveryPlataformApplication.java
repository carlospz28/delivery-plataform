package com.delivery.deliveryplataform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DeliveryPlataformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryPlataformApplication.class, args);
    }

}
