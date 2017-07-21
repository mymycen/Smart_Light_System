package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.apps.smart_light.AutoMode;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Created by Leo on 21.06.2017.
 */

public class autoModeRequestHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    public AutoMode autoMode;
    private StorageAPI storageAPI;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;

    public autoModeRequestHandler(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI, StorageAPI storageAPI) {
        this.storageAPI = storageAPI;
        this.LOGGER = LOGGER;
        this.deviceAPI = deviceAPI;
        this.environmentAPI = environmentAPI;
        autoMode = new AutoMode(LOGGER, deviceAPI, environmentAPI);
    }

    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
        boolean value = true;

        try {
            value = storageAPI.loadBoolean("godmode");
            LOGGER.info(Boolean.toString(value));
        } catch (StorageAPIException e) {
            e.printStackTrace();
        }


        if (!value) {
            try {
                LOGGER.info(Boolean.toString(true));
                storageAPI.saveBoolean("godmode", true);
            } catch (StorageAPIException e) {
                e.printStackTrace();
            }
            autoMode.activateAutopilot();
        } else {
            try {
                LOGGER.info(Boolean.toString(false));
                storageAPI.saveBoolean("godmode", false);
            } catch (StorageAPIException e) {
                e.printStackTrace();
            }
            autoMode.stopAutopilot();
        }

        final JSONObject response = new JSONObject();
        response.put("value", value);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }

}