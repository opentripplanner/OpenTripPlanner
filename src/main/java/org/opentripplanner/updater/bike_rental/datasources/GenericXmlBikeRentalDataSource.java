package org.opentripplanner.updater.bike_rental.datasources;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.util.xml.XmlDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class GenericXmlBikeRentalDataSource implements BikeRentalDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GenericXmlBikeRentalDataSource.class);

    private final String url;

    List<BikeRentalStation> stations = new ArrayList<>();

    private final XmlDataListDownloader<BikeRentalStation> xmlDownloader;


    public GenericXmlBikeRentalDataSource(BikeRentalDataSourceParameters config, String path) {
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
