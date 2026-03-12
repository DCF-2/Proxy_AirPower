package com.lab.dexter.gpsers.airp.proxyairpower.config;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppClient;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppClientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initApps(AppClientRepository repository) {
        return args -> {
            // Cria os 3 aplicativos automaticamente se eles não existirem
            String[] apps = {"Airpower_Costumer", "Airpower_Admin", "Airpower_webapp"};

            for (String appName : apps) {
                if (repository.findByName(appName).isEmpty()) {
                    AppClient app = new AppClient();
                    app.setName(appName);
                    app.setAppKey(appName.toUpperCase() + "_KEY"); // Ex: AIRPOWER_ADMIN_KEY
                    repository.save(app);
                }
            }
        };
    }
}