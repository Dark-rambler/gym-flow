package com.gymflow.member.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import com.gymflow.checkin.domain.model.CheckInCode;
import com.gymflow.gym.application.GymCalendar;
import com.gymflow.gym.domain.port.GymRepository;
import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.domain.model.Memberships;
import com.gymflow.membership.domain.port.MembershipRepository;
import com.gymflow.membership.domain.model.MembershipStatus;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** QR del socio: consulta, regeneración y carnet público (sin login) para mostrarlo desde el celular. */
@Service
@RequiredArgsConstructor
public class MemberQrUseCases {

    private final MemberRepository members;
    private final MembershipRepository memberships;
    private final GymRepository gyms;
    private final GymCalendar calendar;
    private final TransactionTemplate tx;

    /** payload es lo que se codifica en el QR y lo que lee recepción. */
    public record MemberQrResponse(UUID qrToken, String payload) {

        static MemberQrResponse of(Member m) {
            return new MemberQrResponse(m.qrToken(), CheckInCode.payloadFor(m.qrToken()));
        }
    }

    /** Solo lo necesario para mostrar el carnet: sin DNI, teléfono ni ids internos. */
    public record PublicMemberCardResponse(
            String gymName,
            String memberName,
            String payload,
            @Schema(nullable = true) String planName,
            @Schema(nullable = true) LocalDate endDate,
            @Schema(nullable = true) MembershipStatus status) {
    }

    @Transactional(readOnly = true)
    public MemberQrResponse get(Long memberId) {
        return MemberQrResponse.of(members.findById(memberId).orElseThrow(() -> new NotFoundException("Socio no encontrado")));
    }

    @Transactional
    public MemberQrResponse rotate(Long memberId) {
        Member member = members.lockById(memberId).orElseThrow(() -> new NotFoundException("Socio no encontrado"));
        return MemberQrResponse.of(members.save(member.rotateQr()));
    }

    /**
     * Carnet público: el token del enlace ES la credencial (UUID aleatorio, no enumerable). Corre como sistema porque
     * no hay JWT; solo busca por token exacto y devuelve datos mínimos. Token desconocido o regenerado → 404.
     */
    public PublicMemberCardResponse publicCard(UUID token) {
        return TenantContext.callAsSystem(() -> tx.execute(status -> {
            Member member = members.findByQrToken(token)
                    .filter(Member::active)
                    .orElseThrow(() -> new NotFoundException("Enlace no válido"));
            var gym = gyms.findById(member.gymId()).orElseThrow(() -> new NotFoundException("Enlace no válido"));
            LocalDate today = calendar.today(gym.id());
            var current = Memberships.current(memberships.findByMember(member.id()), today);
            return new PublicMemberCardResponse(gym.name(), member.fullName(), CheckInCode.payloadFor(member.qrToken()),
                    current.map(m -> m.planName()).orElse(null), current.map(m -> m.endDate()).orElse(null),
                    current.map(m -> m.statusOn(today)).orElse(null));
        }));
    }
}
