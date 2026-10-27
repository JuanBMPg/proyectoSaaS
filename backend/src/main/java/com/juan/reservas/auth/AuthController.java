package com.juan.reservas.auth;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.dto.AccessTokenResponse;
import com.juan.reservas.auth.dto.LoginRequest;
import com.juan.reservas.auth.dto.RegistroRequest;
import com.juan.reservas.comun.NoAutorizadoException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie cookie;

    public AuthController(AuthService authService, RefreshTokenCookie cookie) {
        this.authService = authService;
        this.cookie = cookie;
    }

    @Operation(summary = "Da de alta un negocio con su usuario propietario e inicia sesión")
    @PostMapping("/registro")
    public ResponseEntity<AccessTokenResponse> registrar(@Valid @RequestBody RegistroRequest peticion) {
        Sesion sesion = authService.registrar(peticion);
        return conSesion(ResponseEntity.created(URI.create("/api/panel/negocio")), sesion);
    }

    @Operation(summary = "Inicia sesión con email y contraseña")
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest peticion) {
        return conSesion(ResponseEntity.ok(), authService.login(peticion));
    }

    @Operation(summary = "Renueva el access token usando la cookie del refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refrescar(
            @CookieValue(name = RefreshTokenCookie.NOMBRE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new NoAutorizadoException("No hay sesión que renovar");
        }
        return conSesion(ResponseEntity.ok(), authService.refrescar(refreshToken));
    }

    @Operation(summary = "Cierra la sesión revocando el refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NOMBRE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.borrar().toString())
                .build();
    }

    private ResponseEntity<AccessTokenResponse> conSesion(ResponseEntity.BodyBuilder respuesta, Sesion sesion) {
        return respuesta
                .header(HttpHeaders.SET_COOKIE, cookie.crear(sesion.refreshToken()).toString())
                .body(AccessTokenResponse.bearer(sesion.accessToken().valor(), sesion.accessToken().expiraEn()));
    }
}
