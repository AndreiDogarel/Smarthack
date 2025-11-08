package com.example.dodo.controller;

import com.example.dodo.entities.Role;
import com.example.dodo.entities.User;
import com.example.dodo.entities.UserLoginDto;
import com.example.dodo.entities.UserRegisterDto;
import com.example.dodo.repository.RoleRepository;
import com.example.dodo.repository.UserRepository;
import com.example.dodo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
//@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String,Object>> register(@RequestBody UserRegisterDto body) {

        if (body.getUsername() == null || body.getUsername().isBlank()
                || body.getPassword() == null || body.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message","username/password lipsă"));
        }

        String roleName = (body.getRole() == null || body.getRole().isBlank())
                ? "CUSTOMER" : body.getRole().toUpperCase();

        User u = new User();
        u.setUsername(body.getUsername());
        u.setPassword(passwordEncoder.encode(body.getPassword()));

        // dacă folosești enum Role + repo pe enum
        try {
            Role enumRole = Role.valueOf(roleName);
            u.setRole(roleRepository.getByName(enumRole)); // ajustează dacă semnătura e diferită
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message","rol invalid"));
        }

        userRepository.save(u);

        Map<String,Object> out = new HashMap<>();
        out.put("id", u.getId());
        out.put("username", u.getUsername());
        out.put("roleName", u.getRole().getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }


    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<com.example.dodo.entities.LoginResponse> login(@RequestBody UserLoginDto user) {
        var existing = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));

        boolean ok = passwordEncoder.matches(user.getPassword(), existing.getPassword());
        if (!ok) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(existing);
        return ResponseEntity.ok(new com.example.dodo.entities.LoginResponse(token));
    }


    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String test() {
        return "test";
    }
}