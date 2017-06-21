package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.common.requesthandler.HTTPStatus;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import de.iolite.drivers.basic.DriverConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Leo on 21.06.2017.
 */

public class setPropertyRequestHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    private DeviceAPI deviceAPI;
    private EnvironmentAPI environmentAPI;

    public setPropertyRequestHandler(Logger LOGGER, DeviceAPI deviceAPI, EnvironmentAPI environmentAPI){

        this.LOGGER = LOGGER;
        this.deviceAPI = deviceAPI;
        this.environmentAPI = environmentAPI;
    }

        @Override
        protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
            String value = "false";
            String deviceType;


            try {
                //propertyType = new JSONObject(readPassedData(request)).getString("propertyType");
                deviceType = new JSONObject(readPassedData(request)).getString("deviceType");
                LOGGER.info("TYPPEEEE  " + deviceType    );
            } catch (final JSONException e) {
                LOGGER.error("Could not handle devices request due to a JSON error: {}", e.getMessage(), e);
                return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
            } catch (final IOException e) {
                LOGGER.error("Could not handle devices request due to an I/O error: {}", e.getMessage(), e);
                return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
            }

            final JSONArray jsonDeviceArray = new JSONArray();
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
            }

            final JSONObject response = new JSONObject();
            response.put("value", value);
            return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
        }

        private String getCharset(final IOLITEHTTPRequest request) {
            final String charset = request.getCharset();
            return charset == null || charset.length() == 0 ? IOLITEHTTPStaticResponse.ENCODING_UTF8 : charset;
        }

        private String readPassedData(final IOLITEHTTPRequest request) throws IOException {
            final String charset = getCharset(request);
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(request.getContent(), charset))) {
                final StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return stringBuilder.toString();
            }
        }
    }