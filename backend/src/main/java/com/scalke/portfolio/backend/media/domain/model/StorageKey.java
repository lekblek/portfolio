package com.scalke.portfolio.backend.media.domain.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Clé de stockage d'un média ({@code 04} §3.8) : identifiant opaque, jamais un chemin. 32 caractères
 * hexadécimaux (UUID aléatoire sans tirets) suivis de l'extension de son format, par exemple
 * {@code 3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a.webp} (D-BP). Le format interdit toute navigation dans une
 * arborescence ({@code /}, {@code \}, {@code ..}) ; imprévisible, la clé sert aussi d'adresse publique.
 */
public record StorageKey(String value) {

    private static final Pattern FORMAT = Pattern.compile("[0-9a-f]{32}\\.("
        + Arrays.stream(MediaFormat.values()).map(MediaFormat::extension).collect(Collectors.joining("|"))
        + ")");

    public StorageKey {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid storage key: \"" + value + "\"");
        }
    }

    /**
     * Pour une valeur non fiable (chemin d'URL) : vide si elle ne peut pas être une clé, ce qui permet de
     * répondre « introuvable » sans accéder au stockage.
     */
    public static Optional<StorageKey> parse(String candidate) {
        return candidate != null && FORMAT.matcher(candidate).matches()
            ? Optional.of(new StorageKey(candidate))
            : Optional.empty();
    }

    public MediaFormat format() {
        String extension = value.substring(value.lastIndexOf('.') + 1);
        return MediaFormat.fromExtension(extension).orElseThrow();
    }

    @Override
    public String toString() {
        return value;
    }
}
