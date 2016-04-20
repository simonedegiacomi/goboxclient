package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.client.SyncEvent;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;

import java.io.File;
import java.nio.file.Files;

/**
 * @author Degiacomi Simone
 * Created on 08/02/16.
 */
public class RenameFileHandler implements WSQueryHandler{

    private final StorageDB db;

    private final Gson gson = new Gson();

    public RenameFileHandler(StorageEnvironment env) {
        this.db = env.getDB();
    }

    @WSQuery(name = "rename")
    @Override
    public JsonElement onQuery(JsonElement data) {

        // Cast the request
        JsonObject request = data.getAsJsonObject();

        // Prepare the response
        JsonObject response = new JsonObject();

        // Get the file from the request
        GBFile file = gson.fromJson(request.get("file"), GBFile.class);

        // Get the new name
        String newName = request.get("newName").getAsString();

        try {

            // Fill with the info
            db.fillFile(file);

            // Change the name on the filesystem
            Files.move(file.toFile().toPath(), new File(file.getPathAsString() + "/" + newName).toPath());

            // Change the name in the database
            file.setName(newName);
            SyncEvent update = db.updateFile(file);

            // complete the response
            response.addProperty("success", false);

        } catch (Exception ex) {

            response.addProperty("success", false);
        }
        return response;
    }
}