package org.opentripplanner.ext.transmodelapi.model.siri.et;

import static org.opentripplanner.model.PickDrop.COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.PickDrop.NONE;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;

public class EstimatedCallType {
  private static final String NAME = "EstimatedCall";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
      GraphQLOutputType bookingArrangementType,
      GraphQLOutputType noticeType,
      GraphQLOutputType quayType,
      GraphQLOutputType destinationDisplayType,
      GraphQLOutputType ptSituationElementType,
      GraphQLOutputType serviceJourneyType,
      GqlUtil gqlUtil
  ) {
    return GraphQLObjectType.newObject()
            .name("EstimatedCall")
            .description("List of visits to quays as part of vehicle journeys. Updated with real time information where available")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("quay")
                    .type(quayType)
                    .dataFetcher(environment -> {
                      return GqlUtil.getRoutingService(environment).getStopForId((
                          (TripTimeOnDate) environment.getSource()
                      ).getStopId());
                        }
                    ).build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("aimedArrivalTime")
                           .description("Scheduled time of arrival at quay. Not affected by read time updated")
                           .type(new GraphQLNonNull(gqlUtil.dateTimeScalar))
                           .dataFetcher(
                                   environment -> 1000 * (
                                       ((TripTimeOnDate) environment.getSource()).getServiceDay()
                                           + ((TripTimeOnDate) environment.getSource()).getScheduledArrival()
                                   ))
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("expectedArrivalTime")
                           .type(new GraphQLNonNull(gqlUtil.dateTimeScalar))
                           .description("Expected time of arrival at quay. Updated with real time information if available. Will be null if an actualArrivalTime exists")
                           .dataFetcher(
                                   environment -> {
                                       TripTimeOnDate tripTimeOnDate = environment.getSource();
                                       return 1000 * (
                                           tripTimeOnDate.getServiceDay()
                                               + tripTimeOnDate.getRealtimeArrival()
                                       );
                                   })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("actualArrivalTime")
                           .type(gqlUtil.dateTimeScalar)
                           .description("Actual time of arrival at quay. Updated from real time information if available. NOT IMPLEMENTED")
                           .dataFetcher(
                                environment -> {
                                  TripTimeOnDate tripTimeOnDate = environment.getSource();
                                  if (tripTimeOnDate.getActualArrival() == -1) { return null; }
                                  return 1000 * (
                                      tripTimeOnDate.getServiceDay() +
                                    tripTimeOnDate.getActualArrival());
                              })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("aimedDepartureTime")
                           .description("Scheduled time of departure from quay. Not affected by read time updated")
                           .type(new GraphQLNonNull(gqlUtil.dateTimeScalar))
                           .dataFetcher(
                                   environment -> 1000 * (
                                       ((TripTimeOnDate) environment.getSource()).getServiceDay()
                                           + ((TripTimeOnDate) environment.getSource()).getScheduledDeparture()
                                   ))
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("expectedDepartureTime")
                           .type(new GraphQLNonNull(gqlUtil.dateTimeScalar))
                           .description("Expected time of departure from quay. Updated with real time information if available. Will be null if an actualDepartureTime exists")
                           .dataFetcher(
                                   environment -> {
                                       TripTimeOnDate tripTimeOnDate = environment.getSource();
                                       return 1000 * (
                                           tripTimeOnDate.getServiceDay()
                                               + tripTimeOnDate.getRealtimeDeparture()
                                       );
                                   })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("actualDepartureTime")
                           .type(gqlUtil.dateTimeScalar)
                           .description("Actual time of departure from quay. Updated with real time information if available. NOT IMPLEMENTED")
                           .dataFetcher(
                                environment -> {
                                    TripTimeOnDate tripTimeOnDate = environment.getSource();
                                    if (tripTimeOnDate.getActualDeparture() == -1) { return null; }
                                    return 1000 * (
                                        tripTimeOnDate.getServiceDay() +
                                        tripTimeOnDate.getActualDeparture());
                              })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("timingPoint")
                    .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
                    .description("Whether this is a timing point or not. Boarding and alighting is not allowed at timing points.")
                    .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).isTimepoint())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("realtime")
                    .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
                    .description("Whether this call has been updated with real time information.")
                    .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).isRealtime())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("predictionInaccurate")
                    .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
                    .description("Whether the updated estimates are expected to be inaccurate. NOT IMPLEMENTED")
                    .dataFetcher(environment -> false)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("realtimeState")
                    .type(new GraphQLNonNull(EnumTypes.REALTIME_STATE))
                    .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getRealtimeState())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("stopPositionInPattern")
                    .type(Scalars.GraphQLInt)
                    .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getStopIndex())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("forBoarding")
                .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
                .description("Whether vehicle may be boarded at quay according to the planned data. "
                    + "If the cancellation flag is set, boarding is not possible, even if this field "
                    + "is set to true.")
                .dataFetcher(environment ->
                    ((TripTimeOnDate) environment.getSource()).getPickupType() != NONE
                ).build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("forAlighting")
                .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
                .description("Whether vehicle may be alighted at quay according to the planned data. "
                    + "If the cancellation flag is set, alighting is not possible, even if this field "
                    + "is set to true.")
                .dataFetcher(environment ->
                    ((TripTimeOnDate) environment.getSource()).getDropoffType() != NONE
                ).build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("requestStop")
                    .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
                    .description("Whether vehicle will only stop on request.")
                    .dataFetcher(environment ->
                        GqlUtil.getRoutingService(environment).getPatternForTrip()
                            .get(((TripTimeOnDate) environment.getSource()).getTrip())
                            .getAlightType(((TripTimeOnDate) environment.getSource()).getStopIndex()) == COORDINATE_WITH_DRIVER)
                    .build())

            .field(GraphQLFieldDefinition
                    .newFieldDefinition()
                    .name("cancellation")
                    .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
                    .description("Whether stop is cancelled. This means that either the "
                        + "ServiceJourney has a planned cancellation, the ServiceJourney has been "
                        + "cancelled by realtime data, or this particular StopPoint has been "
                        + "cancelled. This also means that both boarding and alighting has been "
                        + "cancelled.")
                    .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).isCanceledEffectively())
            .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("date")
                    .type(gqlUtil.dateScalar)
                    .description("The date the estimated call is valid for.")
                    .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getServiceDay())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("serviceJourney")
                    .type(serviceJourneyType)
                    .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getTrip())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("destinationDisplay")
                    .type(destinationDisplayType)
                    .dataFetcher(DataFetchingEnvironment::getSource)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(noticeType))))
                    .dataFetcher(environment -> {
                        TripTimeOnDate tripTimeOnDate = environment.getSource();
                        return GqlUtil.getRoutingService(environment).getNoticesByEntity(
                            tripTimeOnDate.getStopTimeKey());
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("situations")
                    .withDirective(gqlUtil.timingData)
                    .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
                    .description("Get all relevant situations for this EstimatedCall.")
                    .dataFetcher(environment -> {
                      return getAllRelevantAlerts(environment.getSource(),
                          GqlUtil.getRoutingService(environment)
                      );
                    })
                    .build())
             .field(GraphQLFieldDefinition.newFieldDefinition()
                     .name("bookingArrangements")
                     .description("Booking arrangements for this EstimatedCall.")
                     .type(bookingArrangementType)
                     .dataFetcher(environment ->
                             environment.<TripTimeOnDate>getSource().getPickupBookingInfo())
                     .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("flexible")
//                        .type(Scalars.GraphQLBoolean)
//                        .description("Whether this call is part of a flexible trip. This means that arrival or departure " +
//                                "times are not scheduled but estimated within specified operating hours.")
//                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).isFlexible())
//                        .build())
            .build();
  }


  /**
   * Resolves all AlertPatches that are relevant for the supplied TripTimeShort.
   */
  private static Collection<TransitAlert> getAllRelevantAlerts(
      TripTimeOnDate tripTimeOnDate,
      RoutingService routingService
  ) {
    Trip trip = tripTimeOnDate.getTrip();
    FeedScopedId tripId = trip.getId();
    FeedScopedId routeId = trip.getRoute().getId();

    FeedScopedId stopId = tripTimeOnDate.getStopId();

    var stop = routingService.getStopForId(stopId);
    FeedScopedId parentStopId = stop.getParentStation().getId();

    Collection<TransitAlert> allAlerts = new HashSet<>();

    TransitAlertService alertPatchService = routingService.getTransitAlertService();

    final ServiceDate serviceDate = new ServiceDate(LocalDate.ofEpochDay(1+tripTimeOnDate.getServiceDay()/(24*3600)));

    // Quay
    allAlerts.addAll(alertPatchService.getStopAlerts(stopId));
    allAlerts.addAll(alertPatchService.getStopAndTripAlerts(stopId, tripId, serviceDate));
    allAlerts.addAll(alertPatchService.getStopAndRouteAlerts(stopId, routeId));
    // StopPlace
    allAlerts.addAll(alertPatchService.getStopAlerts(parentStopId));
    allAlerts.addAll(alertPatchService.getStopAndTripAlerts(parentStopId, tripId, serviceDate));
    allAlerts.addAll(alertPatchService.getStopAndRouteAlerts(parentStopId, routeId));
    // Trip
    allAlerts.addAll(alertPatchService.getTripAlerts(tripId, serviceDate));
    // Route
    allAlerts.addAll(alertPatchService.getRouteAlerts(routeId));
    // Agency
    // TODO OTP2 This should probably have a FeedScopeId argument instead of string
    allAlerts.addAll(alertPatchService.getAgencyAlerts(trip.getRoute().getAgency().getId()));
    // Route's direction
      allAlerts.addAll(
              alertPatchService.getDirectionAndRouteAlerts(trip.getDirection().gtfsCode, routeId));

    long serviceDayMillis = 1000L * tripTimeOnDate.getServiceDay();
    long arrivalMillis = 1000L * tripTimeOnDate.getRealtimeArrival();
    long departureMillis = 1000L * tripTimeOnDate.getRealtimeDeparture();

    filterSituationsByDateAndStopConditions(allAlerts,
        new Date(serviceDayMillis + arrivalMillis),
        new Date(serviceDayMillis + departureMillis),
        Arrays.asList(StopCondition.STOP, StopCondition.START_POINT, StopCondition.EXCEPTIONAL_STOP));

    return allAlerts;
  }

  private static void filterSituationsByDateAndStopConditions(Collection<TransitAlert> alertPatches, Date fromTime, Date toTime, List<StopCondition> stopConditions) {
    if (alertPatches != null) {

      // First and last period
      alertPatches.removeIf(alert -> (alert.getEffectiveStartDate() != null && alert.getEffectiveStartDate().after(toTime)) ||
          (alert.getEffectiveEndDate() != null && alert.getEffectiveEndDate().before(fromTime)));

      // Handle repeating validityPeriods
      alertPatches.removeIf(alertPatch -> !alertPatch.displayDuring(fromTime.getTime()/1000, toTime.getTime()/1000));

      alertPatches.removeIf(alert -> {
        boolean removeByStopCondition = false;

        if (!alert.getStopConditions().isEmpty()) {
          removeByStopCondition = true;
          for (StopCondition stopCondition : stopConditions) {
            if (alert.getStopConditions().contains(stopCondition)) {
              removeByStopCondition = false;
            }
          }
        }
        return removeByStopCondition;
      });
    }
  }
}
