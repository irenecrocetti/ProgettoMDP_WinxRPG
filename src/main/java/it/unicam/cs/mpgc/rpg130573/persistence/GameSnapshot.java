package it.unicam.cs.mpgc.rpg130573.persistence;

import java.util.List;

/**
 * Dati persistenti di una partita nei momenti in cui è salvabile (nessuna battaglia in corso).
 * <p>
 * È un record immutabile e privo di logica, pensato per essere serializzato da un
 * {@link SaveRepository}. Contiene solo ciò che non si può ricalcolare: tutto il resto
 * (statistiche di base, incantesimi iniziali, livelli del gioco) viene ricreato
 * al caricamento dai contenuti del gioco.
 *
 * @param version             versione del formato di salvataggio, confrontata con
 *                            {@link #CURRENT_VERSION} al caricamento per rifiutare i
 *                            salvataggi incompatibili
 * @param highestClearedLevel id del livello più alto superato almeno una volta (0 se nessuno)
 * @param winxes              i dati di ciascuna Winx del gruppo
 */
public record GameSnapshot(int version,
                           int highestClearedLevel,
                           List<WinxSnapshot> winxes) {
    /**
     * Versione corrente del formato di salvataggio. Va incrementata quando la struttura
     * dei dati cambia in modo incompatibile con i salvataggi precedenti.
     */
    public static final int CURRENT_VERSION = 1;
}
