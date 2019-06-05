package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.ServiceJourney;

import javax.xml.bind.JAXBElement;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

// TODO OTP2 - Add Unit tests
// TODO OTP2 - This code needs cleanup
// TODO OTP2 - JavaDoc needed
/**
 * Agency id must be added when the stop is related to a line
 */
public class TripMapper {

    Trip mapServiceJourney(ServiceJourney serviceJourney, OtpTransitServiceBuilder gtfsDao, NetexImportDataIndex netexIndex){

        JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

        String lineRef = null;
        if(lineRefStruct != null){
            lineRef = lineRefStruct.getValue().getRef();
        }else if(serviceJourney.getJourneyPatternRef() != null){
                JourneyPattern journeyPattern = netexIndex.journeyPatternsById
                        .lookup(serviceJourney.getJourneyPatternRef().getValue().getRef());
            String routeRef = journeyPattern.getRouteRef().getRef();
            lineRef = netexIndex.routeById.lookup(routeRef).getLineRef().getValue().getRef();
        }

        Trip trip = new Trip();
        trip.setId(createFeedScopedId(serviceJourney.getId()));

        trip.setRoute(gtfsDao.getRoutes().get(createFeedScopedId(lineRef)));

        String serviceId = new DayTypeRefsToServiceIdAdapter(serviceJourney.getDayTypes()).getServiceId();

        trip.setServiceId(createFeedScopedId(serviceId));

        return trip;
    }
}
