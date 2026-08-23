package com.baraseck.supportflow.service;

import com.baraseck.supportflow.dto.LoginRequest;
import com.baraseck.supportflow.dto.LoginResponse;
import com.baraseck.supportflow.mapper.UserSummaryMapper;
import com.baraseck.supportflow.repository.UserRepository;
import com.baraseck.supportflow.security.JwtService;
import com.baraseck.supportflow.security.SupportFlowPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserSummaryMapper mapper;
    public LoginResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        var principal = (SupportFlowPrincipal) auth.getPrincipal();
        var user = userRepository.findById(principal.id()).orElseThrow();
        return new LoginResponse(jwtService.generate(principal), "Bearer", jwtService.expirationSeconds(), mapper.toSummary(user));
    }
}
