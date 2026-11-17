package it.unicam.cs.mpgc.rpg130573.domain.ability.spells;

import it.unicam.cs.mpgc.rpg130573.domain.ability.enums.CharacterAllocation;
import it.unicam.cs.mpgc.rpg130573.domain.ability.enums.SpellType;
import it.unicam.cs.mpgc.rpg130573.domain.ability.enums.TargetType;

import java.util.Objects;

/**
 * Rappresenta le caratteristiche base di un incantesimo che costituiscono
 * il suo profilo
 *
 * @param id            id univoco dell'incantesimo
 * @param name          nome dell'incantesimo (mostrato nella ui)
 * @param allocation    a quale personaggio appartiene l'incantesimo
 * @param targetType    il tipo di bersaglio che accetta l'incantesimo
 * @param spellType     il tipo di incantesimo
 */
public record SpellProfile(String id, String name, CharacterAllocation allocation,
                           TargetType targetType, SpellType spellType) {

    public SpellProfile {
        Objects.requireNonNull(name);
        Objects.requireNonNull(allocation);
        Objects.requireNonNull(targetType);
        Objects.requireNonNull(spellType);
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("L'id dell'incantesimo non può essere vuoto");
    }
}
