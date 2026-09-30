package com.gymflow.member.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gymflow.member.domain.model.Member;
import com.gymflow.shared.domain.model.PageResult;

/** Consultas filtradas por el gym actual (@TenantId). */
public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    /** Igual que findById pero con SELECT ... FOR UPDATE: serializa operaciones sobre las membresías del socio. */
    Optional<Member> lockById(Long id);

    List<Member> findByIds(Collection<Long> ids);

    Optional<Member> findByQrToken(UUID qrToken);

    /** Solo el id (sin cargar la entidad en la sesión): para bloquear con lockById ANTES de leer el socio. */
    Optional<Long> findIdByQrToken(UUID qrToken);

    /** dni ya normalizado (Member.normalizeDni). Solo el id, igual que findIdByQrToken. */
    Optional<Long> findIdByDni(String dni);

    /** Busca por nombre (contiene, sin distinguir mayúsculas) o DNI (contiene). query vacía = todos. */
    PageResult<Member> search(String query, int page, int size);
}
