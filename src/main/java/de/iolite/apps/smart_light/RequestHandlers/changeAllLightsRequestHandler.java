package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.environment.Device;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.apps.smart_light.voiceCommander;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import de.iolite.drivers.basic.DriverConstants;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Created by Leo on 21.06.2017.
 */

public class changeAllLightsRequestHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;
    private StorageAPI storageAPI;
    private boolean alllights;
    public voiceCommander voiceCommander;

    public changeAllLightsRequestHandler(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI, StorageAPI storageAPI,
                                         voiceCommander voiceCommander) {
        this.storageAPI = storageAPI;
        this.LOGGER = LOGGER;
        this.deviceAPI = deviceAPI;
        this.environmentAPI = environmentAPI;
        this.voiceCommander = voiceCommander;
    }


    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {

        try {
            voiceCommander.stopPartyMode();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        String value = "false";

        List<de.iolite.app.api.device.access.Device> deviceList = deviceAPI.getDevices();

        try {
            alllights = storageAPI.loadBoolean("alllights");
        } catch (StorageAPIException e) {
            e.printStackTrace();
        }

        for (Device device : environmentAPI.getDevices()) {
            for (de.iolite.app.api.device.access.Device devIndentifier : deviceList) {
                if (device.getIdentifier().equals(devIndentifier.getIdentifier()) && devIndentifier.getProfileIdentifier().contains("Lamp")) {
                    DeviceBooleanProperty onProperty = devIndentifier.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                    try {
                        if (!alllights) {
                            try {
                                storageAPI.saveBoolean("alllights", true);
                            } catch (StorageAPIException e) {
                                e.printStackTrace();
                            }
                            onProperty.requestValueUpdate(true);
                        } else {
                            try {
                                storageAPI.saveBoolean("alllights", false);
                            } catch (StorageAPIException e) {
                                e.printStackTrace();
                            }
                            onProperty.requestValueUpdate(false);
                        }
                        value = onProperty.getValue().toString();
                    } catch (Exception e) {
                        LOGGER.error(e.toString());
                    }
                }
            }
        }

        final JSONObject response = new JSONObject();
        response.put("value", value);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }
}