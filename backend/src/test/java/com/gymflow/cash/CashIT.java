package com.gymflow.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.gymflow.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class CashIT extends ApiTestSupport {

    @Test
    void onlyOneOpenCashPerGym() throws Exception {
        Session owner = registerGym("Gym Caja Unica", uniqueEmail("owner"));
        openCash(owner, "50.00").andExpect(status().isCreated())
                .andExpect(jsonPath("$.session.status", is("OPEN")))
                .andExpect(jsonPath("$.session.expectedCash", is(50.0)));

        openCash(owner, "10").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Ya hay una caja abierta")));
        // otro gym tiene su propia caja
        openCash(registerGym("Gym Caja Otra", uniqueEmail("other")), "0").andExpect(status().isCreated());
    }

    @Test
    void cannotSellWithClosedCash() throws Exception {
        Session owner = registerGym("Gym Sin Caja", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Ana", uniqueDni());

        assign(owner, member, plan).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("La caja está cerrada. Ábrela para poder cobrar")));
        getWithToken("/api/members/" + member, owner.accessToken()).andExpect(jsonPath("$.memberships", hasSize(0)));
    }

    @Test
    void closeComputesExpectedCashAndDifference() throws Exception {
        Session owner = registerGym("Gym Arqueo", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        long quarterly = createPlan(owner, "Trimestral", 90, "270");
        openCash(owner, "50");
        sell(owner, createMember(owner, "Ana", uniqueDni()), monthly, "CASH", UUID.randomUUID()).andExpect(status().isCreated());
        sell(owner, createMember(owner, "Beto", uniqueDni()), quarterly, "YAPE", UUID.randomUUID())
                .andExpect(jsonPath("$.payment.method", is("YAPE")))
                .andExpect(jsonPath("$.payment.amount", is(270.0)));

        getWithToken("/api/cash/current", owner.accessToken())
                .andExpect(jsonPath("$.current.session.totals.cash", is(100.0)))
                .andExpect(jsonPath("$.current.session.totals.yape", is(270.0)))
                .andExpect(jsonPath("$.current.session.totals.total", is(370.0)))
                .andExpect(jsonPath("$.current.session.expectedCash", is(150.0)))
                .andExpect(jsonPath("$.current.payments", hasSize(2)));

        postJson("/api/cash/close", """
                {"countedCash":145,"notes":"faltó sencillo"}""", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status", is("CLOSED")))
                .andExpect(jsonPath("$.session.expectedCash", is(150.0)))
                .andExpect(jsonPath("$.session.countedCash", is(145.0)))
                .andExpect(jsonPath("$.session.difference", is(-5.0)));

        getWithToken("/api/cash/current", owner.accessToken()).andExpect(jsonPath("$.current", nullValue()));
        postJson("/api/cash/close", """
                {"countedCash":0}""", owner.accessToken()).andExpect(status().isConflict());
        getWithToken("/api/cash/sessions", owner.accessToken())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].totals.total", is(370.0)));
    }

    @Test
    void sameIdempotencyKeyDoesNotChargeTwice() throws Exception {
        Session owner = registerGym("Gym Idempotente", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Carla", uniqueDni());
        openCash(owner);
        UUID key = UUID.randomUUID();

        long first = idOf(sell(owner, member, plan, "CASH", key).andExpect(status().isCreated()));
        long retry = idOf(sell(owner, member, plan, "CASH", key).andExpect(status().isCreated()));

        assertThat(retry).isEqualTo(first);
        getWithToken("/api/members/" + member, owner.accessToken()).andExpect(jsonPath("$.memberships", hasSize(1)));
        getWithToken("/api/cash/current", owner.accessToken()).andExpect(jsonPath("$.current.payments", hasSize(1)));
        // la misma clave para otro socio es un error, no una venta nueva
        sell(owner, createMember(owner, "Otro", uniqueDni()), plan, "CASH", key).andExpect(status().isConflict());
    }

    @Test
    void voidPaymentCancelsMembershipAndLeavesTotals() throws Exception {
        Session owner = registerGym("Gym Anula", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Diego", uniqueDni());
        openCash(owner);
        String sale = sell(owner, member, plan, "PLIN", UUID.randomUUID()).andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(sale, "$.payment.id")).longValue();

        postJson("/api/payments/" + paymentId + "/void", """
                {"reason":"se cobró al socio equivocado"}""", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voided", is(true)));

        getWithToken("/api/members/" + member, owner.accessToken())
                .andExpect(jsonPath("$.memberships[0].status", is("CANCELLED")))
                .andExpect(jsonPath("$.memberships[0].payment.voided", is(true)));
        getWithToken("/api/cash/current", owner.accessToken())
                .andExpect(jsonPath("$.current.session.totals.total", is(0.0)))
                .andExpect(jsonPath("$.current.payments[0].voidReason", is("se cobró al socio equivocado")));
        // el socio puede volver a comprar (la cancelada no cuenta para el encadenado)
        assign(owner, member, plan).andExpect(status().isCreated());
        postJson("/api/payments/" + paymentId + "/void", """
                {"reason":"otra vez"}""", owner.accessToken()).andExpect(status().isConflict());
    }

    @Test
    void cannotVoidPaymentOfClosedCash() throws Exception {
        Session owner = registerGym("Gym Cerrada", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        openCash(owner);
        String sale = assign(owner, createMember(owner, "Elena", uniqueDni()), plan).andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(sale, "$.payment.id")).longValue();
        postJson("/api/cash/close", """
                {"countedCash":100}""", owner.accessToken()).andExpect(status().isOk());
        openCash(owner);

        postJson("/api/payments/" + paymentId + "/void", """
                {"reason":"tarde"}""", owner.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Solo se pueden anular pagos de la caja abierta")));
    }

    @Test
    void receptionistOperatesCashButCannotVoidOrSeeHistory() throws Exception {
        Session owner = registerGym("Gym Recep Caja", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        String email = uniqueEmail("recep");
        createStaff(owner, email, "RECEPTIONIST");
        Session recep = login(email, PASSWORD);

        openCash(recep);
        String sale = assign(recep, createMember(recep, "Fede", uniqueDni()), plan).andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(sale, "$.payment.id")).longValue();
        // arqueo a ciegas: recepción no ve esperado ni totales
        getWithToken("/api/cash/current", recep.accessToken())
                .andExpect(jsonPath("$.current.session.openedByName", is("Staff RECEPTIONIST")))
                .andExpect(jsonPath("$.current.payments[0].receivedByName", is("Staff RECEPTIONIST")))
                .andExpect(jsonPath("$.current.session.expectedCash", nullValue()))
                .andExpect(jsonPath("$.current.session.totals", nullValue()));
        getWithToken("/api/cash/current", owner.accessToken())
                .andExpect(jsonPath("$.current.session.expectedCash", is(100.0)));

        postJson("/api/payments/" + paymentId + "/void", """
                {"reason":"x"}""", recep.accessToken()).andExpect(status().isForbidden());
        getWithToken("/api/cash/sessions", recep.accessToken()).andExpect(status().isForbidden());
        postJson("/api/cash/close", """
                {"countedCash":90}""", recep.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.difference", nullValue()));
        getWithToken("/api/cash/sessions", owner.accessToken())
                .andExpect(jsonPath("$.items[0].difference", is(-10.0)));
    }

    @Test
    void reusedKeyWithDifferentSaleIsRejectedButOtherGymIsIndependent() throws Exception {
        Session owner = registerGym("Gym Clave", uniqueEmail("owner"));
        long monthly = createPlan(owner, "Mensual", 30, "100");
        long quarterly = createPlan(owner, "Trimestral", 90, "270");
        long member = createMember(owner, "Juan", uniqueDni());
        openCash(owner);
        UUID key = UUID.randomUUID();
        sell(owner, member, monthly, "CASH", key).andExpect(status().isCreated());

        sell(owner, member, quarterly, "CASH", key).andExpect(status().isConflict());
        sell(owner, member, monthly, "YAPE", key).andExpect(status().isConflict());

        // la clave es por gym: en otro gimnasio es una venta nueva
        Session other = registerGym("Gym Clave B", uniqueEmail("b"));
        long otherPlan = createPlan(other, "Mensual", 30, "100");
        openCash(other);
        sell(other, createMember(other, "Kira", uniqueDni()), otherPlan, "CASH", key).andExpect(status().isCreated());
    }

    @Test
    void cannotVoidWhenALaterRenewalExists() throws Exception {
        Session owner = registerGym("Gym Renov Anula", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Leo", uniqueDni());
        openCash(owner);
        String first = assign(owner, member, plan).andReturn().getResponse().getContentAsString();
        long firstPayment = ((Number) JsonPath.read(first, "$.payment.id")).longValue();
        assign(owner, member, plan).andExpect(status().isCreated()); // renovación programada

        postJson("/api/payments/" + firstPayment + "/void", """
                {"reason":"error"}""", owner.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("El socio tiene una renovación posterior: anula primero esa venta")));
    }

    @Test
    void canVoidPaymentOfAlreadyCancelledMembershipEvenWithLaterRenewal() throws Exception {
        Session owner = registerGym("Gym Anula Cancelada", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Mia", uniqueDni());
        openCash(owner);
        String first = assign(owner, member, plan).andReturn().getResponse().getContentAsString();
        long firstMembership = ((Number) JsonPath.read(first, "$.id")).longValue();
        long firstPayment = ((Number) JsonPath.read(first, "$.payment.id")).longValue();
        assign(owner, member, plan).andExpect(status().isCreated());
        postJson("/api/memberships/" + firstMembership + "/cancel", "", owner.accessToken()).andExpect(status().isOk());

        postJson("/api/payments/" + firstPayment + "/void", """
                {"reason":"cobro por error"}""", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voided", is(true)));
    }

    @Test
    void retryWithoutPriceDoesNotReplayADiscountedSale() throws Exception {
        Session owner = registerGym("Gym Descuento Clave", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Nico", uniqueDni());
        openCash(owner);
        UUID key = UUID.randomUUID();
        postJson("/api/members/" + member + "/memberships", """
                {"planId":%d,"price":80,"paymentMethod":"CASH","idempotencyKey":"%s"}""".formatted(plan, key),
                owner.accessToken()).andExpect(status().isCreated());

        // mismo reintento con descuento → la venta original; sin precio (= precio del plan) → otra venta distinta
        postJson("/api/members/" + member + "/memberships", """
                {"planId":%d,"price":80.00,"paymentMethod":"CASH","idempotencyKey":"%s"}""".formatted(plan, key),
                owner.accessToken()).andExpect(status().isCreated());
        sell(owner, member, plan, "CASH", key).andExpect(status().isConflict());
    }

    @Test
    void amountsAreCapped() throws Exception {
        Session owner = registerGym("Gym Tope", uniqueEmail("owner"));
        openCash(owner, "99999999.99").andExpect(status().isBadRequest());
    }

    @Test
    void cashAndPaymentsAreIsolatedBetweenGyms() throws Exception {
        Session gymA = registerGym("Gym Caja A", uniqueEmail("a"));
        long plan = createPlan(gymA, "Mensual", 30, "100");
        openCash(gymA);
        String sale = assign(gymA, createMember(gymA, "Hugo", uniqueDni()), plan).andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(sale, "$.payment.id")).longValue();
        String current = getWithToken("/api/cash/current", gymA.accessToken()).andReturn().getResponse().getContentAsString();
        long sessionId = ((Number) JsonPath.read(current, "$.current.session.id")).longValue();

        Session gymB = registerGym("Gym Caja B", uniqueEmail("b"));
        openCash(gymB);
        getWithToken("/api/cash/current", gymB.accessToken()).andExpect(jsonPath("$.current.payments", hasSize(0)));
        getWithToken("/api/cash/sessions/" + sessionId, gymB.accessToken()).andExpect(status().isNotFound());
        postJson("/api/payments/" + paymentId + "/void", """
                {"reason":"robo"}""", gymB.accessToken()).andExpect(status().isNotFound());
    }

    @Test
    void concurrentSaleAndCloseNeverLoseAPayment() throws Exception {
        Session owner = registerGym("Gym Carrera Caja", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        long member = createMember(owner, "Iris", uniqueDni());
        openCash(owner);

        Callable<Integer> saleCall = () -> assign(owner, member, plan).andReturn().getResponse().getStatus();
        Callable<Integer> closeCall = () -> postJson("/api/cash/close", """
                {"countedCash":0}""", owner.accessToken()).andReturn().getResponse().getStatus();
        int saleStatus;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Integer> s = pool.submit(saleCall);
            Future<Integer> c = pool.submit(closeCall);
            saleStatus = s.get();
            assertThat(c.get()).isEqualTo(200);
        }

        String history = getWithToken("/api/cash/sessions", owner.accessToken()).andReturn().getResponse().getContentAsString();
        int paymentsInClosedCash = JsonPath.read(history, "$.items[0].totals.count");
        if (saleStatus == 201) {
            // la venta entró antes del cierre: está en la caja cerrada y el arqueo la incluye
            assertThat(paymentsInClosedCash).isEqualTo(1);
            assertThat((Double) JsonPath.read(history, "$.items[0].expectedCash")).isEqualTo(100.0);
        } else {
            assertThat(saleStatus).isEqualTo(409);
            assertThat(paymentsInClosedCash).isZero();
        }
    }
}
