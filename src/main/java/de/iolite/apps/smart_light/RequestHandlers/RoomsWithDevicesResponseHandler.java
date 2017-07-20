package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.environment.Device;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.apps.smart_light.processHttp;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.List;

/**
 * Created by Leo on 15.07.2017.
 */
public class RoomsWithDevicesResponseHandler extends FrontendAPIRequestHandler {

    private EnvironmentAPI environmentAPI;
    private DeviceAPI deviceAPI;
    private Logger LOGGER;
    private processHttp processor = new processHttp();

    public RoomsWithDevicesResponseHandler(EnvironmentAPI environmentAPI, DeviceAPI deviceAPI, Logger LOGGER) {
        this.environmentAPI = environmentAPI;
        this.deviceAPI = deviceAPI;
        this.LOGGER = LOGGER;
    }

    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {

        JSONArray allRooms = new JSONArray();
        List<de.iolite.app.api.device.access.Device> deviceList = deviceAPI.getDevices();

        for (Location location : environmentAPI.getLocations()) {
            JSONObject singleRoom = new JSONObject();
            JSONArray devices = new JSONArray();
            for (Device device : location.getDevices()) {
                JSONObject singleDevice = new JSONObject();
                for (de.iolite.app.api.device.access.Device device1 : deviceList) {
                    if (device.getIdentifier().equals(device1.getIdentifier()) && device1.getProfileIdentifier().contains("Lamp")) {
                        singleDevice.put("name", device1.getName());
                        singleDevice.put("identifier", device1.getIdentifier());
                        devices.put(singleDevice);
                    }
                }
            }
            singleRoom.put("name", location.getName());
            singleRoom.put("identifier", location.getIdentifier());
            singleRoom.put("devices", devices);
            allRooms.put(singleRoom);

        }
        final JSONObject response = new JSONObject();
        response.put("rooms", allRooms);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }
}
