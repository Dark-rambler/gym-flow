package com.gymflow.gym.domain.model;

import java.text.Normalizer;
import java.util.Locale;

// "Gym Fuerza Perú" → "gym-fuerza-peru". Longitud máxima 50 para dejar espacio al sufijo de desempate.
public final class Slugs {

    private static final int MAX_LENGTH = 50;

    private Slugs() {
    }

    public static String of(String text) {
        String slug = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "gym" : slug;
    }
}
