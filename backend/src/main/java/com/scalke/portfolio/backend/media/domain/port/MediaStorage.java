package com.scalke.portfolio.backend.media.domain.port;

import com.scalke.portfolio.backend.media.domain.model.MediaContent;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;

import java.util.Optional;

/**
 * Stockage des fichiers de médias ({@code 04} §3.8, cahier des charges §5.8) : les appelants ne manipulent
 * que des {@link StorageKey}, jamais un chemin. Implémentation V1 locale ({@code LocalMediaStorage}) ; une
 * implémentation compatible S3 pourra la remplacer sans modifier les appelants (D-BO).
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code open} par
 * {@code OpenMediaFileUseCase}. L'écriture et la suppression arrivent avec l'envoi de fichiers (étape 26).
 */
public interface MediaStorage {

    /**
     * Contenu stocké sous cette clé ; vide s'il n'existe pas. L'appelant ferme le flux.
     */
    Optional<MediaContent> open(StorageKey key);
}
