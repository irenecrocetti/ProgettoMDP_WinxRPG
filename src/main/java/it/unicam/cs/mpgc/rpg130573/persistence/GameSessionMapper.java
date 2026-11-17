package it.unicam.cs.mpgc.rpg130573.persistence;

import it.unicam.cs.mpgc.rpg130573.application.session.GameSession;
import it.unicam.cs.mpgc.rpg130573.application.session.GameSessionFactory;
import it.unicam.cs.mpgc.rpg130573.application.storage.PersistenceException;
import it.unicam.cs.mpgc.rpg130573.domain.ability.enums.CharacterAllocation;
import it.unicam.cs.mpgc.rpg130573.domain.ability.spells.Spell;
import it.unicam.cs.mpgc.rpg130573.domain.ability.spells.WinxSpell;
import it.unicam.cs.mpgc.rpg130573.domain.model.managers.WinxStatusManager;
import it.unicam.cs.mpgc.rpg130573.domain.model.winx.Winx;

import java.util.*;

/**
 * Traduce una {@link GameSession} in {@link GameSnapshot} e viceversa.
 * <p>
 * È l'unico punto che conosce sia il dominio sia il formato dei dati persistenti. In salvataggio
 * estrae dalla partita solo ciò che non è ricalcolabile; in caricamento ricostruisce la partita
 * partendo dai contenuti del gioco ({@link GameSessionFactory}) e riapplicando i progressi salvati.
 * <p>
 * Poiché un file di salvataggio può essere corrotto o modificato a mano, il caricamento convalida
 * ogni dato prima di usarlo e segnala qualsiasi incoerenza con una {@link PersistenceException}.
 */
public class GameSessionMapper {
    private final GameSessionFactory factory;

    /**
     * @param factory la factory che conosce i contenuti del gioco (Winx, incantesimi, livelli)
     * @throws NullPointerException se la factory è null
     */
    public GameSessionMapper(GameSessionFactory factory){
        this.factory = Objects.requireNonNull(factory);
    }

    /**
     * Crea la fotografia della partita. Le Winx sono salvate nell'ordine in cui
     * compaiono nella sessione.
     *
     * @param session la partita da salvare
     * @return lo snapshot corrispondente, nella versione corrente del formato
     */
    public GameSnapshot toSnapshot(GameSession session){
        List<WinxSnapshot> winxes = session.getWinxes().stream()
                .map(this::toSnapshot).toList();
        return new GameSnapshot(GameSnapshot.CURRENT_VERSION,
                session.getHighestClearedLevel(), winxes);
    }

    /**
     * Fotografa una singola Winx. Degli incantesimi vengono salvati solo quelli del catalogo dello
     * shop: quelli iniziali sono già nella Winx appena creata dalla factory.
     */
    private WinxSnapshot toSnapshot(Winx winx){
        List<String> purchased = winx.getSpells().stream()
                .map(Spell::getId)
                .filter(id -> factory.findSpell(id).isPresent())
                .toList();
        return new WinxSnapshot(winx.getCharacterAllocation(),
                winx.getLevel(), winx.getXp(), purchased);
    }

    /**
     * Ricostruisce la partita a partire da uno snapshot.
     * <p>
     * Verifica la versione del formato, che il gruppo salvato coincida con quello del gioco e che ogni
     * Winx sia valida. Le Winx ricostruite sono riportate a HP e stamina massimi e seguono l'ordine di
     * turno definito dalla factory, non quello del file.
     *
     * @param snapshot              i dati letti dal supporto
     * @return                      la partita ripristinata
     * @throws PersistenceException se la versione non è supportata o i dati sono mancanti, incoerenti
     *                              o non validi
     */
    public GameSession fromSnapshot(GameSnapshot snapshot){
        if(snapshot.version() != GameSnapshot.CURRENT_VERSION)
            throw new PersistenceException("Versione di salvataggio non supportata: " + snapshot.version());
        if(snapshot.winxes() == null)
            throw new PersistenceException("Nel salvataggio mancano le Winx");

        Map<CharacterAllocation, WinxSnapshot> byCharacter = indexByCharacter(snapshot.winxes());
        requireExpectedParty(byCharacter.keySet());

        List<Winx> winxes = factory.getParty().stream()
                .map(character -> restore(byCharacter.get(character)))
                .toList();
        try {
             return factory.restoreGame(winxes, snapshot.highestClearedLevel());
        } catch (IllegalArgumentException e) {
            throw new PersistenceException("Salvataggio non valido: " + e.getMessage(), e);
        }
    }

    /**
     * Indicizza gli snapshot per personaggio, rifiutando elementi nulli, senza personaggio o duplicati.
     */
    private static Map<CharacterAllocation, WinxSnapshot> indexByCharacter(List<WinxSnapshot> snapshots){
        Map<CharacterAllocation, WinxSnapshot> result = new EnumMap<>(CharacterAllocation.class);
        for (WinxSnapshot s : snapshots) {
            if (s == null || s.character() == null)
                throw new PersistenceException("Nel salvataggio c'è una Winx senza personaggio");
            if (result.put(s.character(), s) != null)
                throw new PersistenceException("Winx duplicata nel salvataggio: " + s.character());
        }
        return result;
    }

    /**
     * Controlla che i personaggi salvati siano esattamente quelli del gruppo del gioco,
     * né di più né di meno.
     */
    private void requireExpectedParty(Set<CharacterAllocation> found){
        if (!found.equals(Set.copyOf(factory.getParty())))
            throw new PersistenceException("Le Winx del salvataggio " + found
            + " non corrispondono a quelle del gioco " + factory.getParty());
    }

    private Winx restore(WinxSnapshot s){
        String who = s.character().name();
        if (s.level() < WinxStatusManager.MIN_LEVEL || s.level() > WinxStatusManager.MAX_LEVEL)
            throw new PersistenceException(who + ": il livello non valido: " + s.level());
        if (s.xp() < 0)
            throw new PersistenceException(who + ": XP non validi: " + s.xp());
        if (s.purchasedSpellsIds() == null)
            throw new PersistenceException(who + ": mancano gli incantesimi acquistati");

        Winx winx = factory.createWinx(s.character());
        for (int i = WinxStatusManager.MIN_LEVEL; i < s.level(); i++){
            winx.levelUp();
        }
        winx.getStatusManager().gainXP(s.xp());
        learnPurchasedSpells(winx, s);
        winx.restore();
        return winx;
    }

    /**
     * Ricostruisce una Winx: parte da quella di base, la porta al livello salvato, le restituisce gli
     * XP e le fa imparare gli incantesimi acquistati. Infine la riporta a HP e stamina massimi.
     *
     * @throws PersistenceException se livello, XP o incantesimi non sono validi
     */
    private void learnPurchasedSpells(Winx winx, WinxSnapshot s){
        Set<String> seen = new HashSet<>();
        for (String id : s.purchasedSpellsIds()) {
            if (!seen.add(id))
                throw new PersistenceException(winx.getName() + ": incantesimo duplicato: " + id);
            WinxSpell spell = factory.findSpell(id).orElseThrow(
                    () -> new PersistenceException("Incantesimo sconosciuto: " + id));
            if (spell.getCharacterAllocation() != winx.getCharacterAllocation())
                throw new PersistenceException(id + " non appartiene a " + winx.getName());
            if (spell.getUnlockLevel() > s.level())
                throw new PersistenceException(winx.getName() + " non può avere " + id
                        + ": richiede livello " + spell.getUnlockLevel());
            winx.learnSpell(spell);
        }
    }
}
