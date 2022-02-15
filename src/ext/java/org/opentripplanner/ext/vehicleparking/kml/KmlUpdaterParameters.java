package org.opentripplanner.ext.vehicleparking.kml;

import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * KmlBikeParkDataSource}.
 */
public class KmlUpdaterParameters extends VehicleParkingUpdaterParameters {

    private final String url;
    private final String feedId;
    private final String namePrefix;
    private final boolean zip;


    public KmlUpdaterParameters(
            String configRef,
            String url,
            String feedId,
            String namePrefix,
            int frequencySec,
            boolean zip,
            DataSourceType sourceType
    ) {
        super(configRef, frequencySec, sourceType);
        this.url = url;
        this.feedId = feedId;
        this.namePrefix = namePrefix;
        this.zip = zip;
    }

    public String getFeedId() {
        return feedId;
    }

    public String getUrl() {
        return url;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public boolean isZip() {
        return zip;
    }
}
