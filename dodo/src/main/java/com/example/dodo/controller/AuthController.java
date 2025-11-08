package com.example.dodo.controller;

import com.example.dodo.entities.*;
import com.example.dodo.repository.RoleRepository;
import com.example.dodo.repository.UserRepository;
import com.example.dodo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public String register(@RequestBody UserRegisterDto user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User user1 = new User(user);
        user1.setRole(roleRepository.getByName(Role.valueOf(user.getRole().toString().toUpperCase())));
        userRepository.save(user1);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDto user) {

        var existing = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(user.getPassword(), existing.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtService.generateToken(existing);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String test() {
        return "test";
    }
}