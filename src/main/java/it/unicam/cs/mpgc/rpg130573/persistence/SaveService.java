package it.unicam.cs.mpgc.rpg130573.persistence;

import it.unicam.cs.mpgc.rpg130573.application.session.GameSession;
import it.unicam.cs.mpgc.rpg130573.application.storage.GameStorage;
import it.unicam.cs.mpgc.rpg130573.application.storage.PersistenceException;

import java.util.Optional;

/**
 * Implementazione di {@link GameStorage} che conserva le partite tramite un {@link SaveRepository}.
 * <p>
 * Fa da ponte tra i due strati: usa il {@link GameSessionMapper} per convertire una
 * {@link GameSession} in {@link GameSnapshot} (e viceversa) e delega al repository la
 * scrittura e la lettura vere e proprie. Non conosce né il formato né il luogo di salvataggio.
 */
public class SaveService implements GameStorage {
    private final SaveRepository repository;
    private final GameSessionMapper mapper;

    /**
     * @param repository il supporto su cui salvare gli snapshot
     * @param mapper     il traduttore tra partita e snapshot
     */
    public SaveService(SaveRepository repository, GameSessionMapper mapper){
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Salva la partita. È consentito solo quando non c'è una battaglia in corso,
     * perché lo snapshot non contiene lo stato del combattimento.
     *
     * @param session la partita da salvare
     * @throws IllegalStateException se c'è una battaglia in corso
     * @throws PersistenceException  se la scrittura non riesce
     */
    public void save(GameSession session){
        if(session.hasActiveBattle())
            throw new IllegalStateException("Non si può salvare durante una battaglia");
        repository.save(mapper.toSnapshot(session));
    }

    /**
     * Ricostruisce la partita dal salvataggio.
     *
     * @return la partita salvata, vuoto se non esiste alcun salvataggio
     * @throws PersistenceException se il salvataggio non è leggibile o contiene dati non validi
     */
    public Optional<GameSession> load(){
        return repository.load().map(mapper::fromSnapshot);
    }

    /** @return true se esiste un salvataggio */
    public boolean hasSave() {
        return repository.exists();
    }
}
