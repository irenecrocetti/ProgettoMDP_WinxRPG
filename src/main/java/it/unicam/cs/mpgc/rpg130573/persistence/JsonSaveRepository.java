package it.unicam.cs.mpgc.rpg130573.persistence;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.unicam.cs.mpgc.rpg130573.application.storage.PersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * {@link SaveRepository} che salva lo snapshot in un file JSON, serializzato con Jackson.
 * <p>
 * Il salvataggio è sicuro rispetto alle interruzioni: i dati vengono scritti prima in un
 * file temporaneo accanto a quello definitivo, che poi lo sostituisce con un unico
 * spostamento atomico. Se la scrittura fallisce a metà, il salvataggio precedente
 * resta intatto.
 * <p>
 * In lettura un campo mancante nel file è considerato un errore
 * ({@code FAIL_ON_MISSING_CREATOR_PROPERTIES}), così un file incompleto non produce
 * snapshot con valori di default silenziosi.
 */
public class JsonSaveRepository implements SaveRepository{
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
    private final Path file;

    /**
     * @param file il file di salvataggio; le cartelle mancanti vengono create al primo salvataggio
     */
    public JsonSaveRepository(Path file){
        this.file = file;
    }

    /**
     * Scrive lo snapshot nel file, passando per un file temporaneo
     * (stesso nome con suffisso {@code .tmp}).
     *
     * @throws PersistenceException se la scrittura non riesce
     */
    @Override
    public void save(GameSnapshot snapshot) {
        try{
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writeValue(tmp.toFile(), snapshot);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e){
            throw new PersistenceException("Impossibile salvare la partita", e);
        }
    }

    /**
     * @return lo snapshot letto dal file, vuoto se il file non esiste
     * @throws PersistenceException se il file è vuoto, corrotto, incompleto o illeggibile
     */
    @Override
    public Optional<GameSnapshot> load() {
        if(!exists()) return Optional.empty();
        try{
            GameSnapshot snapshot = mapper.readValue(file.toFile(), GameSnapshot.class);
            if (snapshot == null)
                throw new PersistenceException("File di salvataggio vuoto");
            return Optional.of(snapshot);
        } catch (JacksonException e){
            throw new PersistenceException("File di salvataggio corrotto o non valido", e);
        } catch (IOException e){
            throw new PersistenceException("Impossibile leggere il salvataggio", e);
        }
    }


    /** @return true se il file di salvataggio esiste */
    @Override
    public boolean exists() {
        return Files.exists(file);
    }

    /**
     * Elimina il file di salvataggio, se presente.
     *
     * @throws PersistenceException se l'eliminazione non riesce
     */
    @Override
    public void delete() {
        try{ Files.deleteIfExists(file); }
        catch (IOException e) {
            throw new PersistenceException("Impossibile eliminare il salvataggio", e);
        }
    }
}
