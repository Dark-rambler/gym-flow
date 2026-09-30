package com.gymflow.shared.domain.model;

import java.util.List;
import java.util.function.Function;

/** Página de resultados sin depender de Spring Data (los ports del dominio la devuelven). page empieza en 0. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
