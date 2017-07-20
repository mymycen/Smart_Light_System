package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.app.api.device.access.DeviceIntegerProperty;
import de.iolite.app.api.environment.Device;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.apps.smart_light.Controller;
import de.iolite.apps.smart_light.LightMode;
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
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Leo on 21.06.2017.
 */

public class changeLightmodeRequestHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;
    private processHttp processHttp = new processHttp();
    public voiceCommander voiceCommander;

    public changeLightmodeRequestHandler(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI, voiceCommander voiceCommander) {
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

        String modeName;
        String where;
        LightMode lightmode = Controller.romaticMode;

        List<Device> allDevices = new ArrayList<>();

        try {
            JSONObject settings = new JSONObject(processHttp.readPassedData(request));
            JSONObject JSONrequest = settings.getJSONObject("settings");
            modeName = JSONrequest.getString("modeName");
            where = JSONrequest.getString("where");
        } catch (final JSONException e) {
            LOGGER.error("Could not handle devices request due to a JSON error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        } catch (final IOException e) {
            LOGGER.error("Could not handle devices request due to an I/O error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        }

        if (modeName.equals("Romantic")) {
            lightmode = Controller.romaticMode;
        } else if (modeName.equals("Party")) {

            voiceCommander voiceC = new voiceCommander(LOGGER, deviceAPI, environmentAPI);

            TimerTask task1 = voiceC.new runPartyLight(Controller.partyMode1.getDimmLevel(), Controller.partyMode1.getHue(),
                    Controller.partyMode1.getSaturation());
            if (voiceCommander.timer1 == null)
                voiceCommander.timer1 = new Timer();
            voiceCommander.timer1.schedule(task1, 0, 8000);

            TimerTask task2 = voiceC.new runPartyLight(Controller.partyMode2.getDimmLevel(), Controller.partyMode2.getHue(),
                    Controller.partyMode2.getSaturation());
            if (voiceCommander.timer2 == null)
                voiceCommander.timer2 = new Timer();
            voiceCommander.timer2.schedule(task2, 2000, 8000);

            TimerTask task3 = voiceC.new runPartyLight(Controller.partyMode3.getDimmLevel(), Controller.partyMode3.getHue(),
                    Controller.partyMode3.getSaturation());
            if (voiceCommander.timer3 == null)
                voiceCommander.timer3 = new Timer();
            voiceCommander.timer3.schedule(task3, 4000, 8000);

            TimerTask task4 = voiceC.new runPartyLight(Controller.partyMode4.getDimmLevel(), Controller.partyMode4.getHue(),
                    Controller.partyMode4.getSaturation());
            if (voiceCommander.timer4 == null)
                voiceCommander.timer4 = new Timer();
            voiceCommander.timer4.schedule(task4, 6000, 8000);

            final JSONObject response = new JSONObject();
            response.put("Progress", "Suceess");
            return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);

        } else if (modeName.equals("Sleeping")) {
            lightmode = Controller.sleepingMode;
        } else if (modeName.equals("Movie")) {
            lightmode = Controller.movieMode;
        } else if (modeName.equals("Working")) {
            lightmode = Controller.workingMode;
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
                        dimmingProperty.requestValueUpdate(lightmode.getDimmLevel());
                        satProperty.requestValueUpdate(lightmode.getSaturation());
                        hueProperty.requestValueUpdate(lightmode.getHue());
                    } catch (Exception e) {
                        LOGGER.error(e.toString());
                    }
                }
                if (device.getIdentifier().equals(devIndentifier.getIdentifier()) && devIndentifier.getProfileIdentifier().contains("DimmableLamp")) {
                    DeviceIntegerProperty dimmingProperty = devIndentifier.getIntegerProperty(DriverConstants.PROFILE_PROPERTY_DimmableLamp_dimmingLevel_ID);
                    try {
                        dimmingProperty.requestValueUpdate(lightmode.getDimmLevel());
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