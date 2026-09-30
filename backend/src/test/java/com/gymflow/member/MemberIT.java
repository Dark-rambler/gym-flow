package com.gymflow.member;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class MemberIT extends ApiTestSupport {

    @Test
    void createAndSearchByNameOrDni() throws Exception {
        Session owner = registerGym("Gym Socios", uniqueEmail("owner"));
        createMember(owner, "María López", "40123456");
        createMember(owner, "José Pérez", "70654321");
        createMember(owner, "Ana Mariátegui", "12345678");

        getWithToken("/api/members?q=mar", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].fullName", contains("Ana Mariátegui", "María López")))
                .andExpect(jsonPath("$.totalItems", is(2)));
        getWithToken("/api/members?q=0654", owner.accessToken())
                .andExpect(jsonPath("$.items[*].fullName", contains("José Pérez")))
                .andExpect(jsonPath("$.items[0].currentMembership", nullValue()));
        // % y _ son literales, no comodines
        getWithToken("/api/members?q=%25", owner.accessToken()).andExpect(jsonPath("$.totalItems", is(0)));
    }

    @Test
    void paginatesSortedByName() throws Exception {
        Session owner = registerGym("Gym Paginas", uniqueEmail("owner"));
        for (String name : new String[] {"Carla", "Alberto", "Beatriz", "Diego", "Elena"}) {
            createMember(owner, name, uniqueDni());
        }

        getWithToken("/api/members?page=1&size=2", owner.accessToken())
                .andExpect(jsonPath("$.items[*].fullName", contains("Carla", "Diego")))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.totalItems", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)));
    }

    @Test
    void dniIsUniquePerGymAndNormalized() throws Exception {
        Session gymA = registerGym("Gym DNI A", uniqueEmail("a"));
        Session gymB = registerGym("Gym DNI B", uniqueEmail("b"));
        createMember(gymA, "Luis", " ab123456 ");

        postJson("/api/members", """
                {"fullName":"Otro Luis","dni":"AB123456"}""", gymA.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Ya existe un socio con ese DNI")));
        // el mismo DNI en otro gimnasio es otra persona para ese gym
        createMember(gymB, "Luis", "AB123456");
    }

    @Test
    void getReturnsDetailAndInvalidDataGives400() throws Exception {
        Session owner = registerGym("Gym Ficha", uniqueEmail("owner"));
        long id = createMember(owner, "Rosa", "44556677");

        getWithToken("/api/members/" + id, owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni", is("44556677")))
                .andExpect(jsonPath("$.qrToken").doesNotExist()) // credencial de check-in: no va en la ficha
                .andExpect(jsonPath("$.memberships", hasSize(0)));
        postJson("/api/members", """
                {"fullName":"","dni":"12","email":"no-email","birthDate":"2999-01-01"}""", owner.accessToken())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName", notNullValue()))
                .andExpect(jsonPath("$.errors.dni", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.birthDate", notNullValue()));
    }

    @Test
    void membersAreIsolatedBetweenGyms() throws Exception {
        Session gymA = registerGym("Gym Iso A", uniqueEmail("a"));
        long memberA = createMember(gymA, "Secreto", uniqueDni());
        Session gymB = registerGym("Gym Iso B", uniqueEmail("b"));

        getWithToken("/api/members", gymB.accessToken()).andExpect(jsonPath("$.totalItems", is(0)));
        getWithToken("/api/members/" + memberA, gymB.accessToken()).andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.put("/api/members/" + memberA)
                        .header("Authorization", "Bearer " + gymB.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Hackeado","dni":"99999999"}"""))
                .andExpect(status().isNotFound());
        patchJson("/api/members/" + memberA + "/active", """
                {"active":false}""", gymB.accessToken()).andExpect(status().isNotFound());
    }

    @Test
    void onlyOwnerOrAdminCanDeactivateMembers() throws Exception {
        Session owner = registerGym("Gym Baja Socio", uniqueEmail("owner"));
        long id = createMember(owner, "Pedro", uniqueDni());
        String email = uniqueEmail("recep");
        createStaff(owner, email, "RECEPTIONIST");
        Session recep = login(email, PASSWORD);

        patchJson("/api/members/" + id + "/active", """
                {"active":false}""", recep.accessToken()).andExpect(status().isForbidden());
        patchJson("/api/members/" + id + "/active", """
                {"active":false}""", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }
}
