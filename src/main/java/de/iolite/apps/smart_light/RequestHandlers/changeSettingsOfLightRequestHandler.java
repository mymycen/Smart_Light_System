package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.app.api.device.access.DeviceIntegerProperty;
import de.iolite.app.api.environment.Device;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.apps.smart_light.processHttp;
import de.iolite.apps.smart_light.voiceCommander;
import de.iolite.common.requesthandler.HTTPStatus;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import de.iolite.drivers.basic.DriverConstants;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leo on 21.06.2017.
 */

public class changeSettingsOfLightRequestHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;
    private processHttp processHttp = new processHttp();
    private voiceCommander voiceCommander;

    public changeSettingsOfLightRequestHandler(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI, voiceCommander voiceCommander) {
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

        List<de.iolite.app.api.device.access.Device> deviceList = deviceAPI.getDevices();

        String dimLevel;
        String hue;
        String sat;
        String dim;
        String where;

        List<Device> allDevices = new ArrayList<>();

        try {
            JSONObject settings = new JSONObject(processHttp.readPassedData(request));
            JSONObject JSONrequest = settings.getJSONObject("settings");
            dimLevel = JSONrequest.getString("dimLevel");
            hue = JSONrequest.getString("hue");
            sat = JSONrequest.getString("sat");
            dim = JSONrequest.getString("dim");
            where = JSONrequest.getString("where");
        } catch (final JSONException e) {
            LOGGER.error("Could not handle devices request due to a JSON error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        } catch (final IOException e) {
            LOGGER.error("Could not handle devices request due to an I/O error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        }

        if (where.equals("apartement")) {
            allDevices = environmentAPI.getDevices();
        } else {
            for (Location location : environmentAPI.getLocations()) {
                if (location.getIdentifier().equals(where)) {
                    allDevices = location.getDevices();
                }
            }
        }

        for (Device device : allDevices) {
            for (de.iolite.app.api.device.access.Device devIndentifier : deviceList) {
                if (device.getIdentifier().equals(devIndentifier.getIdentifier()) && devIndentifier.getProfileIdentifier().contains("HSVLamp")) {
                    DeviceIntegerProperty dimmingProperty = devIndentifier.getIntegerProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_dimmingLevel_ID);
                    DeviceDoubleProperty hueProperty = devIndentifier.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_hue_ID);
                    DeviceDoubleProperty satProperty = devIndentifier.getDoubleProperty(DriverConstants.PROFILE_PROPERTY_HSVLamp_saturation_ID);
                    try {
                        dimmingProperty.requestValueUpdate(Integer.valueOf(dim));
                        satProperty.requestValueUpdate(Double.valueOf(sat));
                        hueProperty.requestValueUpdate(Double.valueOf(hue));
                    } catch (Exception e) {
                        LOGGER.error(e.toString());
                    }
                }
                if (device.getIdentifier().equals(devIndentifier.getIdentifier()) && devIndentifier.getProfileIdentifier().contains("DimmableLamp")) {
                    DeviceIntegerProperty dimmingProperty = devIndentifier.getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                    try {
                        dimmingProperty.requestValueUpdate(Integer.valueOf(dimLevel));
                    } catch (Exception e) {
                        LOGGER.error(e.toString());
                    }
                }
            }
        }

        final JSONObject response = new JSONObject();
        response.put("Progress", "Suceess");
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }
}