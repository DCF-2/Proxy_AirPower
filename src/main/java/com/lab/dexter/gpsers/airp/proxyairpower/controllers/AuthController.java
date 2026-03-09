package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.dtos.AuthRequestDTO;
import com.lab.dexter.gpsers.airp.proxyairpower.dtos.AuthResponseDTO;
import com.lab.dexter.gpsers.airp.proxyairpower.services.AuthProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthProxyService authService;

    public AuthController(AuthProxyService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {
        AuthResponseDTO response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }
}
