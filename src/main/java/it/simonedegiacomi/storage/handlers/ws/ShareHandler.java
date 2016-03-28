package it.simonedegiacomi.storage.handlers.ws;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.simonedegiacomi.configuration.Config;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.myws.WSQueryHandler;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.StorageDB;
import it.simonedegiacomi.storage.StorageEnvironment;
import it.simonedegiacomi.storage.StorageException;
import org.apache.log4j.Logger;

/**
 * @author Degiacomi Simone
 * Created on 21/02/16.
 */
public class ShareHandler implements WSQueryHandler {

    private final Logger log = Logger.getLogger(ShareHandler.class);

    private final Gson gson = new MyGsonBuilder().create();

    private final StorageDB db;

    private final static Config config = Config.getInstance();

    private final static URLBuilder urls = config.getUrls();

    public ShareHandler (StorageEnvironment env) {
        this.db = env.getDB();
    }

    @WSQuery(name = "share")
    @Override
    public JsonElement onQuery(JsonElement data) {
        JsonObject response = new JsonObject();
        JsonObject request = data.getAsJsonObject();
        boolean share = request.has("share") ? request.get("share").getAsBoolean() : false;
        GBFile file = gson.fromJson(request.get("file"), GBFile.class);
        try {
            db.changeAccess(file, share);
            response.addProperty("success", true);
            if(share) {
                response.addProperty("link", generateLink(file));
                log.info("New file shared: " + file.getName());
            } else {
                log.info("Unshared file: " + file.getName());
            }
        } catch (StorageException ex) {
            response.addProperty("success", false);
        }
        return response;
    }

    /**
     * Generate the link for the shared file. Note that this method doesn't share
     * the file, but only creates the link
     * @param file File related to the link
     * @return Link of the file as string
     */
    private String generateLink (GBFile file) {
        return new StringBuilder().append(urls.getAsString("getFile"))
                .append("?host=")
                .append(config.getAuth().getUsername())
                .append("&ID=")
                .append(file.getID())
                .toString();
    }
}