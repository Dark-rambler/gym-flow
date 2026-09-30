package com.gymflow.plan;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class PlanIT extends ApiTestSupport {

    @Test
    void ownerCreatesListsAndDeactivatesPlans() throws Exception {
        Session owner = registerGym("Gym Planes", uniqueEmail("owner"));
        createPlan(owner, "Trimestral", 90, "270.00");
        long monthly = createPlan(owner, "Mensual", 30, "100.00");

        getWithToken("/api/plans", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Mensual", "Trimestral"))) // por duración
                .andExpect(jsonPath("$[0].price", is(100.0)));

        mvc.perform(MockMvcRequestBuilders.put("/api/plans/" + monthly)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mensual","durationDays":30,"price":110.00,"active":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        getWithToken("/api/plans", owner.accessToken()).andExpect(jsonPath("$", hasSize(1)));
        getWithToken("/api/plans?includeInactive=true", owner.accessToken()).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void duplicateNameGives409AndInvalidDataGives400() throws Exception {
        Session owner = registerGym("Gym Dup Plan", uniqueEmail("owner"));
        createPlan(owner, "Mensual", 30, "100");

        postJson("/api/plans", """
                {"name":"Mensual","durationDays":30,"price":90}""", owner.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Ya existe un plan con ese nombre")));
        postJson("/api/plans", """
                {"name":"Raro","durationDays":0,"price":-1}""", owner.accessToken())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.durationDays", is("Mínimo 1 día")))
                .andExpect(jsonPath("$.errors.price", is("El precio no puede ser negativo")));
    }

    @Test
    void receptionistCanListButNotManagePlans() throws Exception {
        Session owner = registerGym("Gym Recep Plan", uniqueEmail("owner"));
        createPlan(owner, "Mensual", 30, "100");
        String email = uniqueEmail("recep");
        createStaff(owner, email, "RECEPTIONIST");
        Session recep = login(email, PASSWORD);

        getWithToken("/api/plans", recep.accessToken()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        postJson("/api/plans", """
                {"name":"Pirata","durationDays":30,"price":1}""", recep.accessToken())
                .andExpect(status().isForbidden());
    }

    @Test
    void plansAreIsolatedBetweenGyms() throws Exception {
        Session gymA = registerGym("Gym A Planes", uniqueEmail("a"));
        long planA = createPlan(gymA, "Mensual", 30, "100");
        Session gymB = registerGym("Gym B Planes", uniqueEmail("b"));

        getWithToken("/api/plans", gymB.accessToken()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(MockMvcRequestBuilders.put("/api/plans/" + planA)
                        .header("Authorization", "Bearer " + gymB.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Robado","durationDays":30,"price":1}"""))
                .andExpect(status().isNotFound());
    }
}
