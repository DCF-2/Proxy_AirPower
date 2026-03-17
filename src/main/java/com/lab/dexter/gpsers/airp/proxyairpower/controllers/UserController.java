package com.lab.dexter.gpsers.airp.proxyairpower.controllers;

import com.lab.dexter.gpsers.airp.proxyairpower.config.CryptoUtil;
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
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AppClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- 1. REGISTAR ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String name = payload.get("name");
        String appName = payload.get("appName");

        // 1. Verifica se o email já existe
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: Email já registado.");
        }

        // 2. Verifica se o Aplicativo é válido
        Optional<AppClient> clientOpt = clientRepository.findByName(appName);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: Aplicativo inválido.");
        }

        AppClient client = clientOpt.get();
        AppUser newUser = new AppUser();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password)); // BCrypt para a senha da nossa API
        newUser.setAppClient(client);

        // 3. A REGRA DE OURO DA APROVAÇÃO
        if (client.getName().equals("Airpower_Admin")) {
            newUser.setStatus(UserStatus.PENDING);
        } else {
            newUser.setStatus(UserStatus.APPROVED);
        }

        // 4. DADOS DINÂMICOS DO THINGSBOARD (Se o utilizador os enviar no registo)
        if (payload.containsKey("tbUrl")) {
            newUser.setTbUrl(payload.get("tbUrl"));
        }
        if (payload.containsKey("tbUsername")) {
            newUser.setTbUsername(payload.get("tbUsername"));
        }
        if (payload.containsKey("tbPassword")) {
            // Tranca a senha do ThingsBoard com AES de dupla via!
            newUser.setTbPassword(CryptoUtil.encrypt(payload.get("tbPassword")));
        }

        userRepository.save(newUser);
        return ResponseEntity.ok("Utilizador registado com sucesso. Status: " + newUser.getStatus());
    }

    // --- 2. LISTAR TODOS OS USUÁRIOS ---
    @GetMapping
    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    // --- 3. APROVAR UM USUÁRIO E DEFINIR VALIDADE ---
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuário não encontrado."));
        }

        AppUser user = userOpt.get();
        user.setStatus(UserStatus.APPROVED);

        try {
            // Tratamento seguro de Data
            if (payload.containsKey("expirationDate")) {
                String expDateStr = payload.get("expirationDate");
                if (expDateStr != null && !expDateStr.trim().isEmpty()) {
                    LocalDateTime expDate = LocalDateTime.parse(expDateStr, DateTimeFormatter.ISO_DATE_TIME);
                    user.setExpirationDate(expDate);
                } else {
                    user.setExpirationDate(null);
                }
            }

            // Tratamento seguro de Strings
            if (payload.containsKey("tbUrl") && payload.get("tbUrl") != null) {
                user.setTbUrl(payload.get("tbUrl"));
            }
            if (payload.containsKey("tbUsername") && payload.get("tbUsername") != null) {
                user.setTbUsername(payload.get("tbUsername"));
            }
            if (payload.containsKey("tbPassword")) {
                String pass = payload.get("tbPassword");
                if (pass != null && !pass.trim().isEmpty()) {
                    user.setTbPassword(CryptoUtil.encrypt(pass));
                }
            }

            userRepository.save(user);
            // Retorna formato JSON válido para o Android/Web não quebrar!
            return ResponseEntity.ok(Map.of("message", "Usuário aprovado com sucesso!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao processar dados: " + e.getMessage()));
        }
    }

    // --- 4. MUDAR STATUS (Banir, Rejeitar, etc.) ---
    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeUserStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuário não encontrado."));
        }

        AppUser user = userOpt.get();
        String newStatus = payload.get("status");

        try {
            if (newStatus != null && !newStatus.trim().isEmpty()) {
                user.setStatus(UserStatus.valueOf(newStatus.toUpperCase()));
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "Status alterado para " + user.getStatus()));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Status não pode estar vazio."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status inválido."));
        }
    }

    // --- 5. EDITAR USUÁRIO (Credenciais TB e Validade) ---
    @PutMapping("/{id}")
    public ResponseEntity<?> editUser(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<AppUser> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuário não encontrado."));
        }

        AppUser user = userOpt.get();

        try {
            // Atualiza Validade de forma segura
            if (payload.containsKey("expirationDate")) {
                String expDateStr = payload.get("expirationDate");
                if (expDateStr != null && !expDateStr.trim().isEmpty()) {
                    user.setExpirationDate(LocalDateTime.parse(expDateStr, DateTimeFormatter.ISO_DATE_TIME));
                } else {
                    user.setExpirationDate(null); // Remove a validade (vitalício)
                }
            }

            // Atualiza Credenciais do TB de forma segura
            if (payload.containsKey("tbUrl") && payload.get("tbUrl") != null) {
                user.setTbUrl(payload.get("tbUrl"));
            }
            if (payload.containsKey("tbUsername") && payload.get("tbUsername") != null) {
                user.setTbUsername(payload.get("tbUsername"));
            }
            if (payload.containsKey("tbPassword")) {
                String pass = payload.get("tbPassword");
                if (pass != null && !pass.trim().isEmpty()) {
                    user.setTbPassword(CryptoUtil.encrypt(pass));
                }
            }

            userRepository.save(user);
            // Retorna JSON!
            return ResponseEntity.ok(Map.of("message", "Usuário atualizado com sucesso!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao atualizar usuário: " + e.getMessage()));
        }
    }
}