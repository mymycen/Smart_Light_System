/**
 * Copyright (C) 2016 IOLITE GmbH, All rights reserved.
 */

package de.iolite.apps.smart_light;

import de.iolite.api.IOLITEAPINotResolvableException;
import de.iolite.api.IOLITEAPIProvider;
import de.iolite.api.IOLITEPermissionDeniedException;
import de.iolite.api.heating.access.HeatingAPI;
import de.iolite.api.heating.access.PlaceSchedule;
import de.iolite.app.AbstractIOLITEApp;
import de.iolite.app.api.device.DeviceAPIException;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceBooleanProperty;
import de.iolite.app.api.device.access.DeviceDoubleProperty;
import de.iolite.app.api.environment.EnvironmentAPI;
import de.iolite.app.api.environment.Location;
import de.iolite.app.api.frontend.FrontendAPI;
import de.iolite.app.api.frontend.FrontendAPIException;
import de.iolite.app.api.frontend.util.FrontendAPIRequestHandler;
import de.iolite.app.api.frontend.util.FrontendAPIUtility;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.app.api.user.access.UserAPI;
import de.iolite.apps.smart_light.RequestHandlers.*;
import de.iolite.apps.smart_light.internals.PageWithEmbeddedSessionTokenRequestHandler;
import de.iolite.common.lifecycle.exception.CleanUpFailedException;
import de.iolite.common.lifecycle.exception.InitializeFailedException;
import de.iolite.common.lifecycle.exception.StartFailedException;
import de.iolite.common.lifecycle.exception.StopFailedException;
import de.iolite.common.requesthandler.*;
import de.iolite.common.requesthandler.StaticResources.PathHandlerPair;
import de.iolite.drivers.basic.DriverConstants;
import de.iolite.utilities.disposeable.Disposeable;
import de.iolite.utilities.time.series.DataEntries.AggregatedEntry;
import de.iolite.utilities.time.series.DataEntries.BooleanEntry;
import de.iolite.utilities.time.series.Function;
import de.iolite.utilities.time.series.TimeInterval;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 */
public final class Controller extends AbstractIOLITEApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

    /* App APIs */
    private FrontendAPI frontendAPI;
    private StorageAPI storageAPI;
    private static DeviceAPI deviceAPI;
    private static EnvironmentAPI environmentAPI;
    private UserAPI userAPI;

    private HeatingAPI heatingAPI;
    private List<LightMode> allModes = new ArrayList<>();
    public static LightMode romaticMode = new LightMode(50, 344.0, 95.0, "Romantic");
    public static LightMode partyMode1 = new LightMode(100, 2.0, 100.0, "Party");
    public static LightMode partyMode2 = new LightMode(1, 274.0, 100.0, "Party2");
    public static LightMode partyMode3 = new LightMode(100, 96.0, 100.0, "Party3");
    public static LightMode partyMode4 = new LightMode(1, 240.0, 100.0, "Party4");
    public static LightMode sleepingMode = new LightMode(3, 2.0, 100.0, "Sleeping");
    public static LightMode movieMode = new LightMode(10, 275, 57, "Movie");
    public static LightMode workingMode = new LightMode(100, 0.0, 0.0, "Working");

    /**
     * front end assets
     */
    private Disposeable disposeableAssets;
    DeviceLogger dLogger = new DeviceLogger(LOGGER);
    Movement2 movement = new Movement2();
    AutoMode autoMode = new AutoMode();
    protected static List<Location> rooms;
    private processHttp processHttp = new processHttp();


    public Controller() {
        // Has to be empty

    }

    private static final class DeviceJSONRequestHandler extends FrontendAPIRequestHandler {

        @Override
        protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
            final JSONArray deviceIdentifiers = new JSONArray();

            final JSONObject response = new JSONObject();
            response.put("identifiers", deviceIdentifiers);
            return new IOLITEHTTPStaticResponse(response.toString(), HTTPStatus.OK,
                    IOLITEHTTPResponse.JSON_CONTENT_TYPE);
        }
    }


    /**
     * A response handler for returning the "not found" response.
     */
    static class NotFoundResponseHandler extends FrontendAPIRequestHandler {

        @Override
        protected IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final String subPath) {
            return new IOLITEHTTPStaticResponse(HTTPStatus.NotFound, IOLITEHTTPResponse.HTML_CONTENT_TYPE);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void cleanUpHook() throws CleanUpFailedException {
        LOGGER.debug("Cleaning");
        LOGGER.debug("Cleaned");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeHook() throws InitializeFailedException {
        LOGGER.debug("Initializing");
        LOGGER.debug("Initialized");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void startHook(@Nonnull final IOLITEAPIProvider context) throws StartFailedException {
        // here the IOLITE App is started
        // the context gives access to IOLITE App APIs
        LOGGER.debug("Starting");


        try {
            // use User API
            this.userAPI = context.getAPI(UserAPI.class);
            LOGGER.debug("Running for user '{}' with locale '{}'", this.userAPI.getUser().getIdentifier(),
                    this.userAPI.getUser().getLocale());

            // Storage API enables the App to store data persistently
            // whatever is stored via the storage API will also be available if
            // the App is restarted
            this.storageAPI = context.getAPI(StorageAPI.class);
            initializeStorage();

            // Frontend API enables the App to expose a user interface
            this.frontendAPI = context.getAPI(FrontendAPI.class);

            // Device API gives access to devices connected to IOLITE
            deviceAPI = context.getAPI(DeviceAPI.class);

            // Environment API gives a access for rooms, current situation etc.
            environmentAPI = context.getAPI(EnvironmentAPI.class);
            rooms = environmentAPI.getLocations();

            // Heating API
            this.heatingAPI = context.getAPI(HeatingAPI.class);
            for (final PlaceSchedule placeSchedule : this.heatingAPI.getHeatingSchedulesOfPlaces()) {
                LOGGER.debug("Heating schedule found for place '{}'", placeSchedule.getPlaceIdentifier());
            }
            allModes.add(sleepingMode);
            allModes.add(movieMode);
            allModes.add(romaticMode);
            allModes.add(workingMode);
            initializeWebResources();

        } catch (final IOLITEAPINotResolvableException e) {
            throw new StartFailedException(
                    MessageFormat.format("Start failed due to required but not resolvable AppAPI: {0}", e.getMessage()),
                    e);
        } catch (final IOLITEPermissionDeniedException e) {
            throw new StartFailedException(MessageFormat
                    .format("Start failed due to permission denied problems in the examples: {0}", e.getMessage()), e);
        } catch (final StorageAPIException | FrontendAPIException e) {
            throw new StartFailedException(
                    MessageFormat.format("Start failed due to an error in the App API examples: {0}", e.getMessage()),
                    e);
        }

        LOGGER.debug("Started");
        movement.detectMovement(LOGGER, deviceAPI, environmentAPI);
        autoMode.activateAutopilot(LOGGER, deviceAPI, environmentAPI);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopHook() throws StopFailedException {
        LOGGER.debug("Stopping");
        // deregister the static assets
        if (this.disposeableAssets != null) {
            this.disposeableAssets.dispose();
        }

        LOGGER.debug("Stopped");
    }

    /**
     * Example method showing how to use the Device API.
     */
    private void initializeDeviceManager() {
        // register a device observer
        this.deviceAPI.setObserver(dLogger.new DeviceAddAndRemoveLogger());

        // go through all devices, and register a property observer for ON/OFF
        // properties
        for (final Device device : this.deviceAPI.getDevices()) {
            // each device has some properties (accessible under
            // device.getProperties())
            // let's get the 'on/off' status property
            final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
            if (onProperty != null) {
                LOGGER.debug("device '{}' has ON/OFF property, current value: '{}'", device.getIdentifier(),
                        onProperty.getValue());

                onProperty.setObserver(dLogger.new DeviceOnOffStatusLogger(device.getIdentifier()));
            }
        }

        // go through all devices, and toggle ON/OFF properties
        for (final Device device : this.deviceAPI.getDevices()) {
            // let's get the 'on/off' status property
            final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);
            final Boolean onValue;
            if (onProperty != null && (onValue = onProperty.getValue()) != null) {
                LOGGER.debug("toggling device '{}'", device.getIdentifier());
                try {
                    onProperty.requestValueUpdate(!onValue);
                } catch (final DeviceAPIException e) {
                    LOGGER.error("Failed to control device", e);
                }
            }
        }

        // go through all devices, and print ON/OFF and POWER USAGE property
        // history datas
        for (final Device device : this.deviceAPI.getDevices()) {
            // ON/OFF history data
            final DeviceBooleanProperty onProperty = device.getBooleanProperty(DriverConstants.PROPERTY_on_ID);

            if (onProperty != null) {
                // retrieve the on/off history of last hour
                final long hourMillis = TimeUnit.SECONDS.toMillis(60 * 60);
                final List<BooleanEntry> onHistory;
                try {
                    onHistory = onProperty.getValuesSince(System.currentTimeMillis() - hourMillis);
                } catch (final DeviceAPIException e) {
                    LOGGER.error("Failed to retrieve the history of property '{}'", onProperty.getKey(), e);
                    continue;
                }
                LOGGER.debug("Got '{}' historical values for property '{}'", onHistory.size(), onProperty.getKey());
                // log history values
                final DateFormat dateFormat = DateFormat.getTimeInstance();
                for (final BooleanEntry historyEntry : onHistory) {
                    LOGGER.debug("At '{}' the value was '{}'", dateFormat.format(new Date(historyEntry.time)),
                            historyEntry.value);
                }
            }

            // POWER USAGE history data
            final DeviceDoubleProperty powerUsage = device.getDoubleProperty(DriverConstants.PROPERTY_powerUsage_ID);
            if (powerUsage != null) {
                LOGGER.debug("Reading today's hourly power usage data from device '{}':", device.getIdentifier());
                List<AggregatedEntry> history;
                try {
                    history = powerUsage.getAggregatedValuesOf(System.currentTimeMillis(), TimeInterval.DAY,
                            TimeInterval.HOUR, Function.AVERAGE);
                    for (final AggregatedEntry entry : history) {
                        LOGGER.debug("The device used an average of {} Watt at '{}'.", entry.getAggregatedValue(),
                                DateFormat.getTimeInstance().format(new Date(entry.getEndTime())));
                    }
                } catch (final DeviceAPIException e) {
                    LOGGER.error("Failed to retrieve history data of device", e);
                }
            }
        }
    }

    /**
     * Example method showing how to use the Storate API.
     *
     * @throws StorageAPIException
     */
    private void initializeStorage() throws StorageAPIException {
        // basically the Storage API provides a key/value storage for different
        // data types
        // save an integer under the key 'test'
        this.storageAPI.saveInt("test", 10);
        this.storageAPI.saveBoolean("recognition", false);
        this.storageAPI.saveBoolean("movement", false);
        this.storageAPI.saveBoolean("godmode", false);
        this.storageAPI.saveBoolean("alllights", false);
        // now let's store a string
        this.storageAPI.saveString("some key", "some value");

        // log the value of an entry, just to demonstrate
        LOGGER.debug("loading 'test' from storage: {}", Integer.valueOf(this.storageAPI.loadInt("test")));
    }

    /**
     * Registering web resources.
     *
     * @throws FrontendAPIException if some resources are not found.
     */
    private final void initializeWebResources() throws FrontendAPIException {

        // go through static assets and register them
        final Map<URI, PathHandlerPair> assets = StaticResources.scanClasspath("assets", getClass().getClassLoader());
        this.disposeableAssets = FrontendAPIUtility.registerPublicHandlers(this.frontendAPI, assets);

        // index page
        final IOLITEHTTPRequestHandler indexPageRequestHandler = new PageWithEmbeddedSessionTokenRequestHandler(
                loadTemplate("assets/index.html"));
        this.frontendAPI.registerRequestHandler("", indexPageRequestHandler);
        this.frontendAPI.registerRequestHandler("index.html", indexPageRequestHandler);

        // default handler returning a not found status
        this.frontendAPI.registerDefaultRequestHandler(new NotFoundResponseHandler());


        voiceCommandRequestHandler voicer = new voiceCommandRequestHandler(LOGGER, deviceAPI, environmentAPI, storageAPI);
        // example JSON request handlers
        this.frontendAPI.registerRequestHandler("rooms", new RoomsResponseHandler(environmentAPI));
        this.frontendAPI.registerRequestHandler("setValue", new setPropertyRequestHandler(LOGGER, deviceAPI, environmentAPI));
        this.frontendAPI.registerRequestHandler("roomsWithDevs", new RoomsWithDevicesResponseHandler(environmentAPI, deviceAPI, LOGGER));
        this.frontendAPI.registerRequestHandler("startVoice", voicer);
        this.frontendAPI.registerRequestHandler("getAllModes", new ModesResponseHandler(LOGGER, allModes));
        this.frontendAPI.registerRequestHandler("getStatus", new StatusResponseHandler(LOGGER, storageAPI));
        this.frontendAPI.registerRequestHandler("changeAllLights", new changeAllLightsRequestHandler(LOGGER, deviceAPI, environmentAPI, storageAPI, voicer.voice));
        this.frontendAPI.registerRequestHandler("changeSettings", new changeSettingsOfLightRequestHandler(LOGGER, deviceAPI, environmentAPI, voicer.voice));
        this.frontendAPI.registerRequestHandler("changeLightmode", new changeLightmodeRequestHandler(LOGGER, deviceAPI, environmentAPI, voicer.voice));
        this.frontendAPI.registerRequestHandler("startDetect", new movementDetectRequestHandler(LOGGER, deviceAPI, environmentAPI, storageAPI));
        this.frontendAPI.registerRequestHandler("configureLightmode", new configureRequestHandler(LOGGER, voicer.voice));


        this.frontendAPI.registerRequestHandler("get_devices.json", new DeviceJSONRequestHandler());
    }

    /**
     * Load a HTML template as string.
     */
    private String loadTemplate(final String templateResource) {
        try {
            return StaticResources.loadResource(templateResource, getClass().getClassLoader());
        } catch (final IOException e) {
            throw new InitializeFailedException("Loading templates for the dummy app failed", e);
        }
    }

}