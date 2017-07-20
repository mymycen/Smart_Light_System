package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.apps.smart_light.voiceCommander;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Created by Leo on 21.06.2017.
 */

public class voiceCommandRequestHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    public voiceCommander voice;
    private StorageAPI storageAPI;


    public voiceCommandRequestHandler(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI, StorageAPI storageAPI) {
        this.storageAPI = storageAPI;
        this.LOGGER = LOGGER;
        voice = new voiceCommander(LOGGER, deviceAPI, environmentAPI);
    }

    public voiceCommander getVoice() {
        return voice;
    }

    public void setVoice(voiceCommander voice) {
        this.voice = voice;
    }

    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
        boolean value = true;

        try {
            value = storageAPI.loadBoolean("recognition");
            LOGGER.info(Boolean.toString(value));
        } catch (StorageAPIException e) {
            e.printStackTrace();
        }


        if (!value) {
            try {
                LOGGER.info(Boolean.toString(true));
                storageAPI.saveBoolean("recognition", true);
            } catch (StorageAPIException e) {
                e.printStackTrace();
            }
            voice.executeVoiceCommand();
        } else {
            try {
                LOGGER.info(Boolean.toString(false));
                storageAPI.saveBoolean("recognition", false);
            } catch (StorageAPIException e) {
                e.printStackTrace();
            }
            voice.stopRecognition();
        }




/*        final JSONArray jsonDeviceArray = new JSONArray();
        for (final Device device : deviceAPI.getDevices()) {
            if (device.getIdentifier().equals(deviceType)){
                DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
                try {
                    if(!onProperty.getValue()){
                        onProperty.requestValueUpdate(true);}
                    else {
                        onProperty.requestValueUpdate(false);
                    }
                    value = onProperty.getValue().toString();
                }catch (Exception e){
                    LOGGER.error(e.toString());
                }
            }
        }*/

        final JSONObject response = new JSONObject();
        response.put("value", value);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }

}