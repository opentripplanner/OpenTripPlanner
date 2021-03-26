package org.opentripplanner.ext.transmodelapi.model.siri.et;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

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
                          (TripTimeShort) environment.getSource()
                      ).getStopId());
                        }
                    ).build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("aimedArrivalTime")
                           .description("Scheduled time of arrival at quay. Not affected by read time updated")
                           .type(gqlUtil.dateTimeScalar)
                           .dataFetcher(
                                   environment -> 1000 * (
                                       ((TripTimeShort) environment.getSource()).getServiceDay()
                                           + ((TripTimeShort) environment.getSource()).getScheduledArrival()
                                   ))
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("expectedArrivalTime")
                           .type(gqlUtil.dateTimeScalar)
                           .description("Expected time of arrival at quay. Updated with real time information if available. Will be null if an actualArrivalTime exists")
                           .dataFetcher(
                                   environment -> {
                                       TripTimeShort tripTimeShort = environment.getSource();
                                       return 1000 * (
                                           tripTimeShort.getServiceDay()
                                               + tripTimeShort.getRealtimeArrival()
                                       );
                                   })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("actualArrivalTime")
                           .type(gqlUtil.dateTimeScalar)
                           .description("Actual time of arrival at quay. Updated from real time information if available. NOT IMPLEMENTED")
                           .dataFetcher(
                                environment -> {
                                  TripTimeShort tripTimeShort = environment.getSource();
                                  if (tripTimeShort.getActualArrival() == -1) { return null; }
                                  return 1000 * (tripTimeShort.getServiceDay() +
                                    tripTimeShort.getActualArrival());
                              })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("aimedDepartureTime")
                           .description("Scheduled time of departure from quay. Not affected by read time updated")
                           .type(gqlUtil.dateTimeScalar)
                           .dataFetcher(
                                   environment -> 1000 * (
                                       ((TripTimeShort) environment.getSource()).getServiceDay()
                                           + ((TripTimeShort) environment.getSource()).getScheduledDeparture()
                                   ))
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("expectedDepartureTime")
                           .type(gqlUtil.dateTimeScalar)
                           .description("Expected time of departure from quay. Updated with real time information if available. Will be null if an actualDepartureTime exists")
                           .dataFetcher(
                                   environment -> {
                                       TripTimeShort tripTimeShort = environment.getSource();
                                       return 1000 * (
                                           tripTimeShort.getServiceDay()
                                               + tripTimeShort.getRealtimeDeparture()
                                       );
                                   })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                           .name("actualDepartureTime")
                           .type(gqlUtil.dateTimeScalar)
                           .description("Actual time of departure from quay. Updated with real time information if available. NOT IMPLEMENTED")
                           .dataFetcher(
                                environment -> {
                                    TripTimeShort tripTimeShort = environment.getSource();
                                    if (tripTimeShort.getActualDeparture() == -1) { return null; }
                                    return 1000 * (tripTimeShort.getServiceDay() +
                                        tripTimeShort.getActualDeparture());
                              })
                           .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("timingPoint")
                    .type(Scalars.GraphQLBoolean)
                    .description("Whether this is a timing point or not. Boarding and alighting is not allowed at timing points.")
                    .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).isTimepoint())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("realtime")
                    .type(Scalars.GraphQLBoolean)
                    .description("Whether this call has been updated with real time information.")
                    .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).isRealtime())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("predictionInaccurate")
                    .type(Scalars.GraphQLBoolean)
                    .description("Whether the updated estimates are expected to be inaccurate. NOT IMPLEMENTED")
                    .dataFetcher(environment -> false)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("realtimeState")
                    .type(EnumTypes.REALTIME_STATE)
                    .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getRealtimeState())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("forBoarding")
                .type(Scalars.GraphQLBoolean)
                .description("Whether vehicle may be boarded at quay.")
                .dataFetcher(environment -> {
                    if (((TripTimeShort) environment.getSource()).getPickupType() >= 0) {
                        //Realtime-updated
                        return ((TripTimeShort) environment.getSource()).getPickupType() != PICKDROP_NONE;
                    }
                  return GqlUtil.getRoutingService(environment).getPatternForTrip()
                        .get(((TripTimeShort) environment.getSource()).getTrip())
                        .getBoardType(((TripTimeShort) environment.getSource()).getStopIndex()) != PICKDROP_NONE;
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("forAlighting")
                .type(Scalars.GraphQLBoolean)
                .description("Whether vehicle may be alighted at quay.")
                .dataFetcher(environment -> {
                    if (((TripTimeShort) environment.getSource()).getDropoffType() >= 0) {
                        //Realtime-updated
                        return ((TripTimeShort) environment.getSource()).getDropoffType() != PICKDROP_NONE;
                    }
                    return GqlUtil.getRoutingService(environment).getPatternForTrip()
                            .get(((TripTimeShort) environment.getSource()).getTrip())
                            .getAlightType(((TripTimeShort) environment.getSource()).getStopIndex()) != PICKDROP_NONE;
                })
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("requestStop")
                    .type(Scalars.GraphQLBoolean)
                    .description("Whether vehicle will only stop on request.")
                    .dataFetcher(environment ->
                        GqlUtil.getRoutingService(environment).getPatternForTrip()
                            .get(((TripTimeShort) environment.getSource()).getTrip())
                            .getAlightType(((TripTimeShort) environment.getSource()).getStopIndex()) == PICKDROP_COORDINATE_WITH_DRIVER)
                    .build())

            .field(GraphQLFieldDefinition
                    .newFieldDefinition()
                    .name("cancellation")
                    .type(Scalars.GraphQLBoolean)
                    .description("Whether stop is cancelled.")
                    .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).isCanceledEffectively())
            .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("date")
                    .type(gqlUtil.dateScalar)
                    .description("The date the estimated call is valid for.")
                    .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getServiceDay())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("serviceJourney")
                    .type(serviceJourneyType)
                    .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getTrip())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("destinationDisplay")
                    .type(destinationDisplayType)
                    .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).getHeadsign())
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("notices")
                    .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                    .dataFetcher(environment -> {
                        // TODO OTP2 - Fix it!
                        //TripTimeShort tripTimeShort = environment.getSource();
                        return Collections.emptyList(); //index.getNoticesByEntity(tripTimeShort.stopTimeId);
                    })
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("situations")
                    .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                    .description("Get all relevant situations for this EstimatedCall.")
                    .dataFetcher(environment -> {
                      return getAllRelevantAlerts(environment.getSource(),
                          GqlUtil.getRoutingService(environment)
                      );
                    })
                    .build())
             .field(GraphQLFieldDefinition.newFieldDefinition()
                     .name("bookingArrangements")
                     .description("Booking arrangements for flexible service. NOT IMPLEMENTED")
                     .dataFetcher(environment ->  null)
                     .type(bookingArrangementType)
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
      TripTimeShort tripTimeShort,
      RoutingService routingService
  ) {
    Trip trip = tripTimeShort.getTrip();
    FeedScopedId tripId = trip.getId();
    FeedScopedId routeId = trip.getRoute().getId();

    FeedScopedId stopId = tripTimeShort.getStopId();

    Stop stop = routingService.getStopForId(stopId);
    FeedScopedId parentStopId = stop.getParentStation().getId();

    Collection<TransitAlert> allAlerts = new HashSet<>();

    TransitAlertService alertPatchService = routingService.getTransitAlertService();

    // Quay
    allAlerts.addAll(alertPatchService.getStopAlerts(stopId));
    allAlerts.addAll(alertPatchService.getStopAndTripAlerts(stopId, tripId));
    allAlerts.addAll(alertPatchService.getStopAndRouteAlerts(stopId, routeId));
    // StopPlace
    allAlerts.addAll(alertPatchService.getStopAlerts(parentStopId));
    allAlerts.addAll(alertPatchService.getStopAndTripAlerts(parentStopId, tripId));
    allAlerts.addAll(alertPatchService.getStopAndRouteAlerts(parentStopId, routeId));
    // Trip
    allAlerts.addAll(alertPatchService.getTripAlerts(tripId));
    // Route
    allAlerts.addAll(alertPatchService.getRouteAlerts(routeId));
    // Agency
    // TODO OTP2 This should probably have a FeedScopeId argument instead of string
    allAlerts.addAll(alertPatchService.getAgencyAlerts(trip.getRoute().getAgency().getId()));
    // TripPattern
    allAlerts.addAll(alertPatchService.getTripPatternAlerts(routingService.getPatternForTrip().get(trip).getId()));

    long serviceDayMillis = 1000 * tripTimeShort.getServiceDay();
    long arrivalMillis = 1000 * tripTimeShort.getRealtimeArrival();
    long departureMillis = 1000 * tripTimeShort.getRealtimeDeparture();

    filterSituationsByDateAndStopConditions(allAlerts,
        new Date(serviceDayMillis + arrivalMillis),
        new Date(serviceDayMillis + departureMillis),
        Arrays.asList(StopCondition.STOP, StopCondition.START_POINT, StopCondition.EXCEPTIONAL_STOP));

    return allAlerts;
  }

  private static void filterSituationsByDateAndStopConditions(Collection<TransitAlert> alertPatches, Date fromTime, Date toTime, List<StopCondition> stopConditions) {
    if (alertPatches != null) {

      // First and last period
      alertPatches.removeIf(alert -> alert.getEffectiveStartDate().after(toTime) ||
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
