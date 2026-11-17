package it.unicam.cs.mpgc.rpg130573.domain.ability.spells;

import it.unicam.cs.mpgc.rpg130573.domain.model.managers.WinxStatusManager;

/**
 * Rappresenta il costo di un incantesimo.
 *
 * @param stamina       quanta stamina serve per lanciare l'incantesimo
 * @param xp            quanti xp servono per comprare l'incantesimo nello shop
 * @param unlockLevel   a quale livello si può comprare l'incantesimo
 */
public record SpellCost(int stamina, int xp, int unlockLevel) {

    public SpellCost {
        if (stamina < 0 || xp < 0 || unlockLevel < WinxStatusManager.MIN_LEVEL)
            throw new IllegalArgumentException("Costo non valido: " + this);
    }
}
