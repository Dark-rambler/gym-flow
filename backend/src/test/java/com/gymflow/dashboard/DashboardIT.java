package com.gymflow.dashboard;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

// "Hoy" = 2026-03-10.
class DashboardIT extends ApiTestSupport {

    @Test
    void countsActiveFrozenAndExpiringWithoutRenewal() throws Exception {
        Session owner = registerGym("Gym Tablero", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        openCash(owner);

        long vence = createMember(owner, "Vence Pronto", uniqueDni());
        long renovo = createMember(owner, "Ya Renovó", uniqueDni());
        long congelado = createMember(owner, "Congelado", uniqueDni());
        long lejos = createMember(owner, "Vence Lejos", uniqueDni());
        long baja = createMember(owner, "Dado De Baja", uniqueDni());
        createMember(owner, "Sin Plan", uniqueDni());

        // se venden el 10/03 (vencen el 08/04); luego se adelanta el reloj al 03/04 → faltan 6 días
        assign(owner, vence, monthly);
        assign(owner, renovo, monthly);
        long frozenMs = idOf(assign(owner, congelado, monthly));
        assign(owner, baja, monthly);
        patchJson("/api/members/" + baja + "/active", """
                {"active":false}""", owner.accessToken());  // desactivado: no aparece en "por vencer"
        clock.advanceDays(24);
        assign(owner, renovo, monthly);                 // renovación programada
        assign(owner, lejos, monthly);                  // nueva desde hoy, vence en mayo
        postJson("/api/memberships/" + frozenMs + "/freeze", "", owner.accessToken()).andExpect(status().isOk());

        getWithToken("/api/dashboard/summary", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeMembers", is(4)))   // vence, renovo, lejos, baja (congelado no)
                .andExpect(jsonPath("$.frozenMembers", is(1)))
                .andExpect(jsonPath("$.checkInsToday", is(0)))
                .andExpect(jsonPath("$.expiringSoon[*].memberName", contains("Vence Pronto")))
                .andExpect(jsonPath("$.expiringSoon[0].daysLeft", is(6)));
    }

    @Test
    void summaryIsIsolatedBetweenGyms() throws Exception {
        Session gymA = registerGym("Gym Tablero A", uniqueEmail("a"));
        long plan = createPlan(gymA, "Mensual", 30, "100");
        openCash(gymA);
        assign(gymA, createMember(gymA, "Uno", uniqueDni()), plan);

        getWithToken("/api/dashboard/summary", registerGym("Gym Tablero B", uniqueEmail("b")).accessToken())
                .andExpect(jsonPath("$.activeMembers", is(0)));
    }
}
