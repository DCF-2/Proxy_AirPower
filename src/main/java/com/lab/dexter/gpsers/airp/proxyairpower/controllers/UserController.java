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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // Permite que o Painel Web aceda
public class UserController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AppClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- 1. REGISTAR (O que já tínhamos) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String name = payload.get("name");
        String appName = payload.get("appName");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: Email já registado.");
        }

        Optional<AppClient> clientOpt = clientRepository.findByName(appName);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Aplicativo inválido.");
        }

        AppClient client = clientOpt.get();
        AppUser newUser = new AppUser();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAppClient(client);

        if (client.getName().equals("Airpower_Admin")) {
            newUser.setStatus(UserStatus.PENDING);
        } else {
            newUser.setStatus(UserStatus.APPROVED);
        }

        userRepository.save(newUser);
        return ResponseEntity.ok("Utilizador registado com sucesso. Status: " + newUser.getStatus());
    }

    // --- OS MÉTODOS NOVOS (Para fechar a checklist) ---

    // 2. LISTAR TODOS OS USUÁRIOS (Para o Painel Web)
    @GetMapping
    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    // 3. APROVAR UM USUÁRIO E DEFINIR VALIDADE
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        user.setStatus(UserStatus.APPROVED);

        // Verifica se o Admin enviou uma data de validade (formato ISO: 2026-12-31T23:59:59)
        if (payload.containsKey("expirationDate") && payload.get("expirationDate") != null) {
            LocalDateTime expDate = LocalDateTime.parse(payload.get("expirationDate"), DateTimeFormatter.ISO_DATE_TIME);
            user.setExpirationDate(expDate);
        } else {
            user.setExpirationDate(null); // Sem data limite (acesso vitalício)
        }

        userRepository.save(user);
        return ResponseEntity.ok("Usuário aprovado com sucesso!");
    }

    // 4. MUDAR STATUS (Banir, Rejeitar, etc.)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeUserStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        String newStatus = payload.get("status"); // PENDING, APPROVED, REJECTED, BANNED

        try {
            user.setStatus(UserStatus.valueOf(newStatus.toUpperCase()));
            userRepository.save(user);
            return ResponseEntity.ok("Status alterado para " + user.getStatus());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Status inválido.");
        }
    }
}