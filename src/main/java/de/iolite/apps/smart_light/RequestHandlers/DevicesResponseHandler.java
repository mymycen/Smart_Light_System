package de.iolite.apps.smart_light.RequestHandlers;

/**
 * Created by Leo on 22.06.2017.
 */

import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.apps.smart_light.processHttp;
import de.iolite.common.requesthandler.HTTPStatus;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;


/**
 * A response handler returning devices filtered by the property type.
 */
public class DevicesResponseHandler extends FrontendAPIRequestHandler {

    processHttp processHttp = new processHttp();
    private Logger LOGGER;
    private DeviceAPI deviceAPI;

    public DevicesResponseHandler(Logger LOGGER, DeviceAPI deviceAPI){
        this.LOGGER = LOGGER;
        this.deviceAPI = deviceAPI;
    }

    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
        String propertyType;
        try {
            propertyType = new JSONObject(processHttp.readPassedData(request)).getString("propertyType");
        } catch (final JSONException e) {
            LOGGER.error("Could not handle devices request due to a JSON error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        } catch (final IOException e) {
            LOGGER.error("Could not handle devices request due to an I/O error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        }

        final JSONArray jsonDeviceArray = new JSONArray();
        for (final Device device : deviceAPI.getDevices()) {
            if (device.getProperty(propertyType) != null) {
                // device has the correct property type
                final JSONObject jsonDeviceObject = new JSONObject();
                jsonDeviceObject.put("name", device.getName());
                jsonDeviceObject.put("identifier", device.getIdentifier());
                jsonDeviceArray.put(jsonDeviceObject);
            }
        }

        final JSONObject response = new JSONObject();
        response.put("devices", jsonDeviceArray);
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }

}