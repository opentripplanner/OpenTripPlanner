package org.opentripplanner.ext.tnc.api.routing;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.TransportationNetworkCompanyAvailabilityException;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompanyService;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import static org.opentripplanner.api.resource.TransportationNetworkCompanyResource.ACCEPTED_RIDE_TYPES;

public class TncRequestMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TncRequestMapper.class);

    private final TransportationNetworkCompanyService service;


    /**
     * TODO TNC - Inject this, but not from graph...
     *             TransportationNetworkCompanyService service = api.router.graph.getService(
     *                     TransportationNetworkCompanyService.class);
     * @param service
     */
    public TncRequestMapper(TransportationNetworkCompanyService service) {
        this.service = service;
    }

    public void mapToRequest(RoutingResource api, RoutingRequest request)
            throws ParameterException
    {
        if(OTPFeature.TncRouting.isOff())  { return;}

        // If using Transportation Network Companies, make sure service exists at origin.
        // This is not a future-proof solution as TNC coverage areas could be different in the future.  For example, a
        // trip planned months in advance may not take into account a TNC company deciding to no longer provide service
        // on that particular date in the future.  The current ETA estimate is only valid for perhaps 30 minutes into
        // the future.
        //
        // Also, if "depart at" and leaving soonish, save earliest departure time for use later use when boarding the
        // first TNC before transit.  (See StateEditor.boardHailedCar)
        if (api.modes != null && api.modes.qModes.contains(new QualifiedMode("CAR_HAIL"))) {
            if (api.companies == null) {
                throw new ParameterException(Message.TRANSPORTATION_NETWORK_COMPANY_REQUEST_INVALID);
            }

            request.companies = api.companies;

            if (service == null) {
                LOG.error("Unconfigured Transportation Network Company service for router with id: "
                        + api.routerId);
                throw new ParameterException(Message.TRANSPORTATION_NETWORK_COMPANY_CONFIG_INVALID);
            }

            List<ArrivalTime> arrivalEstimates;

            try {
                arrivalEstimates = service.getArrivalTimes(
                        request.from.lng,
                        request.from.lat,
                        api.companies
                );
            }
            catch (Exception e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(
                        "Unable to verify availability of Transportation Network Company service due to error: "
                                + e.getMessage());
            }

            // iterate through results and find earliest ETA of an acceptable ride type
            int earliestEta = Integer.MAX_VALUE;
            for (ArrivalTime arrivalEstimate : arrivalEstimates) {
                for (String rideType : ACCEPTED_RIDE_TYPES) {
                    if (arrivalEstimate.productId.equals(rideType)
                            && arrivalEstimate.estimatedSeconds < earliestEta) {
                        earliestEta = arrivalEstimate.estimatedSeconds;
                        break;
                    }
                }
            }

            if (earliestEta == Integer.MAX_VALUE) {
                // no acceptable ride types found
                throw new TransportationNetworkCompanyAvailabilityException();
            }

            // store the earliest ETA if planning a "depart at" trip that begins soonish (within + or - 30 minutes)
            long now = (new Date()).getTime() / 1000;
            long departureTimeWindow = 1800;
            if (!api.arriveBy && request.dateTime < now + departureTimeWindow
                    && request.dateTime > now - departureTimeWindow) {
                request.transportationNetworkCompanyEtaAtOrigin = earliestEta;
            }
        }

    }
}
