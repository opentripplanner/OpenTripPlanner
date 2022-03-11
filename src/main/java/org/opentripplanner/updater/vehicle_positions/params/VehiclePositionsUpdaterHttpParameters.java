package org.opentripplanner.updater.vehicle_positions.params;

import java.util.Objects;

public class VehiclePositionsUpdaterHttpParameters implements VehiclePositionsUpdaterParameters {

    /**
     * Feed id that is used for the trip ids in the TripUpdates
     */
    final public String feedId;
    final public String url;
    final public int frequencySec;
    final String configRef;

    public VehiclePositionsUpdaterHttpParameters(
            String configRef,
            String feedId,
            String url,
            int frequencySec
    ) {
        Objects.requireNonNull(feedId, "feedId is required");
        Objects.requireNonNull(url, "url is required");
        this.feedId = feedId;
        this.url = url;
        this.frequencySec = frequencySec;
        this.configRef = configRef;
    }

    @Override
    public int getFrequencySec() {
        return frequencySec;
    }

    @Override
    public String getConfigRef() {
        return configRef;
    }

    @Override
    public String feedId() {
        return feedId;
    }
}
