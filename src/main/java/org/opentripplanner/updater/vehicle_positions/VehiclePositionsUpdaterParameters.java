package org.opentripplanner.updater.vehicle_positions;

import java.net.URI;
import java.util.Objects;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public record VehiclePositionsUpdaterParameters(String configRef, String feedId,
                                                URI url, int frequencySec)
        implements PollingGraphUpdaterParameters {

    public VehiclePositionsUpdaterParameters {
        Objects.requireNonNull(feedId, "feedId is required");
        Objects.requireNonNull(url, "url is required");
    }

    @Override
    public int getFrequencySec() {
        return frequencySec;
    }

    @Override
    public String getConfigRef() {
        return configRef;
    }
}
