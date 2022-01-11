package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;

/**
 * Class that implements {@link PollingGraphUpdaterParameters} and can be extended to include other
 * parameters required by a custom vehicle parking updater.
 */
public class VehicleParkingUpdaterParameters implements PollingGraphUpdaterParameters {

    private final String configRef;
    private final int frequencySec;
    private final DataSourceType sourceType;


    public VehicleParkingUpdaterParameters(
            String configRef,
            int frequencySec,
            DataSourceType sourceType
    ) {
        this.configRef = configRef;
        this.frequencySec = frequencySec;
        this.sourceType = sourceType;
    }

    @Override
    public int getFrequencySec() {return frequencySec;}

    /**
     * The config name/type for the updater. Used to reference the configuration element.
     */
    @Override
    public String getConfigRef() {
        return configRef;
    }

    public DataSourceType getSourceType() {
        return sourceType;
    }
}
