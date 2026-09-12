package com.example.ridersafety.controller;

import com.example.ridersafety.model.User;
import com.example.ridersafety.repository.UserRepository;
import com.example.ridersafety.service.CurrentUser;
import com.example.ridersafety.service.DtoMapper;
import com.example.ridersafety.util.Payload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String username = Payload.require(body, "username", "用户名");
        String password = Payload.require(body, "password", "密码");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);
            User u = userRepository.findByUsername(username).orElseThrow();
            return ResponseEntity.ok(DtoMapper.user(u));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return DtoMapper.user(currentUser.require(auth));
    }
}
