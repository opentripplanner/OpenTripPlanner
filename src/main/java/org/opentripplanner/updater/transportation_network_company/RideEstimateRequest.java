package org.opentripplanner.updater.transportation_network_company;

import java.util.Objects;

public class RideEstimateRequest {

    public Position startPosition;
    public Position endPosition;

    public RideEstimateRequest(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        this.startPosition = new Position(startLatitude, startLongitude);
        this.endPosition = new Position(endLatitude, endLongitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideEstimateRequest request = (RideEstimateRequest) o;
        return startPosition.equals(request.startPosition) &&
            endPosition.equals(request.endPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            startPosition.getIntLat(),
            startPosition.getIntLon(),
            endPosition.getIntLat(),
            endPosition.getIntLon()
        );
    }
}
