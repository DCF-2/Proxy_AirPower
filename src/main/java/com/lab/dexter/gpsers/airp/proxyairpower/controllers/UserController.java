package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppClient;
import com.lab.dexter.gpsers.airp.proxyairpower.entities.AppUser;
import com.lab.dexter.gpsers.airp.proxyairpower.entities.UserStatus;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppClientRepository;
import com.lab.dexter.gpsers.airp.proxyairpower.repositories.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AppClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // O nosso BCrypt

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String name = payload.get("name");
        String appName = payload.get("appName"); // O app deve dizer quem ele é

        // 1. Verifica se o email já existe
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: Email já registado.");
        }

        // 2. Verifica se o Aplicativo é válido
        Optional<AppClient> clientOpt = clientRepository.findByName(appName);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Aplicativo inválido ou não reconhecido.");
        }

        AppClient client = clientOpt.get();
        AppUser newUser = new AppUser();
        newUser.setName(name);
        newUser.setEmail(email);

        // Criptografa a senha do utilizador para NINGUÉM conseguir ler
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAppClient(client);

        // 3. A REGRA DE OURO DA APROVAÇÃO
        if (client.getName().equals("Airpower_Admin")) {
            newUser.setStatus(UserStatus.PENDING); // Admin Mobile exige aprovação
        } else {
            newUser.setStatus(UserStatus.APPROVED); // Costumer e WebApp entram direto
        }

        userRepository.save(newUser);

        return ResponseEntity.ok("Utilizador registado com sucesso. Status: " + newUser.getStatus());
    }
}