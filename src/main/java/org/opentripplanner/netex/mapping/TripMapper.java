package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;

/**
 * Agency id must be added when the stop is related to a line
 */

public class TripMapper {

    public Trip mapServiceJourney(ServiceJourney serviceJourney, OtpTransitServiceBuilder gtfsDao, NetexDao netexDao){

        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        String lineRef = null;
        if(lineRefStruct != null){
            lineRef = lineRefStruct.getValue().getRef();
        }else if(serviceJourney.getJourneyPatternRef() != null){
            JourneyPattern journeyPattern = netexDao.lookupJourneyPatternById(serviceJourney.getJourneyPatternRef().getValue().getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = netexDao.lookupRouteById(routeRef).getLineRef().getValue().getRef();
        }

        Trip trip = new Trip();
        trip.setId(FeedScopedIdFactory.createFeedScopedId(serviceJourney.getId()));

        trip.setRoute(gtfsDao.getRoutes().get(FeedScopedIdFactory.createFeedScopedId(lineRef)));

        String serviceId = ServiceIdMapper.mapToServiceId(serviceJourney.getDayTypes());

        // Add all unique service ids to map. Used when mapping calendars later.
        netexDao.addCalendarServiceId(serviceId);

        trip.setServiceId(FeedScopedIdFactory.createFeedScopedId(serviceId));

        return trip;
    }
}
