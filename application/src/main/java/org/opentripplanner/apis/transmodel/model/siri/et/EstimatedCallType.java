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
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.mapping.OccupancyStatusMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.apis.transmodel.model.timetable.EmpiricalDelayType;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
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
    GraphQLOutputType sjEstimatedCallType,
    GraphQLOutputType datedServiceJourneyType,
    GraphQLOutputType empiricalDelayType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLObjectType.newObject()
      .name("EstimatedCall")
      .description(
        "List of calls on quays as part of vehicle journeys. Updated with real time information where available"
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quay")
          .type(new GraphQLNonNull(quayType))
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).getStop())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedArrivalTime")
          .description("Scheduled time of arrival at quay. Not affected by read time updated")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env -> calcTime(env, TripTimeOnDate::getScheduledArrival))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedArrivalTime")
          .type(new GraphQLNonNull(dateTimeScalar))
          .description(
            "Expected time of arrival at quay. Updated with real time information if available."
          )
          .dataFetcher(env -> calcTime(env, TripTimeOnDate::getRealtimeArrival))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("actualArrivalTime")
          .type(dateTimeScalar)
          .description(
            "Actual time of arrival at quay. Updated from real time information if available."
          )
          .dataFetcher(env -> calcTimeOptional(env, TripTimeOnDate::getActualArrival))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedDepartureTime")
          .description("Scheduled time of departure from quay. Not affected by read time updated")
          .type(new GraphQLNonNull(dateTimeScalar))
          .dataFetcher(env -> calcTime(env, TripTimeOnDate::getScheduledDeparture))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedDepartureTime")
          .type(new GraphQLNonNull(dateTimeScalar))
          .description(
            "Expected time of departure from quay. Updated with real time information if available."
          )
          .dataFetcher(env -> calcTime(env, TripTimeOnDate::getRealtimeDeparture))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("actualDepartureTime")
          .type(dateTimeScalar)
          .description(
            "Actual time of departure from quay. Updated with real time information if available."
          )
          .dataFetcher(env -> calcTimeOptional(env, TripTimeOnDate::getActualDeparture))
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
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).isTimepoint())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("realtime")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether this call has been updated with real time information.")
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).isRealtime())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("predictionInaccurate")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether the updated estimates are expected to be inaccurate.")
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).isPredictionInaccurate())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("realtimeState")
          .type(new GraphQLNonNull(EnumTypes.REALTIME_STATE))
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).getRealTimeState())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("occupancyStatus")
          .type(new GraphQLNonNull(EnumTypes.OCCUPANCY_STATUS))
          .dataFetcher(env ->
            OccupancyStatusMapper.mapStatus(((TripTimeOnDate) env.getSource()).getOccupancyStatus())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPositionInPattern")
          .type(new GraphQLNonNull(Scalars.GraphQLInt))
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).getStopPosition())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("forBoarding")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether vehicle may be boarded at quay.")
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).getPickupType().isRoutable())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("forAlighting")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether vehicle may be alighted at quay.")
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).getDropoffType().isRoutable())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("requestStop")
          .type(new GraphQLNonNull(Scalars.GraphQLBoolean))
          .description("Whether vehicle will only stop on request.")
          .dataFetcher(
            env -> ((TripTimeOnDate) env.getSource()).getDropoffType() == COORDINATE_WITH_DRIVER
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
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).isCanceledEffectively())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("date")
          .type(new GraphQLNonNull(TransmodelScalars.DATE_SCALAR))
          .description("The date the estimated call is valid for.")
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).getServiceDay())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourneyEstimatedCalls")
          .type(new GraphQLNonNull(sjEstimatedCallType))
          .description("Estimated calls for the ServiceJourney on this date.")
          .dataFetcher(DataFetchingEnvironment::getSource)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("serviceJourney")
          .type(new GraphQLNonNull(serviceJourneyType))
          .dataFetcher(env -> ((TripTimeOnDate) env.getSource()).getTrip())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("datedServiceJourney")
          .type(datedServiceJourneyType)
          .dataFetcher(env ->
            GqlUtil.getTransitService(env).getTripOnServiceDate(
              new TripIdAndServiceDate(
                env.<TripTimeOnDate>getSource().getTrip().getId(),
                env.<TripTimeOnDate>getSource().getServiceDay()
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
          .dataFetcher(env -> {
            TripTimeOnDate tripTimeOnDate = env.getSource();
            return GqlUtil.getTransitService(env).findNotices(tripTimeOnDate.getStopTimeKey());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .description("Get all relevant situations for this EstimatedCall.")
          .dataFetcher(env -> getAllRelevantAlerts(env.getSource(), GqlUtil.getTransitService(env)))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("bookingArrangements")
          .description("Booking arrangements for this EstimatedCall.")
          .type(bookingArrangementType)
          .dataFetcher(env -> env.<TripTimeOnDate>getSource().getPickupBookingInfo())
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("flexible")
      //                        .type(Scalars.GraphQLBoolean)
      //                        .description("Whether this call is part of a flexible trip. This means that arrival or departure " +
      //                                "times are not scheduled but estimated within specified operating hours.")
      //                        .dataFetcher(env -> ((TripTimeShort) env.getSource()).isFlexible())
      //                        .build())
      .build();
  }

  /// Same as [#calcTime(TripTimeOnDate, ToIntFunction)]. If the offset is `-1`, this
  /// method returns `null`.
  @Nullable
  private static Long calcTimeOptional(
    DataFetchingEnvironment env,
    ToIntFunction<TripTimeOnDate> offsetProvider
  ) {
    TripTimeOnDate instance = env.getSource();
    int offset = offsetProvider.applyAsInt(instance);
    return (offset == -1) ? null : calcTime(instance, offset);
  }

  /// Calculate the Epoch time in milliseconds from the given `instance` and `timeOffsetProvider`.
  private static long calcTime(
    DataFetchingEnvironment env,
    ToIntFunction<TripTimeOnDate> timeOffsetProvider
  ) {
    TripTimeOnDate instance = env.getSource();
    return calcTime(instance, timeOffsetProvider.applyAsInt(instance));
  }

  private static long calcTime(TripTimeOnDate instance, int offset) {
    return 1000L * (instance.getServiceDayMidnight() + offset);
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
