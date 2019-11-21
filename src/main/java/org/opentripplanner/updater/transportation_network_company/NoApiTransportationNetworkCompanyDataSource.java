package org.opentripplanner.updater.transportation_network_company;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This data source is to model a transportation network company for which no API exists to calculate real-time arrival
 * estimates or ride estimates. The config for this updater can include a default value for the estimated arrival time
 * which will always be provided when estimates for the arrival time are desired.
 */
public class NoApiTransportationNetworkCompanyDataSource extends TransportationNetworkCompanyDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(NoApiTransportationNetworkCompanyDataSource.class);

    /**
     * The default arrival time in seconds to respond with for all valid arrival time estimate requests. Defaults to 0.
     */
    private int defaultArrivalTimeSeconds = 0;

    /**
     * Whether or not the TNC service being modeled is wheelchair accessible. Defaults to false.
     */
    private boolean isWheelChairAccessible = false;

    /**
     * Configures the data source. At this point, the only values the config could contain are the following:
     * - defaultArrivalTimeSeconds
     * - isWheelChairAccessible
     */
    public NoApiTransportationNetworkCompanyDataSource(JsonNode config) {
        defaultArrivalTimeSeconds = config.path("defaultArrivalTimeSeconds").asInt();
        isWheelChairAccessible = config.path("isWheelChairAccessible").asBoolean();
    }

    /**
     * For testing purposes only.
     */
    public NoApiTransportationNetworkCompanyDataSource() {
        defaultArrivalTimeSeconds = 123;
        isWheelChairAccessible = true;
    }

    @Override public TransportationNetworkCompany getTransportationNetworkCompanyType() {
        return TransportationNetworkCompany.NOAPI;
    }

    /**
     * In lieu of making an API request, this will merely return a single arrival estimate with the default arrive time.
     * An idea for improvement of this could be to add the ability to parse GeoJSON representing the service area of
     * the TNC provider in order to be able to determine if TNC service is unavailable at the given position.
     */
    @Override protected List<ArrivalTime> queryArrivalTimes(Position position) throws IOException {
        List arrivalEstimates = new ArrayList();
        arrivalEstimates.add(new ArrivalTime(
            TransportationNetworkCompany.NOAPI,
            "no-api-tnc-service",
            "no-api-tnc-service",
            defaultArrivalTimeSeconds,
            isWheelChairAccessible

        ));
        return arrivalEstimates;
    }

    /**
     * In lieu of making an API request, this will merely return a single ride estimate with 0 duration and cost, but
     * with the wheelchair accessibility type that is possibly defined in the config.
     */
    @Override protected List<RideEstimate> queryRideEstimates(RideEstimateRequest request) throws IOException {
        List rideEstimates = new ArrayList();
        rideEstimates.add(new RideEstimate(
            TransportationNetworkCompany.NOAPI,
            "undefined",
            0,
            0,
            0,
            "no-api-tnc-service",
            isWheelChairAccessible
        ));
        return rideEstimates;
    }
}
