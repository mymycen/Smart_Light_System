package de.iolite.apps.smart_light.RequestHandlers;

import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.apps.smart_light.Controller;
import de.iolite.apps.smart_light.processHttp;
import de.iolite.apps.smart_light.voiceCommander;
import de.iolite.common.requesthandler.HTTPStatus;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Created by Leo on 21.06.2017.
 */

public class configureRequestHandler extends FrontendAPIRequestHandler {

    private Logger LOGGER;
    private voiceCommander voiceCommander;
    private processHttp processHttp = new processHttp();

    public configureRequestHandler(Logger LOGGER, voiceCommander voiceCommander) {

        this.voiceCommander = voiceCommander;
        this.LOGGER = LOGGER;
    }


    @Override
    protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {

        String hue;
        String sat;
        String dim;
        String mode;


        try {

            JSONObject settings = new JSONObject(processHttp.readPassedData(request));
            JSONObject JSONrequest = settings.getJSONObject("settings");
            hue = JSONrequest.getString("hue");
            sat = JSONrequest.getString("sat");
            dim = JSONrequest.getString("dim");
            mode = JSONrequest.getString("mode");
        } catch (final JSONException e) {
            LOGGER.error("Could not handle devices request due to a JSON error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        } catch (final IOException e) {
            LOGGER.error("Could not handle devices request due to an I/O error: {}", e.getMessage(), e);
            return new IOLITEHTTPStaticResponse(e.getMessage(), HTTPStatus.BadRequest, "text/plain");
        }


        try {
            voiceCommander.stopPartyMode();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        if (mode.equals("Party")) {
            Controller.partyMode1.setDimmLevel(Integer.valueOf(dim));
            Controller.partyMode1.setHue(Double.valueOf(hue));
            Controller.partyMode1.setSaturation(Double.valueOf(sat));
            Controller.partyMode2.setDimmLevel(Integer.valueOf(dim));
            Controller.partyMode2.setHue(Double.valueOf(hue));
            Controller.partyMode2.setSaturation(Double.valueOf(sat));
            Controller.partyMode3.setDimmLevel(Integer.valueOf(dim));
            Controller.partyMode3.setHue(Double.valueOf(hue));
            Controller.partyMode3.setSaturation(Double.valueOf(sat));
            Controller.partyMode4.setDimmLevel(Integer.valueOf(dim));
            Controller.partyMode4.setHue(Double.valueOf(hue));
            Controller.partyMode4.setSaturation(Double.valueOf(sat));
        } else if (mode.equals("Romantic")) {
            Controller.romaticMode.setDimmLevel(Integer.valueOf(dim));
            Controller.romaticMode.setHue(Double.valueOf(hue));
            Controller.romaticMode.setSaturation(Double.valueOf(sat));
        } else if (mode.equals("Sleeping")) {
            Controller.sleepingMode.setDimmLevel(Integer.valueOf(dim));
            Controller.sleepingMode.setHue(Double.valueOf(hue));
            Controller.sleepingMode.setSaturation(Double.valueOf(sat));
        } else if (mode.equals("Working")) {
            Controller.workingMode.setDimmLevel(Integer.valueOf(dim));
            Controller.workingMode.setHue(Double.valueOf(hue));
            Controller.workingMode.setSaturation(Double.valueOf(sat));
        } else if (mode.equals("Movie")) {
            Controller.movieMode.setDimmLevel(Integer.valueOf(dim));
            Controller.movieMode.setHue(Double.valueOf(hue));
            Controller.movieMode.setSaturation(Double.valueOf(sat));
        }


        LOGGER.info(String.valueOf(Controller.movieMode.getDimmLevel()));
        LOGGER.info(String.valueOf(Controller.partyMode1.getDimmLevel()));
        LOGGER.info(String.valueOf(Controller.sleepingMode.getDimmLevel()));
        LOGGER.info(String.valueOf(Controller.workingMode.getDimmLevel()));
        LOGGER.info(String.valueOf(Controller.partyMode1.getDimmLevel()));


        final JSONObject response = new JSONObject();
        response.put("value", "success");
        return new IOLITEHTTPStaticResponse(response.toString(), IOLITEHTTPResponse.JSON_CONTENT_TYPE);
    }
}