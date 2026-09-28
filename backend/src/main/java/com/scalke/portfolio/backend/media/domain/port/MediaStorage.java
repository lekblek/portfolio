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
 * {@code OpenMediaFileUseCase}, {@code store} par {@code UploadMediaUseCase}. La suppression arrive avec ses
 * appelants, la compensation d'un envoi et la suppression contrôlée d'un média du catalogue (étape 27, D-BT).
 */
public interface MediaStorage {

    /**
     * Contenu stocké sous cette clé ; vide s'il n'existe pas. L'appelant ferme le flux.
     */
    Optional<MediaContent> open(StorageKey key);

    /**
     * Enregistre {@code content} sous une clé encore inutilisée ; échoue si elle existe déjà. Le fichier n'est
     * lisible sous sa clé qu'une fois entièrement écrit. Le contenu est borné par l'appelant (10 Mio au plus).
     */
    void store(StorageKey key, byte[] content);
}
