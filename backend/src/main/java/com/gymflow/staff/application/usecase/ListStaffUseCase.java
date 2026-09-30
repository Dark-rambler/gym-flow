package com.gymflow.staff.application.usecase;

import java.util.List;

import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.staff.application.dto.StaffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStaffUseCase {

    private final UserRepository users;

    // El filtro por gym lo aplica @TenantId: solo devuelve el staff del gym del JWT.
    @Transactional(readOnly = true)
    public List<StaffResponse> execute() {
        return users.findAll().stream().map(StaffResponse::of).toList();
    }
}
