package com.lab.dexter.gpsers.airp.proxyairpower.repositories;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.WifiNetwork;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WifiNetworkRepository extends JpaRepository<WifiNetwork, Long> {
}
