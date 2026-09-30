package com.gymflow.membership;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

// "Hoy" en los tests es 2026-03-10 en Lima (TestClockConfiguration).
class MembershipIT extends ApiTestSupport {

    @Test
    void assignComputesInclusiveDatesAndRenewalChains() throws Exception {
        Session owner = registerGym("Gym Venta", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100.00");
        long quarterly = createPlan(owner, "Trimestral", 90, "270.00");
        long member = createMember(owner, "Ana", uniqueDni());

        assign(owner, member, monthly)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate", is("2026-03-10")))
                .andExpect(jsonPath("$.endDate", is("2026-04-08")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.daysLeft", is(30)))
                .andExpect(jsonPath("$.price", is(100.0)));

        assign(owner, member, quarterly)
                .andExpect(jsonPath("$.startDate", is("2026-04-09")))
                .andExpect(jsonPath("$.endDate", is("2026-07-07")))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));

        // como máximo una renovación por adelantado
        assign(owner, member, monthly)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("El socio ya tiene una renovación programada")));

        getWithToken("/api/members/" + member, owner.accessToken())
                .andExpect(jsonPath("$.memberships", hasSize(2)))
                .andExpect(jsonPath("$.memberships[0].planName", is("Trimestral"))) // más reciente primero
                .andExpect(jsonPath("$.currentMembership.planName", is("Mensual")));
    }

    @Test
    void expiredMembershipIsDerivedFromDateAndRenewalStartsToday() throws Exception {
        Session owner = registerGym("Gym Vence", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Beto", uniqueDni());
        assign(owner, member, monthly);

        clock.advanceDays(45);

        getWithToken("/api/members?q=Beto", owner.accessToken())
                .andExpect(jsonPath("$.items[0].currentMembership.status", is("EXPIRED")))
                .andExpect(jsonPath("$.items[0].currentMembership.daysLeft", is(0)));
        assign(owner, member, monthly).andExpect(jsonPath("$.startDate", is("2026-04-24")));
    }

    @Test
    void freezeAndUnfreezeAddsFrozenDaysToEndDate() throws Exception {
        Session owner = registerGym("Gym Congela", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Carla", uniqueDni());
        long membership = idOf(assign(owner, member, monthly));

        postJson("/api/memberships/" + membership + "/freeze", "", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FROZEN")))
                .andExpect(jsonPath("$.frozenSince", is("2026-03-10")));
        // no se puede renovar mientras está congelada
        assign(owner, member, monthly).andExpect(status().isConflict());

        clock.advanceDays(10);

        postJson("/api/memberships/" + membership + "/unfreeze", "", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.endDate", is("2026-04-18"))) // 04-08 + 10
                .andExpect(jsonPath("$.frozenDays", is(10)))
                .andExpect(jsonPath("$.frozenSince", nullValue()));
    }

    @Test
    void cannotFreezeWithScheduledRenewal() throws Exception {
        Session owner = registerGym("Gym Programada", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Diego", uniqueDni());
        long current = idOf(assign(owner, member, monthly));
        assign(owner, member, monthly);

        postJson("/api/memberships/" + current + "/freeze", "", owner.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("No se puede congelar: el socio tiene una renovación programada")));
    }

    @Test
    void cancelledMembershipDoesNotCountForChain() throws Exception {
        Session owner = registerGym("Gym Cancela", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Elena", uniqueDni());
        long first = idOf(assign(owner, member, monthly));

        postJson("/api/memberships/" + first + "/cancel", "", owner.accessToken())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        assign(owner, member, monthly).andExpect(jsonPath("$.startDate", is("2026-03-10")));
    }

    @Test
    void receptionistSellsAtPlanPriceButCannotOverridePriceOrFreeze() throws Exception {
        Session owner = registerGym("Gym Recep Venta", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        String email = uniqueEmail("recep");
        createStaff(owner, email, "RECEPTIONIST");
        Session recep = login(email, PASSWORD);
        long member = createMember(recep, "Fede", uniqueDni());

        postJson("/api/members/" + member + "/memberships", """
                {"planId":%d,"price":1}""".formatted(monthly), recep.accessToken())
                .andExpect(status().isForbidden());
        long membership = idOf(assign(recep, member, monthly).andExpect(status().isCreated()));
        postJson("/api/memberships/" + membership + "/freeze", "", recep.accessToken())
                .andExpect(status().isForbidden());

        // el dueño sí puede aplicar descuento
        long other = createMember(owner, "Gabi", uniqueDni());
        postJson("/api/members/" + other + "/memberships", """
                {"planId":%d,"price":80.50}""".formatted(monthly), owner.accessToken())
                .andExpect(jsonPath("$.price", is(80.5)));
    }

    @Test
    void cannotUseMemberOrPlanFromAnotherGym() throws Exception {
        Session gymA = registerGym("Gym Plan A", uniqueEmail("a"));
        long planA = createPlan(gymA, "Mensual", 30, "100");
        long memberA = createMember(gymA, "Hugo", uniqueDni());
        long membershipA = idOf(assign(gymA, memberA, planA));
        Session gymB = registerGym("Gym Plan B", uniqueEmail("b"));
        long planB = createPlan(gymB, "Mensual", 30, "100");
        long memberB = createMember(gymB, "Iris", uniqueDni());

        assign(gymB, memberB, planA).andExpect(status().isNotFound());
        assign(gymB, memberA, planB).andExpect(status().isNotFound());
        for (String action : new String[] {"cancel", "freeze", "unfreeze"}) {
            postJson("/api/memberships/" + membershipA + "/" + action, "", gymB.accessToken())
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void inactivePlanOrMemberCannotBeSold() throws Exception {
        Session owner = registerGym("Gym Inactivo", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Juan", uniqueDni());
        patchJson("/api/members/" + member + "/active", """
                {"active":false}""", owner.accessToken());

        assign(owner, member, monthly)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("El socio está desactivado")));
    }
}
