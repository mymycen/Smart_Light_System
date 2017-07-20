package de.iolite.apps.smart_light.RequestHandlers;

/**
 * Created by Leo on 20.07.2017.
 */

import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.apps.smart_light.LightMode;
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
public class ModesResponseHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    private ArrayList<LightMode> allModes;

    public ModesResponseHandler(Logger LOGGER, List<LightMode> allModes) {
        this.allModes = new ArrayList<>(allModes);
        this.LOGGER = LOGGER;
    }

    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {

        final JSONArray JSONmodes = new JSONArray();
        for (int a = 0; a < allModes.size(); a++) {
            JSONObject mode = new JSONObject();
            LightMode lightMode = allModes.get(a);
            mode.put("name", lightMode.getModeName());
            mode.put("hue", lightMode.getHue());
            mode.put("dimmLevel", lightMode.getDimmLevel());
            mode.put("saturation", lightMode.getSaturation());
            JSONmodes.put(mode);
        }

        final JSONObject response = new JSONObject();
        response.put("modes", JSONmodes);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }

}