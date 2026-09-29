package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.domain.port.MediaStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aligne les fichiers sur l'issue de la transaction du catalogue (D-DE), sans transaction distribuée : PostgreSQL
 * décide, le système de fichiers suit.
 * <ul>
 *   <li>un fichier écrit par une transaction **annulée** est supprimé (compensation) : aucun fichier orphelin, qui
 *       resterait lisible par sa clé ;</li>
 *   <li>un fichier n'est supprimé qu'**après validation** de la suppression de sa ligne : une suppression annulée
 *       garde son fichier, jamais de ligne sans fichier.</li>
 * </ul>
 * Une action différée qui échoue est journalisée, jamais propagée (la transaction est déjà terminée) : il reste au pire
 * un fichier orphelin, sans effet sur le catalogue. Une issue inconnue du commit ne supprime rien. Un arrêt brutal entre
 * l'écriture et la fin de la transaction peut aussi laisser un orphelin (rapprochement : reporté).
 */
@Component
@RequiredArgsConstructor
@Slf4j
class MediaFileLifecycle {

    private final MediaStorage mediaStorage;

    void discardIfRolledBack(StorageKey key) {
        register(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    delete(key, "transaction annulée après l'écriture");
                }
            }
        });
    }

    void deleteAfterCommit(StorageKey key) {
        register(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete(key, "suppression validée");
            }
        });
    }

    /**
     * Hors transaction, Spring refuse l'inscription ({@code IllegalStateException}) : l'action ne peut pas être perdue.
     */
    private static void register(TransactionSynchronization synchronization) {
        TransactionSynchronizationManager.registerSynchronization(synchronization);
    }

    private void delete(StorageKey key, String reason) {
        try {
            mediaStorage.delete(key);
        } catch (RuntimeException e) {
            log.warn("Fichier de média {} non supprimé ({}) : orphelin à retirer", key.value(), reason, e);
        }
    }
}
