package org.opentripplanner.updater.vehicle_positions;

import java.net.URI;
import java.util.Objects;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class VehiclePositionsUpdaterParameters implements PollingGraphUpdaterParameters {

    /**
     * Feed id that is used for the trip ids in the TripUpdates
     */
    final public String feedId;
    final public URI url;
    final public int frequencySec;
    final String configRef;

    public VehiclePositionsUpdaterParameters(
            String configRef,
            String feedId,
            URI url,
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

    public String feedId() {
        return feedId;
    }
}
