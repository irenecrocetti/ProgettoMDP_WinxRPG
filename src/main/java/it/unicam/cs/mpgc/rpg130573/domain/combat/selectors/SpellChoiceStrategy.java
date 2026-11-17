package it.unicam.cs.mpgc.rpg130573.domain.combat.selectors;

import it.unicam.cs.mpgc.rpg130573.domain.ability.spells.Spell;
import it.unicam.cs.mpgc.rpg130573.domain.model.villains.Villain;

/**
 * Interfaccia funzionale per la scelta dell'incantesimo. Le classi che
 * la implementano forniscono la logica per la scelta dell'incantesimo da
 * parte dei nemici.
 */
@FunctionalInterface
public interface SpellChoiceStrategy {
    Spell chooseSpell(Villain villain);
}
