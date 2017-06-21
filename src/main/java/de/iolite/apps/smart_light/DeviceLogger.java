package de.iolite.apps.smart_light;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI.DeviceAPIObserver;
import de.iolite.app.api.device.access.DeviceBooleanProperty.DeviceBooleanPropertyObserver;

	public class DeviceLogger  {

	private Logger LOGGER;
	
	public DeviceLogger(Logger logger){
	this.LOGGER=logger;
	
	}

	public  class DeviceAddAndRemoveLogger implements DeviceAPIObserver {
		
		public DeviceAddAndRemoveLogger(){
		}
		
		
		@Override
		public void addedToDevices(final Device device) {
			LOGGER.debug("a new device added '{}'", device.getIdentifier());
		}

		@Override
		public void removedFromDevices(final Device device) {
			LOGGER.debug("a device removed '{}'", device.getIdentifier());
		}
	}

	public  class DeviceOnOffStatusLogger implements DeviceBooleanPropertyObserver {
		
			
		@Nonnull
		private final String identifier;

		public DeviceOnOffStatusLogger(final String deviceIdentifier) {
			this.identifier = Validate.notNull(deviceIdentifier, "'deviceIdentifier' must not be null");
			
		}

		@Override
		public void deviceChanged(final Device element) {
			// nothing here
		}

		@Override
		public void keyChanged(final String key) {
			// nothing here
		}

		@Override
		public void valueChanged(final Boolean value) {
			if (value) {
				LOGGER.debug("device '{}' turned on", this.identifier);
			} else {
				LOGGER.debug("device '{}' turned off", this.identifier);
			}
		}
	}
}