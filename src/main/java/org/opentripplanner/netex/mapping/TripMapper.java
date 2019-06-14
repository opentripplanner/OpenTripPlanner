package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * This maps a NeTEx ServiceJourney to an OTP Trip. A ServiceJourney can be connected to a Line (OTP Route) in two ways.
 * Either directly from the ServiceJourney or through JourneyPattern->Route. The former has precedent over the latter.
 */
public class TripMapper {

    private EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteById;
    private HierarchicalMapById<Route> routeById;
    private HierarchicalMapById<JourneyPattern> journeyPatternsById;

    TripMapper(
            EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteById,
            HierarchicalMapById<Route> routeById,
            HierarchicalMapById<JourneyPattern> journeyPatternsById
    ) {
        this.otpRouteById = otpRouteById;
        this.routeById = routeById;
        this.journeyPatternsById = journeyPatternsById;
    }

    Trip mapServiceJourney(
            ServiceJourney serviceJourney
    ){
        // Check for direct connection to Line
        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        String lineRef = null;
        if (lineRefStruct != null){
            // Connect to Line referenced directly from ServiceJourney
            lineRef = lineRefStruct.getValue().getRef();
        } else if(serviceJourney.getJourneyPatternRef() != null){
            // Connect to Line referenced through JourneyPattern->Route
            JourneyPattern journeyPattern = journeyPatternsById
                .lookup(serviceJourney.getJourneyPatternRef().getValue().getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = routeById.lookup(routeRef).getLineRef().getValue().getRef();
        }

        Trip trip = new Trip();
        trip.setId(createFeedScopedId(serviceJourney.getId()));
        trip.setRoute(otpRouteById.get(createFeedScopedId(lineRef)));
        String serviceId = new DayTypeRefsToServiceIdAdapter(serviceJourney.getDayTypes()).getServiceId();
        trip.setServiceId(createFeedScopedId(serviceId));

        return trip;
    }
}
