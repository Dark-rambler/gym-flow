package com.gymflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.gymflow.shared.infrastructure.config.JwtProperties;
import com.gymflow.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class AuthFlowIT extends ApiTestSupport {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void registerLoginAndMe() throws Exception {
        String email = uniqueEmail("owner");
        Session registered = registerGym("Gym Fuerza Perú", email);

        Session session = login(email.toUpperCase(), PASSWORD); // el email no distingue mayúsculas

        getWithToken("/api/me", session.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("OWNER")))
                .andExpect(jsonPath("$.gymId", is((int) registered.gymId())))
                .andExpect(jsonPath("$.gymName", is("Gym Fuerza Perú")));
    }

    @Test
    void wrongPasswordOrUnknownEmailGive401WithSameMessage() throws Exception {
        String email = uniqueEmail("owner");
        registerGym("Gym Login", email);

        postJson("/api/auth/login", """
                {"email":"%s","password":"incorrecta"}""".formatted(email))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Email o contraseña incorrectos")));
        postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(uniqueEmail("nadie"), PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Email o contraseña incorrectos")));
    }

    @Test
    void duplicateEmailGives409() throws Exception {
        String email = uniqueEmail("dup");
        registerGym("Gym Uno", email);

        postJson("/api/auth/register-gym", """
                {"gymName":"Gym Dos","ownerName":"Otro","email":"%s","password":"%s"}""".formatted(email, PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("El email ya está registrado")));
    }

    @Test
    void invalidBodyGives400WithFieldErrors() throws Exception {
        postJson("/api/auth/register-gym", """
                {"gymName":"","ownerName":"X","email":"no-es-email","password":"corta"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.gymName", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void refreshRotatesAndReuseRevokesAllSessions() throws Exception {
        Session first = registerGym("Gym Refresh", uniqueEmail("refresh"));

        Session second = toSession(postJson("/api/auth/refresh", refreshBody(first.refreshToken()))
                .andExpect(status().isOk()));

        // reutilizar el token ya rotado → 401 y se revoca también el vigente
        postJson("/api/auth/refresh", refreshBody(first.refreshToken())).andExpect(status().isUnauthorized());
        postJson("/api/auth/refresh", refreshBody(second.refreshToken())).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Session session = registerGym("Gym Logout", uniqueEmail("logout"));

        postJson("/api/auth/logout", refreshBody(session.refreshToken())).andExpect(status().isNoContent());
        postJson("/api/auth/refresh", refreshBody(session.refreshToken())).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireValidToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
        getWithToken("/api/me", "token.invalido.xx").andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshWithSameTokenYieldsAtMostOneSession() throws Exception {
        Session session = registerGym("Gym Carrera", uniqueEmail("race"));
        int threads = 5;
        var start = new CountDownLatch(1);
        var ok = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    start.await();
                    int code = postJson("/api/auth/refresh", refreshBody(session.refreshToken()))
                            .andReturn().getResponse().getStatus();
                    if (code == 200) {
                        ok.incrementAndGet();
                    }
                    return code;
                });
            }
            start.countDown();
        }
        assertThat(ok.get()).isLessThanOrEqualTo(1);
    }

    @Test
    void staleBearerHeaderDoesNotBlockRefresh() throws Exception {
        Session session = registerGym("Gym Bearer", uniqueEmail("bearer"));

        postJson("/api/auth/refresh", refreshBody(session.refreshToken()), "token.caducado.xx")
                .andExpect(status().isOk());
    }

    @Test
    void forgedTokenWithSystemGymIdSeesNothing() throws Exception {
        Session victim = registerGym("Gym Victima", uniqueEmail("victim"));
        // token bien firmado pero con gymId = -1 (SYSTEM): no debe desactivar el filtro de tenant
        String forged = signToken(victim.userId(), -1L, "OWNER");

        getWithToken("/api/staff", forged).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getWithToken("/api/me", forged).andExpect(status().isUnauthorized());
    }

    @Test
    void multibytePasswordOver72BytesIsRejectedWith400() throws Exception {
        postJson("/api/auth/register-gym", """
                {"gymName":"Gym Ñ","ownerName":"X","email":"%s","password":"%s"}""".formatted(uniqueEmail("n"), "ñ".repeat(40)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    private String signToken(long userId, long gymId, String role) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(JwtProperties.ISSUER).subject(Long.toString(userId))
                .issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("gymId", gymId).claim("role", role).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String refreshBody(String token) {
        return """
                {"refreshToken":"%s"}""".formatted(token);
    }
}
