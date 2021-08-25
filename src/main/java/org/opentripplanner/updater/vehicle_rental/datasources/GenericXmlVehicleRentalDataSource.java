package org.opentripplanner.updater.vehicle_rental.datasources;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;
import org.opentripplanner.util.xml.XmlDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class GenericXmlVehicleRentalDataSource implements VehicleRentalDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GenericXmlVehicleRentalDataSource.class);

    private final String url;

    List<BikeRentalStation> stations = new ArrayList<>();

    private final XmlDataListDownloader<BikeRentalStation> xmlDownloader;


    public GenericXmlVehicleRentalDataSource(VehicleRentalDataSourceParameters config, String path) {
        url = config.getUrl();
        xmlDownloader = new XmlDataListDownloader<>();
        xmlDownloader.setPath(path);
        /* TODO Do not make this class abstract, but instead make the client
         * provide itself the factory?
         */
        xmlDownloader.setDataFactory(this::makeStation);
    }

    @Override
    public boolean update() {
        List<BikeRentalStation> newStations = xmlDownloader.download(url, false);
        if (newStations != null) {
            synchronized(this) {
                stations = newStations;
            }
            return true;
        }
        LOG.info("Can't update bike rental station list from: " + url + ", keeping current list.");
        return false;
    }

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return stations;
    }

    public void setReadAttributes(boolean readAttributes) {
        // if readAttributes is true, read XML attributes of selected elements, instead of children
        xmlDownloader.setReadAttributes(readAttributes);
    }

    public abstract BikeRentalStation makeStation(Map<String, String> attributes);

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }
}
