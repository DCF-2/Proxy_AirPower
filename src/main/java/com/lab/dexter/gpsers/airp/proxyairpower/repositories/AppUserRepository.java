package com.lab.dexter.gpsers.airp.proxyairpower.repositories;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
}