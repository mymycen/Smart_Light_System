package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Leo on 21.06.2017.
 */

public class RoomsResponseHandler extends FrontendAPIRequestHandler {

    private EnvironmentAPI environmentAPI;

    public RoomsResponseHandler(EnvironmentAPI environmentAPI) {
        this.environmentAPI = environmentAPI;
    }

    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
        final JSONArray locationNames = new JSONArray();
        for (final Location location : environmentAPI.getLocations()) {
            locationNames.put(location.getName());
        }
        final JSONObject response = new JSONObject();
        response.put("rooms", locationNames);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }
}