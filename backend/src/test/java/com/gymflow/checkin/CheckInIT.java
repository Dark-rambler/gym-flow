package com.gymflow.checkin;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

// "Hoy" = 2026-03-10 10:00 en Lima.
class CheckInIT extends ApiTestSupport {

    private ResultActions checkIn(Session as, String code) throws Exception {
        return postJson("/api/check-ins", """
                {"code":"%s"}""".formatted(code), as.accessToken());
    }

    private String qrPayload(Session as, long memberId) throws Exception {
        String json = getWithToken("/api/members/" + memberId + "/qr", as.accessToken()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.payload");
    }

    @Test
    void activeMemberEntersByDniAndQrAndSecondReadIsSameVisit() throws Exception {
        Session owner = registerGym("Gym Entrada", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        String dni = uniqueDni();
        long member = createMember(owner, "Ana Torres", dni);
        openCash(owner);
        assign(owner, member, plan);

        checkIn(owner, " " + dni + " ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is("ALLOWED")))
                .andExpect(jsonPath("$.duplicate", is(false)))
                .andExpect(jsonPath("$.message", is("Bienvenido/a, Ana")))
                .andExpect(jsonPath("$.membership.planName", is("Mensual")))
                .andExpect(jsonPath("$.membership.daysLeft", is(30)));
        checkIn(owner, qrPayload(owner, member))
                .andExpect(jsonPath("$.result", is("ALLOWED")))
                .andExpect(jsonPath("$.duplicate", is(true)));

        getWithToken("/api/check-ins", owner.accessToken()).andExpect(jsonPath("$.items", hasSize(1)));

        clock.set(clock.instant().plusSeconds(3 * 3600)); // 3 h después: otra visita
        checkIn(owner, dni).andExpect(jsonPath("$.duplicate", is(false)));
        getWithToken("/api/dashboard/summary", owner.accessToken()).andExpect(jsonPath("$.checkInsToday", is(2)));
    }

    @Test
    void deniedReasonsAreReportedAndLogged() throws Exception {
        Session owner = registerGym("Gym Denegado", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        openCash(owner);

        String noMembership = uniqueDni();
        createMember(owner, "Sin Plan", noMembership);
        checkIn(owner, noMembership)
                .andExpect(jsonPath("$.result", is("DENIED")))
                .andExpect(jsonPath("$.reason", is("NO_MEMBERSHIP")))
                .andExpect(jsonPath("$.member.fullName", is("Sin Plan")))
                .andExpect(jsonPath("$.membership", nullValue()));

        checkIn(owner, "99999999")
                .andExpect(jsonPath("$.reason", is("UNKNOWN")))
                .andExpect(jsonPath("$.member", nullValue()));
        checkIn(owner, "%%%").andExpect(jsonPath("$.reason", is("INVALID_CODE")));

        String frozenDni = uniqueDni();
        long frozen = createMember(owner, "Congelado", frozenDni);
        long frozenMembership = idOf(assign(owner, frozen, plan));
        postJson("/api/memberships/" + frozenMembership + "/freeze", "", owner.accessToken());
        checkIn(owner, frozenDni).andExpect(jsonPath("$.reason", is("FROZEN")));

        String inactiveDni = uniqueDni();
        long inactive = createMember(owner, "Inactivo", inactiveDni);
        assign(owner, inactive, plan);
        patchJson("/api/members/" + inactive + "/active", """
                {"active":false}""", owner.accessToken());
        checkIn(owner, inactiveDni).andExpect(jsonPath("$.reason", is("MEMBER_INACTIVE")));

        // INVALID_CODE no se registra; el resto sí
        getWithToken("/api/check-ins", owner.accessToken())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].memberName", is("Inactivo")))
                .andExpect(jsonPath("$.items[2].memberName", nullValue()));
    }

    @Test
    void expiredAndNotStartedMemberships() throws Exception {
        Session owner = registerGym("Gym Fechas", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        String dni = uniqueDni();
        long member = createMember(owner, "Beto", dni);
        openCash(owner);
        assign(owner, member, plan);

        clock.advanceDays(31);
        checkIn(owner, dni)
                .andExpect(jsonPath("$.reason", is("EXPIRED")))
                .andExpect(jsonPath("$.membership.status", is("EXPIRED")));
    }

    @Test
    void qrOfAnotherGymIsUnknown() throws Exception {
        Session gymA = registerGym("Gym QR A", uniqueEmail("a"));
        long plan = createPlan(gymA, "Mensual", 30, "100");
        long member = createMember(gymA, "Carla", uniqueDni());
        openCash(gymA);
        assign(gymA, member, plan);
        String payload = qrPayload(gymA, member);

        Session gymB = registerGym("Gym QR B", uniqueEmail("b"));
        checkIn(gymB, payload).andExpect(jsonPath("$.reason", is("UNKNOWN"))).andExpect(jsonPath("$.member", nullValue()));
        getWithToken("/api/members/" + member + "/qr", gymB.accessToken()).andExpect(status().isNotFound());
        postJson("/api/members/" + member + "/qr/rotate", "", gymB.accessToken()).andExpect(status().isNotFound());
    }

    @Test
    void dniOfAnotherGymIsUnknown() throws Exception {
        Session gymA = registerGym("Gym DNI Otro A", uniqueEmail("a"));
        long plan = createPlan(gymA, "Mensual", 30, "100");
        String dni = uniqueDni();
        long member = createMember(gymA, "Dora", dni);
        openCash(gymA);
        assign(gymA, member, plan);

        checkIn(registerGym("Gym DNI Otro B", uniqueEmail("b")), dni)
                .andExpect(jsonPath("$.reason", is("UNKNOWN")))
                .andExpect(jsonPath("$.member", nullValue()));
    }

    @Test
    void concurrentScansOfSameMemberCountOnce() throws Exception {
        Session owner = registerGym("Gym Doble Scan", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        String dni = uniqueDni();
        long member = createMember(owner, "Eli", dni);
        openCash(owner);
        assign(owner, member, plan);

        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < 4; i++) {
                futures.add(pool.submit(() -> checkIn(owner, dni).andReturn().getResponse().getStatus()));
            }
            for (var f : futures) {
                org.assertj.core.api.Assertions.assertThat(f.get()).isEqualTo(200);
            }
        }
        getWithToken("/api/dashboard/summary", owner.accessToken()).andExpect(jsonPath("$.checkInsToday", is(1)));
    }
}
