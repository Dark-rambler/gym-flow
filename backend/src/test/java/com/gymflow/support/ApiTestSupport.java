package com.gymflow.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.gymflow.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Base de tests de API: Postgres real (Testcontainers) compartido entre clases; cada test usa emails únicos.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "secreto123";

    @Autowired
    protected MockMvc mvc;

    protected record Session(String accessToken, String refreshToken, long userId, long gymId) {
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions postJson(String url, String body, String token) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body).header("Authorization", "Bearer " + token));
    }

    protected ResultActions patchJson(String url, String body, String token) throws Exception {
        return mvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(body).header("Authorization", "Bearer " + token));
    }

    protected ResultActions getWithToken(String url, String token) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    protected Session registerGym(String gymName, String email) throws Exception {
        String body = """
                {"gymName":"%s","ownerName":"Dueño %s","email":"%s","password":"%s"}
                """.formatted(gymName, gymName, email, PASSWORD);
        return toSession(postJson("/api/auth/register-gym", body).andExpect(status().isCreated()));
    }

    protected Session login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        return toSession(postJson("/api/auth/login", body).andExpect(status().isOk()));
    }

    protected long createStaff(Session as, String email, String role) throws Exception {
        String body = """
                {"fullName":"Staff %s","email":"%s","password":"%s","role":"%s"}
                """.formatted(role, email, PASSWORD, role);
        String json = postJson("/api/staff", body, as.accessToken()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected static Session toSession(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return new Session(
                JsonPath.read(json, "$.accessToken"),
                JsonPath.read(json, "$.refreshToken"),
                ((Number) JsonPath.read(json, "$.user.id")).longValue(),
                ((Number) JsonPath.read(json, "$.user.gymId")).longValue());
    }
}
