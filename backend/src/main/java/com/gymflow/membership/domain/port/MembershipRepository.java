package com.gymflow.membership.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.gymflow.membership.domain.model.Membership;

/** Consultas filtradas por el gym actual (@TenantId). */
public interface MembershipRepository {

    Membership save(Membership membership);

    Optional<Membership> findById(Long id);

    /** Solo el socio dueño de la membresía, sin cargar la entidad en la sesión. */
    Optional<Long> findMemberIdOf(Long membershipId);

    /** Historial del socio, la más reciente primero. */
    List<Membership> findByMember(Long memberId);

    /** Membresías de varios socios en una sola consulta (listados paginados). */
    List<Membership> findByMembers(Collection<Long> memberIds);

    List<Membership> findByIds(Collection<Long> ids);
}
