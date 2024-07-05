package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.AssertException;
import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.TripTimesShortHelper;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

public class ServiceJourneyType {

  private static final String NAME = "ServiceJourney";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
    GraphQLOutputType bookingArrangementType,
    GraphQLOutputType linkGeometryType,
    GraphQLOutputType operatorType,
    GraphQLOutputType noticeType,
    GraphQLOutputType quayType,
    GraphQLOutputType lineType,
    GraphQLOutputType ptSituationElementType,
    GraphQLOutputType journeyPatternType,
    GraphQLOutputType estimatedCallType,
    GraphQLOutputType timetabledPassingTimeType,
    GqlUtil gqlUtil
  ) {
    return GraphQLObjectType
      .newObject()
      .name(NAME)
      .description("A planned vehicle journey with passengers.")
      .field(GqlUtil.newTransitIdField())
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("line")
          .type(new GraphQLNonNull(lineType))
          .dataFetcher(environment -> (trip(environment)).getRoute())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("activeDates")
          .withDirective(gqlUtil.timingData)
          .type(new GraphQLNonNull(new GraphQLList(gqlUtil.dateScalar)))
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getCalendarService()
              .getServiceDatesForServiceId(((trip(environment)).getServiceId()))
              .stream()
              .sorted()
              .collect(Collectors.toList())
          )
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("serviceAlteration")
      //                        .type(serviceAlterationEnum)
      //                        .description("Whether journey is as planned, a cancellation or an extra journey. Default is as planned")
      //                        .dataFetcher(environment -> (((Trip) trip(environment)).getServiceAlteration()))
      //                        .build())

      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("transportMode")
          .type(EnumTypes.TRANSPORT_MODE)
          .dataFetcher(environment -> ((trip(environment)).getMode()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("transportSubmode")
          .type(EnumTypes.TRANSPORT_SUBMODE)
          .dataFetcher(environment ->
            TransmodelTransportSubmode.fromValue(((trip(environment))).getNetexSubMode())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("publicCode")
          .type(Scalars.GraphQLString)
          .description(
            "Publicly announced code for service journey, differentiating it from other service journeys for the same line."
          )
          .dataFetcher(environment -> ((trip(environment)).getShortName()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("privateCode")
          .type(Scalars.GraphQLString)
          .description("For internal use by operators.")
          .dataFetcher(environment -> ((trip(environment)).getNetexInternalPlanningCode()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("operator")
          .type(operatorType)
          .dataFetcher(environment -> ((trip(environment)).getOperator()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("directionType")
          .type(EnumTypes.DIRECTION_TYPE)
          .dataFetcher(environment -> trip(environment).getDirection())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("serviceAlteration")
          .deprecate(
            "The service journey alteration will be moved out of SJ and grouped " +
            "together with the SJ and date. In Netex this new type is called " +
            "DatedServiceJourney. We will create artificial DSJs for the old SJs."
          )
          .type(EnumTypes.SERVICE_ALTERATION)
          .dataFetcher(environment -> trip(environment).getNetexAlteration())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("wheelchairAccessible")
          .type(EnumTypes.WHEELCHAIR_BOARDING)
          .dataFetcher(environment -> trip(environment).getWheelchairBoarding())
          .description("Whether service journey is accessible with wheelchair.")
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bikesAllowed")
          .type(EnumTypes.BIKES_ALLOWED)
          .description("Whether bikes are allowed on service journey.")
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("journeyPattern")
          .description(
            "JourneyPattern for the service journey, according to scheduled data. If the " +
            "ServiceJourney is not included in the scheduled data, null is returned."
          )
          .type(journeyPatternType)
          .dataFetcher(env -> GqlUtil.getTransitService(env).getPatternForTrip(trip(env)))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("quays")
          .description(
            "Quays visited by service journey, according to scheduled data. If the " +
            "ServiceJourney is not included in the scheduled data, an empty list is returned."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("first")
              .description("Only fetch the first n quays on the service journey")
              .type(Scalars.GraphQLInt)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("last")
              .description("Only fetch the last n quays on the service journey")
              .type(Scalars.GraphQLInt)
              .build()
          )
          .dataFetcher(environment -> {
            Integer first = environment.getArgument("first");
            Integer last = environment.getArgument("last");

            TransitService transitService = GqlUtil.getTransitService(environment);
            TripPattern tripPattern = transitService.getPatternForTrip(trip(environment));

            if (tripPattern == null) {
              return List.of();
            }

            List<StopLocation> stops = tripPattern.getStops();

            if (first != null && last != null) {
              throw new AssertException("Both first and last can't be defined simultaneously.");
            } else if (first != null) {
              if (first > stops.size()) {
                return stops.subList(0, first);
              }
            } else if (last != null) {
              if (last > stops.size()) {
                return stops.subList(stops.size() - last, stops.size());
              }
            }
            return stops;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("passingTimes")
          .type(new GraphQLNonNull(new GraphQLList(timetabledPassingTimeType)))
          .withDirective(gqlUtil.timingData)
          .description(
            "Returns scheduled passing times only - without real-time-updates, for realtime-data use 'estimatedCalls'"
          )
          .dataFetcher(env -> {
            Trip trip = trip(env);
            TripPattern tripPattern = GqlUtil.getTransitService(env).getPatternForTrip(trip);
            if (tripPattern == null) {
              return List.of();
            }
            return TripTimeOnDate.fromTripTimes(tripPattern.getScheduledTimetable(), trip);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("estimatedCalls")
          .type(new GraphQLList(estimatedCallType))
          .withDirective(gqlUtil.timingData)
          .description(
            "Returns scheduled passingTimes for this ServiceJourney for a given date, updated with real-time-updates (if available). " +
            "NB! This takes a date as argument (default=today) and returns estimatedCalls for that date and should only be used if the date is " +
            "known when creating the request. For fetching estimatedCalls for a given trip.leg, use leg.serviceJourneyEstimatedCalls instead."
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("date")
              .type(gqlUtil.dateScalar)
              .description("Date to get estimated calls for. Defaults to today.")
              .build()
          )
          .dataFetcher(environment -> {
            var serviceDate = Optional
              .ofNullable(environment.getArgument("date"))
              .map(LocalDate.class::cast)
              .orElse(LocalDate.now(GqlUtil.getTransitService(environment).getTimeZone()));
            return TripTimesShortHelper.getTripTimesShort(
              GqlUtil.getTransitService(environment),
              trip(environment),
              serviceDate
            );
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("pointsOnLink")
          .type(linkGeometryType)
          .description(
            "Detailed path travelled by service journey. Not available for flexible trips."
          )
          .dataFetcher(environment -> {
            TripPattern tripPattern = GqlUtil
              .getTransitService(environment)
              .getPatternForTrip(trip(environment));
            if (tripPattern == null) {
              return null;
            }

            LineString geometry = tripPattern.getGeometry();
            if (geometry == null) {
              return null;
            }

            return EncodedPolyline.encode(geometry);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("notices")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(noticeType))))
          .dataFetcher(env -> GqlUtil.getTransitService(env).getNoticesByEntity(trip(env)))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("situations")
          .description("Get all situations active for the service journey.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(environment ->
            GqlUtil
              .getTransitService(environment)
              .getTransitAlertService()
              .getTripAlerts(trip(environment).getId(), null)
          )
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("keyValues")
      //                        .description("List of keyValue pairs for the service journey.")
      //                        .type(new GraphQLList(keyValueType))
      //                        .dataFetcher(environment -> ((Trip) trip(environment)).getKeyValues())
      //                        .build())
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("flexibleServiceType")
      //                        .description("Type of flexible service, or null if service is not flexible.")
      //                        .type(flexibleServiceTypeEnum)
      //                        .build())
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("bookingArrangements")
          .description("Booking arrangements for flexible services.")
          .type(bookingArrangementType)
          .deprecate(
            "BookingArrangements are defined per stop, and can be found under `passingTimes` or `estimatedCalls`"
          )
          .build()
      )
      .build();
  }

  private static Trip trip(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
