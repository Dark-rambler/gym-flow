package com.gymflow.staff;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

class StaffIT extends ApiTestSupport {

    @Test
    void ownerCreatesReceptionistAndListsStaff() throws Exception {
        Session owner = registerGym("Gym Staff", uniqueEmail("owner"));
        String email = uniqueEmail("recep");

        createStaff(owner, email, "RECEPTIONIST");

        getWithToken("/api/staff", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", hasItem(email)));
    }

    @Test
    void receptionistCannotManageStaff() throws Exception {
        Session owner = registerGym("Gym Roles", uniqueEmail("owner"));
        String email = uniqueEmail("recep");
        createStaff(owner, email, "RECEPTIONIST");
        Session recep = login(email, PASSWORD);

        getWithToken("/api/staff", recep.accessToken()).andExpect(status().isForbidden());
        postJson("/api/staff", """
                {"fullName":"X","email":"%s","password":"%s","role":"RECEPTIONIST"}""".formatted(uniqueEmail("x"), PASSWORD),
                recep.accessToken())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
        getWithToken("/api/me", recep.accessToken()).andExpect(status().isOk());
    }

    @Test
    void adminOnlyManagesReceptionists() throws Exception {
        Session owner = registerGym("Gym Admin", uniqueEmail("owner"));
        String adminEmail = uniqueEmail("admin");
        createStaff(owner, adminEmail, "ADMIN");
        long otherAdminId = createStaff(owner, uniqueEmail("admin2"), "ADMIN");
        Session admin = login(adminEmail, PASSWORD);

        // puede crear y desactivar recepcionistas
        long recepId = createStaff(admin, uniqueEmail("recep"), "RECEPTIONIST");
        patchJson("/api/staff/" + recepId, """
                {"active":false}""", admin.accessToken()).andExpect(status().isOk());

        // no puede crear ADMIN/OWNER, ascender, ni tocar a otros ADMIN u OWNER
        for (String role : new String[] {"ADMIN", "OWNER"}) {
            postJson("/api/staff", """
                    {"fullName":"X","email":"%s","password":"%s","role":"%s"}""".formatted(uniqueEmail("x"), PASSWORD, role),
                    admin.accessToken()).andExpect(status().isForbidden());
        }
        patchJson("/api/staff/" + recepId, """
                {"role":"ADMIN"}""", admin.accessToken()).andExpect(status().isForbidden());
        patchJson("/api/staff/" + otherAdminId, """
                {"active":false}""", admin.accessToken()).andExpect(status().isForbidden());
        patchJson("/api/staff/" + owner.userId(), """
                {"active":false}""", admin.accessToken()).andExpect(status().isForbidden());
    }

    @Test
    void staffIsIsolatedBetweenGyms() throws Exception {
        Session gymA = registerGym("Gym A", uniqueEmail("owner-a"));
        String staffA = uniqueEmail("staff-a");
        long staffAId = createStaff(gymA, staffA, "RECEPTIONIST");
        Session gymB = registerGym("Gym B", uniqueEmail("owner-b"));

        getWithToken("/api/staff", gymB.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].email", not(hasItem(staffA))));

        // un id de otro gym se comporta como inexistente
        patchJson("/api/staff/" + staffAId, """
                {"active":false}""", gymB.accessToken()).andExpect(status().isNotFound());
        login(staffA, PASSWORD); // sigue activo
    }

    @Test
    void lastOwnerCannotBeDemotedOrDeactivateSelf() throws Exception {
        Session owner = registerGym("Gym Owner", uniqueEmail("owner"));

        patchJson("/api/staff/" + owner.userId(), """
                {"active":false}""", owner.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("No puedes desactivar tu propia cuenta")));
        patchJson("/api/staff/" + owner.userId(), """
                {"role":"ADMIN"}""", owner.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("El gimnasio debe tener al menos un OWNER activo")));
    }

    @Test
    void deactivatedUserCannotLoginOrRefresh() throws Exception {
        Session owner = registerGym("Gym Baja", uniqueEmail("owner"));
        String email = uniqueEmail("recep");
        long id = createStaff(owner, email, "RECEPTIONIST");
        Session recep = login(email, PASSWORD);

        patchJson("/api/staff/" + id, """
                {"active":false}""", owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        postJson("/api/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, PASSWORD)).andExpect(status().isUnauthorized());
        postJson("/api/auth/refresh", """
                {"refreshToken":"%s"}""".formatted(recep.refreshToken())).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateStaffEmailGives409() throws Exception {
        String ownerEmail = uniqueEmail("owner");
        Session owner = registerGym("Gym Dup", ownerEmail);

        postJson("/api/staff", """
                {"fullName":"X","email":"%s","password":"%s","role":"ADMIN"}""".formatted(ownerEmail, PASSWORD),
                owner.accessToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("El email ya está registrado")));
    }
}
