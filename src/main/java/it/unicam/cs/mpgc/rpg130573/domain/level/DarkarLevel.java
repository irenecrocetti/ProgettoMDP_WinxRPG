package it.unicam.cs.mpgc.rpg130573.domain.level;

import it.unicam.cs.mpgc.rpg130573.domain.combat.events.BattleEvent;
import it.unicam.cs.mpgc.rpg130573.domain.combat.events.DarkarHalfLifeEvent;
import it.unicam.cs.mpgc.rpg130573.domain.model.villains.Darkar;
import it.unicam.cs.mpgc.rpg130573.domain.model.villains.Villain;

import java.util.List;

/**
 * Livello 2: le Winx combattono contro Darkar. Si vince se viene
 * sconfitto.
 */
public class DarkarLevel implements Level{
    @Override
    public int getId() {
        return 2;
    }

    @Override
    public String getName() {
        return "Darkar";
    }

    @Override
    public List<Villain> createVillains() {
        return List.of(new Darkar());
    }

    @Override
    public boolean isWon(List<Villain> villains) {
        return villains.stream().noneMatch(v -> v instanceof Darkar && v.isAlive());
    }

    /**
     * Crea l'evento che deve essere scatenato nel livello
     */
    @Override
    public List<BattleEvent> createEvents() {
        return List.of(new DarkarHalfLifeEvent());
    }
}
