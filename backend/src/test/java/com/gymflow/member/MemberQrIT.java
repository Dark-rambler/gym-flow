package com.gymflow.member;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class MemberQrIT extends ApiTestSupport {

    @Test
    void publicCardWorksWithoutLoginAndRotationInvalidatesOldQr() throws Exception {
        Session owner = registerGym("Gym Carnet", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        String dni = uniqueDni();
        long member = createMember(owner, "Diana Ruiz", dni);
        openCash(owner);
        assign(owner, member, plan);

        String qr = getWithToken("/api/members/" + member + "/qr", owner.accessToken())
                .andExpect(jsonPath("$.payload", startsWith("GF1:")))
                .andReturn().getResponse().getContentAsString();
        String oldToken = JsonPath.read(qr, "$.qrToken");
        String oldPayload = JsonPath.read(qr, "$.payload");

        mvc.perform(get("/api/public/member-card/" + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gymName", is("Gym Carnet")))
                .andExpect(jsonPath("$.memberName", is("Diana Ruiz")))
                .andExpect(jsonPath("$.planName", is("Mensual")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.dni").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
        // un Bearer caducado no bloquea la ruta pública
        mvc.perform(get("/api/public/member-card/" + oldToken).header("Authorization", "Bearer caducado.x.y"))
                .andExpect(status().isOk());

        String rotated = postJson("/api/members/" + member + "/qr/rotate", "", owner.accessToken())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newToken = JsonPath.read(rotated, "$.qrToken");

        mvc.perform(get("/api/public/member-card/" + oldToken)).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/member-card/" + newToken)).andExpect(status().isOk());
        postJson("/api/check-ins", """
                {"code":"%s"}""".formatted(oldPayload), owner.accessToken())
                .andExpect(jsonPath("$.reason", is("UNKNOWN")));
    }

    @Test
    void publicCardOfDeactivatedMemberIsGone() throws Exception {
        Session owner = registerGym("Gym Carnet Baja", uniqueEmail("owner"));
        long member = createMember(owner, "Fabio", uniqueDni());
        String qr = getWithToken("/api/members/" + member + "/qr", owner.accessToken()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(qr, "$.qrToken");
        patchJson("/api/members/" + member + "/active", """
                {"active":false}""", owner.accessToken()).andExpect(status().isOk());

        mvc.perform(get("/api/public/member-card/" + token)).andExpect(status().isNotFound());
        // el modo sistema de la ruta pública no deja rastro en la siguiente petición autenticada
        getWithToken("/api/members", registerGym("Gym Limpio", uniqueEmail("clean")).accessToken())
                .andExpect(jsonPath("$.totalItems", is(0)));
    }

    @Test
    void onlyOwnerOrAdminCanRotate() throws Exception {
        Session owner = registerGym("Gym Rotar", uniqueEmail("owner"));
        long member = createMember(owner, "Eva", uniqueDni());
        String email = uniqueEmail("recep");
        createStaff(owner, email, "RECEPTIONIST");
        Session recep = login(email, PASSWORD);

        getWithToken("/api/members/" + member + "/qr", recep.accessToken()).andExpect(status().isOk());
        postJson("/api/members/" + member + "/qr/rotate", "", recep.accessToken()).andExpect(status().isForbidden());
    }

    @Test
    void unknownOrMalformedTokenGives404Or400() throws Exception {
        mvc.perform(get("/api/public/member-card/00000000-0000-4000-8000-000000000000")).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/member-card/no-es-uuid")).andExpect(status().isBadRequest());
    }
}
