package it.unicam.cs.mpgc.rpg130573.application.session;

import it.unicam.cs.mpgc.rpg130573.domain.ability.spells.WinxSpell;
import it.unicam.cs.mpgc.rpg130573.domain.combat.Battle;
import it.unicam.cs.mpgc.rpg130573.domain.level.Level;
import it.unicam.cs.mpgc.rpg130573.domain.level.LevelRegistry;
import it.unicam.cs.mpgc.rpg130573.domain.model.villains.Villain;
import it.unicam.cs.mpgc.rpg130573.domain.model.winx.Winx;
import it.unicam.cs.mpgc.rpg130573.domain.shop.PurchaseResult;
import it.unicam.cs.mpgc.rpg130573.domain.shop.SpellShop;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Una partita in corso: il gruppo di Winx, lo shop, i livelli del gioco e il progresso del giocatore.
 * <p>
 * È l'unico punto da cui si avviano e si concludono i livelli e si fanno acquisti, quindi è
 * qui che si garantiscono le regole di progressione (sblocco dei livelli, assegnazione di XP e
 * avanzamenti di livello) e che non si faccia shopping durante una battaglia. Non dipende da
 * nessuna libreria grafica.
 * <p>
 * Una nuova sessione non si crea direttamente: lo fa {@link GameSessionFactory}.
 */
public class GameSession {
    private final List<Winx> winxes;
    private final SpellShop shop;
    private final LevelRegistry registry;
    private int highestClearedLevel;

    /** Livello attualmente in gioco, null se non c'è una battaglia */
    private Level activeLevel;
    /** Battaglia attualmente in corso, null se non ce n'è una */
    private Battle activeBattle;

    /**
     * Costruttore visibile solo nel package: le sessioni le crea {@link GameSessionFactory}.
     *
     * @param winxes              le Winx del gruppo, nell'ordine di turno
     * @param shop                lo shop in cui spendere gli XP
     * @param registry            i livelli del gioco
     * @param highestClearedLevel id del livello più alto già superato (0 se nessuno)
     * @throws IllegalArgumentException se il progresso è negativo o supera il numero di livelli
     */
    GameSession(List<Winx> winxes, SpellShop shop,
                       LevelRegistry registry, int highestClearedLevel){
        if(highestClearedLevel < 0 || highestClearedLevel > registry.size())
            throw new IllegalArgumentException("Progresso non valido: " + highestClearedLevel);
        this.winxes = List.copyOf(winxes);
        this.shop = shop;
        this.registry = registry;
        this.highestClearedLevel = highestClearedLevel;
    }

    /** @return le Winx del gruppo, nell'ordine di turno (lista non modificabile) */
    public List<Winx> getWinxes() { return this.winxes; }

    /** @return tutti i livelli del gioco, in ordine di gioco */
    public List<Level> getLevels() { return registry.getLevels(); }

    /** @return l'id del livello più alto superato almeno una volta, 0 se nessuno */
    public int getHighestClearedLevel() { return this.highestClearedLevel; }

    /**
     * Un livello è sbloccato se esiste ed è già stato superato oppure è il primo
     * ancora da superare.
     *
     * @param levelId l'id del livello
     * @return true se il livello può essere giocato, false se è bloccato o non esiste
     */
    public boolean isUnlocked(int levelId){
        return registry.getLevel(levelId).isPresent()
                && levelId <= highestClearedLevel + 1;
    }

    /** @return true se tutti i livelli del gioco sono stati superati almeno una volta */
    public boolean isCompleted() { return highestClearedLevel == registry.size(); }

    /** @return true se c'è una battaglia in corso (non ancora conclusa né abbandonata) */
    public boolean hasActiveBattle() { return activeBattle != null; }

    /**
     * Avvia un livello. I nemici e gli eventi vengono creati da zero a ogni avvio e la
     * battaglia parte con le Winx a HP e stamina massimi.
     *
     * @param levelId l'id del livello da giocare
     * @return la battaglia appena avviata
     * @throws IllegalArgumentException se il livello non esiste
     * @throws IllegalStateException    se c'è già una battaglia in corso o il livello è bloccato
     */
    public Battle startLevel(int levelId){
        if(activeBattle != null)
            throw new IllegalStateException("C'è una battaglia già in corso");
        Level level = registry.getLevel(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Livello inesistente: " + levelId));
        if(!isUnlocked(levelId))
            throw new IllegalStateException("Livello non ancora sbloccato: " + levelId);

        Battle battle = new Battle(winxes, level.createVillains(),
                level.createEvents(), level::isWon);
        battle.startBattle();

        this.activeLevel = level;
        this.activeBattle = battle;
        return battle;
    }

    /**
     * Chiude la battaglia finita e ne applica le conseguenze.
     * <p>
     * In caso di sconfitta non cambia nulla. In caso di vittoria ogni Winx guadagna gli
     * XP rilasciati dai nemici presenti a fine scontro (compresi quelli comparsi dopo l'inizio).
     * Alla prima vittoria su un livello, inoltre, tutte le Winx salgono di livello, il progresso
     * avanza e si sblocca il livello successivo, se c'è. Rigiocare un livello già superato
     * dà solo gli XP.
     *
     * @return l'esito del livello, da mostrare al giocatore
     * @throws IllegalStateException se non c'è una battaglia in corso o non è ancora finita
     */
    public LevelOutcome completeLevel(){
        if(activeBattle == null)
            throw new IllegalStateException("Nessuna battaglia in corso");
        if(!activeBattle.isFinished())
            throw new IllegalStateException("La battaglia non è ancora finita");

        Battle battle = activeBattle;
        Level level = activeLevel;
        activeBattle = null;
        activeLevel = null;

        if(!battle.winxWon())
            return LevelOutcome.defeat(level.getId());

        int xp = battle.getVillains().stream().mapToInt(Villain::getXPReleased).sum();
        winxes.forEach(w -> w.getStatusManager().gainXP(xp));

        boolean firstClear = level.getId() > highestClearedLevel;
        OptionalInt unlocked = OptionalInt.empty();
        if(firstClear){
            winxes.forEach(Winx::levelUp);
            highestClearedLevel = level.getId();
            Optional<Level> next = registry.getNext(level.getId());
            if(next.isPresent()) unlocked = OptionalInt.of(next.get().getId());
        }
        return new LevelOutcome(level.getId(), true, xp, firstClear, unlocked);
    }

    /**
     * Abbandona la battaglia in corso, se c'è, senza ricompense né progressi.
     */
    public void abandonLevel(){
        activeBattle = null;
        activeLevel = null;
    }

    /**
     * @param buyer la Winx che vuole acquistare
     * @return gli incantesimi che la Winx può già acquistare (livello sufficiente, non posseduti)
     */
    public List<WinxSpell> getAvailableSpells(Winx buyer) { return shop.getAvailableSpells(buyer); }

    /**
     * @param buyer la Winx che vuole acquistare
     * @return gli incantesimi della Winx non ancora sbloccati perché il suo livello è troppo basso
     */
    public List<WinxSpell> getLockedSpells(Winx buyer) { return shop.getLockedSpells(buyer); }

    /**
     * Fa acquistare un incantesimo a una Winx, spendendo i suoi XP.
     *
     * @param buyer la Winx che acquista, deve far parte di questa partita
     * @param spell l'incantesimo da acquistare
     * @return l'esito dell'acquisto
     * @throws IllegalArgumentException se la Winx non fa parte della partita
     * @throws IllegalStateException    se c'è una battaglia in corso
     */
    public PurchaseResult buySpell(Winx buyer, WinxSpell spell){
        if(!winxes.contains(buyer))
            throw new IllegalArgumentException("La Winx non fa parte di questa partita");
        if(activeBattle != null)
            throw new IllegalStateException("Lo shop non è usabile durante una battaglia");
        return shop.buy(buyer, spell);
    }

    /**
     * @param levelId l'id del livello
     * @return lo stato del livello rispetto al progresso della partita
     * @throws IllegalArgumentException se il livello non esiste
     */
    public LevelStatus getLevelStatus(int levelId){
        if(registry.getLevel(levelId).isEmpty())
            throw new IllegalArgumentException("Livello inesistente: " + levelId);
        if(levelId <= highestClearedLevel) return LevelStatus.COMPLETED;
        if(levelId == highestClearedLevel + 1) return LevelStatus.AVAILABLE;
        return LevelStatus.LOCKED;
    }
}
