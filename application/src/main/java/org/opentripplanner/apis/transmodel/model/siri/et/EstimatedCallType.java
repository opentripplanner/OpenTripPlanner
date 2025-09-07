package org.opentripplanner.apis.transmodel.model.siri.et;

import static org.opentripplanner.model.PickDrop.COORDINATE_WITH_DRIVER;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.apis.transmodel.mapping.OccupancyStatusMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.apis.transmodel.model.timetable.EmpiricalDelayType;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.service.TransitService;

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
    GraphQLOutputType datedServiceJourneyType,
    GraphQLOutputType empiricalDelayType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLObjectType.newObject()
      .name("EstimatedCall")
      .description(
        "List of visits to quays as part of vehicle journeys. Updated with real time information where available"
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .type(new GraphQLNonNull(quayType))
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getStop())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedArrivalTime")
          .description("Scheduled time of arrival at quay. Not affected by read time updated")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(
            environment ->
              1000 *
              (((TripTimeOnDate) environment.getSource()).getServiceDayMidnight() +
                ((TripTimeOnDate) environment.getSource()).getScheduledArrival())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedArrivalTime")
          .type(new GraphQLNonNull(dateTimeScalar))
          .description(
            "Expected time of arrival at quay. Updated with real time information if available. Will be null if an actualArrivalTime exists"
          )
          .dataFetcher(environment -> {
            TripTimeOnDate tripTimeOnDate = environment.getSource();
            return (
              1000 * (tripTimeOnDate.getServiceDayMidnight() + tripTimeOnDate.getRealtimeArrival())
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("actualArrivalTime")
          .type(dateTimeScalar)
          .description(
            "Actual time of arrival at quay. Updated from real time information if available."
          )
          .dataFetcher(environment -> {
            TripTimeOnDate tripTimeOnDate = environment.getSource();
            if (tripTimeOnDate.getActualArrival() == -1) {
              return null;
            }
            return (
              1000 * (tripTimeOnDate.getServiceDayMidnight() + tripTimeOnDate.getActualArrival())
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedDepartureTime")
          .description("Scheduled time of departure from quay. Not affected by read time updated")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(
            environment ->
              1000 *
              (((TripTimeOnDate) environment.getSource()).getServiceDayMidnight() +
                ((TripTimeOnDate) environment.getSource()).getScheduledDeparture())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedDepartureTime")
          .type(new GraphQLNonNull(dateTimeScalar))
          .description(
            "Expected time of departure from quay. Updated with real time information if available. Will be null if an actualDepartureTime exists"
          )
          .dataFetcher(environment -> {
            TripTimeOnDate tripTimeOnDate = environment.getSource();
            return (
              1000 *
              (tripTimeOnDate.getServiceDayMidnight() + tripTimeOnDate.getRealtimeDeparture())
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("actualDepartureTime")
          .type(dateTimeScalar)
          .description(
            "Actual time of departure from quay. Updated with real time information if available."
          )
          .dataFetcher(environment -> {
            TripTimeOnDate tripTimeOnDate = environment.getSource();
            if (tripTimeOnDate.getActualDeparture() == -1) {
              return null;
            }
            return (
              1000 * (tripTimeOnDate.getServiceDayMidnight() + tripTimeOnDate.getActualDeparture())
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("empiricalDelay")
          .type(empiricalDelayType)
          .description(
            "The typical delay for this trip on this day for this stop based on historical data."
          )
          .dataFetcher(EmpiricalDelayType::dataFetcherForTripTimeOnDate)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timingPoint")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description(
            "Whether this is a timing point or not. Boarding and alighting is not allowed at timing points."
          )
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).isTimepoint())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("realtime")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether this call has been updated with real time information.")
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).isRealtime())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("predictionInaccurate")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether the updated estimates are expected to be inaccurate.")
          .dataFetcher(environment ->
            ((TripTimeOnDate) environment.getSource()).isPredictionInaccurate()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("realtimeState")
          .type(new GraphQLNonNull(EnumTypes.REALTIME_STATE))
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getRealTimeState())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("occupancyStatus")
          .type(new GraphQLNonNull(EnumTypes.OCCUPANCY_STATUS))
          .dataFetcher(environment ->
            OccupancyStatusMapper.mapStatus(
              ((TripTimeOnDate) environment.getSource()).getOccupancyStatus()
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPositionInPattern")
          .type(new GraphQLNonNull(Scalars.GraphQLInt))
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getStopPosition())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("forBoarding")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether vehicle may be boarded at quay.")
          .dataFetcher(environment ->
            ((TripTimeOnDate) environment.getSource()).getPickupType().isRoutable()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("forAlighting")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether vehicle may be alighted at quay.")
          .dataFetcher(environment ->
            ((TripTimeOnDate) environment.getSource()).getDropoffType().isRoutable()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("requestStop")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether vehicle will only stop on request.")
          .dataFetcher(
            environment ->
              ((TripTimeOnDate) environment.getSource()).getDropoffType() == COORDINATE_WITH_DRIVER
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("cancellation")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description(
            "Whether stop is cancelled. This means that either the " +
            "ServiceJourney has a planned cancellation, the ServiceJourney has been " +
            "cancelled by real-time data, or this particular StopPoint has been " +
            "cancelled. This also means that both boarding and alighting has been " +
            "cancelled."
          )
          .dataFetcher(environment ->
            ((TripTimeOnDate) environment.getSource()).isCanceledEffectively()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("date")
          .type(new GraphQLNonNull(TransmodelScalars.DATE_SCALAR))
          .description("The date the estimated call is valid for.")
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getServiceDay())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourney")
          .type(new GraphQLNonNull(serviceJourneyType))
          .dataFetcher(environment -> ((TripTimeOnDate) environment.getSource()).getTrip())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("datedServiceJourney")
          .type(datedServiceJourneyType)
          .dataFetcher(environment ->
            GqlUtil.getTransitService(environment).getTripOnServiceDate(
              new TripIdAndServiceDate(
                environment.<TripTimeOnDate>getSource().getTrip().getId(),
                environment.<TripTimeOnDate>getSource().getServiceDay()
              )
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("destinationDisplay")
          .type(destinationDisplayType)
          .dataFetcher(DataFetchingEnvironment::getSource)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("notices")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(noticeType))))
          .dataFetcher(environment -> {
            TripTimeOnDate tripTimeOnDate = environment.getSource();
            return GqlUtil.getTransitService(environment).findNotices(
              tripTimeOnDate.getStopTimeKey()
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .description("Get all relevant situations for this EstimatedCall.")
          .dataFetcher(environment ->
            getAllRelevantAlerts(environment.getSource(), GqlUtil.getTransitService(environment))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bookingArrangements")
          .description("Booking arrangements for this EstimatedCall.")
          .type(bookingArrangementType)
          .dataFetcher(environment ->
            environment.<TripTimeOnDate>getSource().getPickupBookingInfo()
          )
          .build()
      )
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
   * Resolves all AlertPatches that are relevant for the supplied TripTimeOnDate.
   */
  private static Collection<TransitAlert> getAllRelevantAlerts(
    TripTimeOnDate tripTimeOnDate,
    TransitService transitService
  ) {
    Trip trip = tripTimeOnDate.getTrip();
    FeedScopedId tripId = trip.getId();
    FeedScopedId routeId = trip.getRoute().getId();

    StopLocation stop = tripTimeOnDate.getStop();
    FeedScopedId stopId = stop.getId();

    Collection<TransitAlert> allAlerts = new HashSet<>();

    TransitAlertService alertPatchService = transitService.getTransitAlertService();

    final LocalDate serviceDate = tripTimeOnDate.getServiceDay();

    Set<StopCondition> stopConditions = Set.of(
      StopCondition.STOP,
      StopCondition.START_POINT,
      StopCondition.EXCEPTIONAL_STOP
    );

    // Quay
    allAlerts.addAll(alertPatchService.getStopAlerts(stopId, stopConditions));
    allAlerts.addAll(
      alertPatchService.getStopAndTripAlerts(stopId, tripId, serviceDate, stopConditions)
    );
    allAlerts.addAll(alertPatchService.getStopAndRouteAlerts(stopId, routeId, stopConditions));
    // StopPlace
    if (stop.getParentStation() != null) {
      FeedScopedId parentStopId = stop.getParentStation().getId();
      allAlerts.addAll(alertPatchService.getStopAlerts(parentStopId, stopConditions));
      allAlerts.addAll(
        alertPatchService.getStopAndTripAlerts(parentStopId, tripId, serviceDate, stopConditions)
      );
      allAlerts.addAll(
        alertPatchService.getStopAndRouteAlerts(parentStopId, routeId, stopConditions)
      );
    }
    // Trip
    allAlerts.addAll(alertPatchService.getTripAlerts(tripId, serviceDate));
    // Route
    allAlerts.addAll(alertPatchService.getRouteAlerts(routeId));
    // Agency
    // TODO OTP2 This should probably have a FeedScopeId argument instead of string
    allAlerts.addAll(alertPatchService.getAgencyAlerts(trip.getRoute().getAgency().getId()));
    // Route's direction
    allAlerts.addAll(alertPatchService.getDirectionAndRouteAlerts(trip.getDirection(), routeId));

    long serviceDay = tripTimeOnDate.getServiceDayMidnight();
    long arrivalTime = tripTimeOnDate.getRealtimeArrival();
    long departureTime = tripTimeOnDate.getRealtimeDeparture();

    filterSituationsByDateAndStopConditions(
      allAlerts,
      Instant.ofEpochSecond(serviceDay + arrivalTime),
      Instant.ofEpochSecond(serviceDay + departureTime)
    );

    return allAlerts;
  }

  private static void filterSituationsByDateAndStopConditions(
    Collection<TransitAlert> alertPatches,
    Instant fromTime,
    Instant toTime
  ) {
    if (alertPatches != null) {
      // First and last period
      alertPatches.removeIf(
        alert ->
          (alert.getEffectiveStartDate() != null &&
            alert.getEffectiveStartDate().isAfter(toTime)) ||
          (alert.getEffectiveEndDate() != null && alert.getEffectiveEndDate().isBefore(fromTime))
      );

      // Handle repeating validityPeriods
      alertPatches.removeIf(alertPatch ->
        !alertPatch.displayDuring(fromTime.getEpochSecond(), toTime.getEpochSecond())
      );
    }
  }
}
