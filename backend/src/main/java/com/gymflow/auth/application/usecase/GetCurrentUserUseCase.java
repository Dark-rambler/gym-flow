package com.gymflow.auth.application.usecase;

import com.gymflow.auth.application.dto.MeResponse;
import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.gym.domain.port.GymRepository;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.domain.model.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserRepository users;
    private final GymRepository gyms;

    @Transactional(readOnly = true)
    public MeResponse execute(Actor actor) {
        var user = users.findById(actor.userId()).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        var gym = gyms.findById(user.gymId()).orElseThrow(() -> new NotFoundException("Gimnasio no encontrado"));
        return MeResponse.of(user, gym);
    }
}
