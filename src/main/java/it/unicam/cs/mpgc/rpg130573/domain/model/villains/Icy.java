package it.unicam.cs.mpgc.rpg130573.domain.model.villains;

import it.unicam.cs.mpgc.rpg130573.domain.ability.enums.CharacterAllocation;
import it.unicam.cs.mpgc.rpg130573.domain.ability.spells.StandardVillainSpell;

import static it.unicam.cs.mpgc.rpg130573.domain.ability.spells.SpellBehavior.damage;
import static it.unicam.cs.mpgc.rpg130573.domain.ability.spells.SpellBehavior.inflict;
import static it.unicam.cs.mpgc.rpg130573.domain.effect.StatusEffects.frost;

/**
 * Definisce le statistiche ed abilità di Icy
 */
public class Icy extends Villain{
    private static final int ICY_HEALTH = 100;
    private static final int ICY_ACCURACY = 70;
    private static final int ICY_XP_RELEASED = 50;

    public Icy(){
        super("Icy", ICY_HEALTH, ICY_HEALTH, ICY_ACCURACY, ICY_XP_RELEASED);
        addSpell(StandardVillainSpell.attack("IcyBaseSpell", "Frostbite", CharacterAllocation.ICY,
                damage(15).andThen(inflict(() -> frost(3,4)))));
    }
}
