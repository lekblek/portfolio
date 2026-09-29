package com.scalke.portfolio.backend.media.application.usecase;

import com.scalke.portfolio.backend.media.InMemoryMediaStorage;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * D-DE : chaque issue de transaction, rejouée à la main sur la synchronisation de Spring (le déroulé réel avec
 * PostgreSQL est couvert par {@code MediaFileConsistencyIT}).
 */
@ExtendWith(OutputCaptureExtension.class)
class MediaFileLifecycleTest {

    private static final StorageKey KEY = StorageKey.random(MediaFormat.PNG);

    private final InMemoryMediaStorage storage = new InMemoryMediaStorage();
    private final MediaFileLifecycle lifecycle = new MediaFileLifecycle(storage);

    @AfterEach
    void closeTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void discards_a_written_file_only_when_the_transaction_rolls_back() {
        assertThat(afterCompletion(lifecycle::discardIfRolledBack,
            TransactionSynchronization.STATUS_COMMITTED)).isTrue();
        assertThat(afterCompletion(lifecycle::discardIfRolledBack,
            TransactionSynchronization.STATUS_UNKNOWN)).isTrue();
        assertThat(afterCompletion(lifecycle::discardIfRolledBack,
            TransactionSynchronization.STATUS_ROLLED_BACK)).isFalse();
    }

    @Test
    void deletes_a_file_only_after_the_commit() {
        assertThat(afterCompletion(lifecycle::deleteAfterCommit,
            TransactionSynchronization.STATUS_ROLLED_BACK)).isTrue();
        assertThat(afterCompletion(lifecycle::deleteAfterCommit,
            TransactionSynchronization.STATUS_COMMITTED)).isFalse();
    }

    /**
     * La transaction est déjà terminée : un échec de suppression laisse un orphelin signalé, sans exception.
     */
    @Test
    void logs_a_file_that_could_not_be_deleted(CapturedOutput output) {
        InMemoryMediaStorage failing = new InMemoryMediaStorage() {
            @Override
            public void delete(StorageKey key) {
                throw new IllegalStateException("disque en lecture seule");
            }
        };
        TransactionSynchronizationManager.initSynchronization();
        new MediaFileLifecycle(failing).deleteAfterCommit(KEY);

        assertThatNoException().isThrownBy(
            () -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
        assertThat(output).contains("Fichier de média " + KEY.value() + " non supprimé (suppression validée)");
    }

    /**
     * Hors transaction, aucune issue ne viendrait jamais : l'appel est refusé plutôt que de perdre l'action.
     */
    @Test
    void requires_a_transaction() {
        assertThatThrownBy(() -> lifecycle.deleteAfterCommit(KEY)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> lifecycle.discardIfRolledBack(KEY)).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Enregistre l'action sur un fichier présent, rejoue l'issue {@code status} comme le gestionnaire de transactions,
     * et indique si le fichier existe encore.
     */
    private boolean afterCompletion(Consumer<StorageKey> register, int status) {
        StorageKey key = StorageKey.random(MediaFormat.PNG);
        storage.store(key, new byte[] {1});
        TransactionSynchronizationManager.initSynchronization();
        try {
            register.accept(key);
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                synchronizations.forEach(TransactionSynchronization::afterCommit);
            }
            synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        return storage.files().containsKey(key);
    }
}
