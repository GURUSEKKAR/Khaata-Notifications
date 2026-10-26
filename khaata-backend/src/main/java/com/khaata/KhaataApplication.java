package com.khaata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Khaata backend.
 *
 * <p>Khaata is a digital udhaar-book for local shopkeepers. It is built as a
 * classic layered Spring Boot service around an append-only transaction ledger.
 *
 * <p>{@code @EnableScheduling} turns on the background cron used by the reminder
 * engine — the piece that "chases" customers automatically without the owner
 * having to remember.
 */
@SpringBootApplication
@EnableScheduling
public class KhaataApplication {

    public static void main(String[] args) {
        SpringApplication.run(KhaataApplication.class, args);
    }
}
