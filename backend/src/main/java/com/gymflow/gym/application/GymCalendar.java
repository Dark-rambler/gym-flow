package com.gymflow.gym.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.gymflow.gym.domain.port.GymRepository;
import com.gymflow.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** "Hoy" en la zona horaria del gimnasio (un gym de Lima no cambia de día a las 19:00 por estar en UTC). */
@Component
@RequiredArgsConstructor
public class GymCalendar {

    private final GymRepository gyms;
    private final Clock clock;

    public LocalDate today(Long gymId) {
        return LocalDate.now(clock.withZone(zone(gymId)));
    }

    public ZoneId zone(Long gymId) {
        var gym = gyms.findById(gymId).orElseThrow(() -> new NotFoundException("Gimnasio no encontrado"));
        return ZoneId.of(gym.timezone());
    }

    public Instant now() {
        return clock.instant();
    }
}
