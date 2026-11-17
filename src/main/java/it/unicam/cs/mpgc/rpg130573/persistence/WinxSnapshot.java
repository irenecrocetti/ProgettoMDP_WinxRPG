package it.unicam.cs.mpgc.rpg130573.persistence;

import it.unicam.cs.mpgc.rpg130573.domain.ability.enums.CharacterAllocation;

import java.util.List;

/**
 * Dati persistenti di una Winx, tutto ciò che non è ricalcolabile.
 * <p>
 * Le statistiche che dipendono dal livello (HP e stamina massimi) e gli incantesimi
 * iniziali non sono salvati: vengono ricostruiti al caricamento a partire dalla Winx
 * di base.
 *
 * @param character           il personaggio a cui si riferiscono i dati
 * @param level               il livello raggiunto
 * @param xp                  gli XP attualmente posseduti (quelli non ancora spesi nello shop)
 * @param purchasedSpellsIds  gli id degli incantesimi acquistati nello shop
 */
public record WinxSnapshot(CharacterAllocation character,
                           int level,
                           int xp,
                           List<String> purchasedSpellsIds) {}