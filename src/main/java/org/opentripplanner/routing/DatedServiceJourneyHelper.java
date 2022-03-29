package org.opentripplanner.routing;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TripOnServiceDate;
import org.opentripplanner.model.calendar.ServiceDate;

public class DatedServiceJourneyHelper {

    /**
     * Gets a TripOnServiceDate from a timetable snapshot if it has realtime updates otherwise from
     * the graph index
     *
     * @param routingService
     * @param tripId
     * @param serviceDate
     * @return
     */
    public static TripOnServiceDate getTripOnServiceDate(
            RoutingService routingService,
            FeedScopedId tripId,
            ServiceDate serviceDate
    ) {

        var tuple = new T2<>(tripId, serviceDate);
        if (routingService.getTimetableSnapshot() != null) {
            if (routingService
                    .getTimetableSnapshot()
                    .getLastAddedTripOnServiceDateByTripIdAndServiceDate()
                    .containsKey(tuple)
            ) {

                return routingService
                        .getTimetableSnapshot()
                        .getLastAddedTripOnServiceDateByTripIdAndServiceDate()
                        .get(tuple);
            }
        }
        return routingService.getTripOnServiceDateForTripAndDay().get(tuple);
    }

    /**
     * Gets a TripOnServiceDate from a timetable snapshot if it has realtime updates otherwise from
     * the graph index
     *
     * @param routingService
     * @param datedServiceJourneyId
     * @return
     */
    public static TripOnServiceDate getTripOnServiceDate(
            RoutingService routingService,
            FeedScopedId datedServiceJourneyId
    ) {
        if (routingService
                .getTimetableSnapshot()
                .getLastAddedTripOnServiceDate()
                .containsKey(datedServiceJourneyId)
        ) {

            return routingService
                    .getTimetableSnapshot()
                    .getLastAddedTripOnServiceDate().get(datedServiceJourneyId);
        }

        return routingService
                .getTripOnServiceDateById()
                .get(datedServiceJourneyId);
    }
}
