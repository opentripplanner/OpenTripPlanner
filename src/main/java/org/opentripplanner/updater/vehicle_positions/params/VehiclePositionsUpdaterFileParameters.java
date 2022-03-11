package org.opentripplanner.updater.vehicle_positions.params;

import java.util.Objects;

public class VehiclePositionsUpdaterFileParameters implements VehiclePositionsUpdaterParameters {

    /**
     * Feed id that is used for the trip ids in the TripUpdates
     */
    public final String feedId;
    public final String path;
    public final int frequencySec;
    public final String configRef;

    public VehiclePositionsUpdaterFileParameters(
            String configRef,
            String feedId,
            String path,
            int frequencySec
    ) {
        Objects.requireNonNull(feedId, "feedId is required");
        Objects.requireNonNull(path, "filePath is required");
        this.feedId = feedId;
        this.path = path;
        this.frequencySec = frequencySec;
        this.configRef = configRef;
    }

    @Override
    public int getFrequencySec() {
        return frequencySec;
    }

    @Override
    public String getConfigRef() {
        return null;
    }

    @Override
    public String feedId() {
        return null;
    }
}
