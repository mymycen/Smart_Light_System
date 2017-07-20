package de.iolite.apps.smart_light.RequestHandlers;

/**
 * Created by Leo on 22.06.2017.
 */

import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * A response handler returning devices filtered by the property type.
 */
public class StatusResponseHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    private StorageAPI storageAPI;

    public StatusResponseHandler(Logger LOGGER, StorageAPI storageAPI) {
        this.LOGGER = LOGGER;
        this.storageAPI = storageAPI;
    }

    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {

        List<String> keys = new ArrayList<>();
        JSONArray status = new JSONArray();


        try {
            keys = (storageAPI.getBooleanKeys());
        } catch (StorageAPIException e) {
            e.printStackTrace();
        }

        for (int a = 0; a < keys.size(); a++) {
            JSONObject JSONstatus = new JSONObject();
            JSONstatus.put("name", keys.get(a));
            try {
                JSONstatus.put("value", storageAPI.loadBoolean(keys.get(a)));
            } catch (StorageAPIException e) {
                e.printStackTrace();
            }
            status.put(JSONstatus);
        }

        final JSONObject response = new JSONObject();
        response.put("status", status);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }

}