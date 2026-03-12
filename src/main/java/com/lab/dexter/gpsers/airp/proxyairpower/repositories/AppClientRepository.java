package com.lab.dexter.gpsers.airp.proxyairpower.repositories;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppClientRepository extends JpaRepository<AppClient, Long> {
    Optional<AppClient> findByName(String name);
}