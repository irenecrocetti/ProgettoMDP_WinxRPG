package it.unicam.cs.mpgc.rpg130573.persistence;

import it.unicam.cs.mpgc.rpg130573.application.storage.PersistenceException;

import java.util.Optional;

/**
 * Astrazione del supporto su cui si salva. Non dice né dove né in che formato.
 * <p>
 * Lavora su {@link GameSnapshot}, la fotografia dei dati persistenti, e non sugli oggetti
 * di dominio: la traduzione tra i due mondi spetta a {@link GameSessionMapper}.
 * In questo modo cambiare supporto (file JSON, database, servizio remoto, ...) non
 * richiede di toccare né il dominio né la UI: basta un'altra implementazione di questa
 * interfaccia, come {@link JsonSaveRepository}.
 */
public interface SaveRepository {
    /**
     * Scrive lo snapshot sul supporto, sostituendo quello eventualmente già presente.
     *
     * @param snapshot i dati da salvare
     * @throws PersistenceException se la scrittura non riesce
     */
    void save(GameSnapshot snapshot);

    /**
     * @return lo snapshot salvato, vuoto se non esiste alcun salvataggio
     * @throws PersistenceException se il salvataggio esiste ma non è leggibile o non è valido
     */
    Optional<GameSnapshot> load();

    /** @return true se esiste un salvataggio sul supporto */
    boolean exists();

    /**
     * Elimina il salvataggio. Non fa nulla se non esiste.
     *
     * @throws PersistenceException se l'eliminazione non riesce
     */
    void delete();
}
