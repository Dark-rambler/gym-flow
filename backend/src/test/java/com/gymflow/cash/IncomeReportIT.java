package com.gymflow.cash;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.gymflow.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

// "Hoy" = 2026-03-10 10:00 en Lima (TestClockConfiguration).
class IncomeReportIT extends ApiTestSupport {

    @Test
    void groupsValidPaymentsByGymLocalDayAndMethod() throws Exception {
        Session owner = registerGym("Gym Reporte", uniqueEmail("owner"));
        long plan = createPlan(owner, "Mensual", 30, "100");
        openCash(owner);
        sell(owner, createMember(owner, "Ana", uniqueDni()), plan, "CASH", UUID.randomUUID());
        sell(owner, createMember(owner, "Beto", uniqueDni()), plan, "YAPE", UUID.randomUUID());

        clock.advanceDays(1);
        sell(owner, createMember(owner, "Carla", uniqueDni()), plan, "CARD", UUID.randomUUID());

        getWithToken("/api/reports/income?from=2026-03-10&to=2026-03-12", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.total", is(300.0)))
                .andExpect(jsonPath("$.totals.cash", is(100.0)))
                .andExpect(jsonPath("$.totals.yape", is(100.0)))
                .andExpect(jsonPath("$.totals.card", is(100.0)))
                .andExpect(jsonPath("$.days", hasSize(3)))
                .andExpect(jsonPath("$.days[0].date", is("2026-03-10")))
                .andExpect(jsonPath("$.days[0].total", is(200.0)))
                .andExpect(jsonPath("$.days[1].count", is(1)))
                .andExpect(jsonPath("$.days[2].total", is(0.0)));
    }

    @Test
    void invalidRangeGives400AndReceptionist403() throws Exception {
        Session owner = registerGym("Gym Rango", uniqueEmail("owner"));
        getWithToken("/api/reports/income?from=2026-03-10&to=2026-03-01", owner.accessToken())
                .andExpect(status().isBadRequest());
        getWithToken("/api/reports/income?from=2025-01-01&to=2026-03-01", owner.accessToken())
                .andExpect(status().isBadRequest());
        getWithToken("/api/reports/income?from=2026-03-01", owner.accessToken())
                .andExpect(status().isBadRequest());

        String email = uniqueEmail("recep");
        createStaff(owner, email, "RECEPTIONIST");
        getWithToken("/api/reports/income?from=2026-03-01&to=2026-03-10", login(email, PASSWORD).accessToken())
                .andExpect(status().isForbidden());
    }
}
