package org.opentripplanner.apis.transmodel.model.stop;

import static org.opentripplanner.apis.transmodel.support.GqlUtil.getPositiveNonNullIntegerArgument;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.plan.JourneyWhiteListed;
import org.opentripplanner.apis.transmodel.model.scalars.GeoJSONCoordinatesScalar;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class QuayType {

  private static final String NAME = "Quay";
  public static final GraphQLOutputType REF = new GraphQLTypeReference(NAME);

  private final FeedScopedIdMapper idMapper;

  public QuayType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType create(
    GraphQLInterfaceType placeInterface,
    GraphQLOutputType stopPlaceType,
    GraphQLOutputType lineType,
    GraphQLOutputType journeyPatternType,
    GraphQLOutputType estimatedCallType,
    GraphQLOutputType ptSituationElementType,
    GraphQLOutputType tariffZoneType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "A place such as platform, stance, or quayside where passengers have access to PT vehicles."
      )
      .withInterface(placeInterface)
      .field(GqlUtil.newTransitIdField(idMapper))
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .argument(
            GraphQLArgument.newArgument()
              .name("lang")
              .deprecate("Use 'language' instead")
              .description(
                "Fetch the name in the language given. The language should be represented as a ISO-639 language code. If the translation does not exits, the default name is returned."
              )
              .type(Scalars.GraphQLString)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("language")
              .description(
                "Fetch the name in the language given. The language should be represented as a ISO-639 language code. If the translation does not exits, the default name is returned."
              )
              .type(Scalars.GraphQLString)
              .build()
          )
          .dataFetcher(env ->
            (((StopLocation) env.getSource()).getName().toString(GqlUtil.getLocale(env)))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> (((StopLocation) env.getSource()).getLat()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("longitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> (((StopLocation) env.getSource()).getLon()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("description")
          .type(Scalars.GraphQLString)
          .dataFetcher(env ->
            GraphQLUtils.getTranslation(((StopLocation) env.getSource()).getDescription(), env)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopPlace")
          .description("The stop place to which this quay belongs to.")
          .type(stopPlaceType)
          .dataFetcher(env -> {
            Station station = ((StopLocation) env.getSource()).getParentStation();
            if (station != null) {
              return new MonoOrMultiModalStation(
                station,
                GqlUtil.getTransitService(env).findMultiModalStation(station)
              );
            } else {
              return null;
            }
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("wheelchairAccessible")
          .type(EnumTypes.WHEELCHAIR_BOARDING)
          .description("Whether this quay is suitable for wheelchair boarding.")
          .dataFetcher(env ->
            Objects.requireNonNullElse(
              (((StopLocation) env.getSource()).getWheelchairAccessibility()),
              Accessibility.NO_INFORMATION
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timeZone")
          .type(Scalars.GraphQLString)
          .dataFetcher(env ->
            Optional.ofNullable(((StopLocation) env.getSource()).getTimeZone()).map(ZoneId::getId)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("publicCode")
          .type(Scalars.GraphQLString)
          .description(
            "Public code used to identify this quay within the stop place. For instance a platform code."
          )
          .dataFetcher(env -> (((StopLocation) env.getSource()).getPlatformCode()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("lines")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("List of lines servicing this quay")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(lineType))))
          .dataFetcher(env ->
            GqlUtil.getTransitService(env)
              .findPatterns(env.getSource(), true)
              .stream()
              .map(TripPattern::getRoute)
              .distinct()
              .toList()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("journeyPatterns")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("List of journey patterns servicing this quay")
          .type(new GraphQLNonNull(new GraphQLList(journeyPatternType)))
          .dataFetcher(env -> GqlUtil.getTransitService(env).findPatterns(env.getSource(), true))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("estimatedCalls")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("List of visits to this quay as part of vehicle journeys.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(estimatedCallType))))
          .argument(
            GraphQLArgument.newArgument()
              .name("startTime")
              .type(dateTimeScalar)
              .description(
                "DateTime for when to fetch estimated calls from. Default value is current time"
              )
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("timeRange")
              .description(
                "Duration in seconds from start time to search forward for estimated calls. Must be a positive value. Default value is 24 hours"
              )
              .type(Scalars.GraphQLInt)
              .defaultValue(24 * 60 * 60)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("numberOfDepartures")
              .description("Limit the total number of departures returned.")
              .type(Scalars.GraphQLInt)
              .defaultValue(5)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("numberOfDeparturesPerLineAndDestinationDisplay")
              .description(
                "Limit the number of departures per line and destination display returned. The parameter is only applied " +
                "when the value is between 1 and 'numberOfDepartures'."
              )
              .type(Scalars.GraphQLInt)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("omitNonBoarding")
              .type(Scalars.GraphQLBoolean)
              .deprecate("Non-functional. Use arrivalDeparture instead.")
              .defaultValue(false)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("arrivalDeparture")
              .type(EnumTypes.ARRIVAL_DEPARTURE)
              .description(
                "Filters results by either departures, arrivals or both. " +
                "For departures forBoarding has to be true and the departure " +
                "time has to be within the specified time range. For arrivals, " +
                "forAlight has to be true and the arrival time has to be within " +
                "the specified time range. If both are asked for, either the " +
                "conditions for arrivals or the conditions for departures will " +
                "have to be true for an EstimatedCall to show."
              )
              .defaultValue(ArrivalDeparture.DEPARTURES)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("whiteListed")
              .description("Whitelisted")
              .description(
                "Parameters for indicating the only authorities and/or lines or quays to list estimatedCalls for"
              )
              .type(JourneyWhiteListed.INPUT_TYPE)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("whiteListedModes")
              .description("Only show estimated calls for selected modes.")
              .type(GraphQLList.list(EnumTypes.TRANSPORT_MODE))
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("includeCancelledTrips")
              .description("Indicates that real-time-cancelled trips should also be included.")
              .type(Scalars.GraphQLBoolean)
              .defaultValue(false)
              .build()
          )
          .dataFetcher(environment -> {
            ArrivalDeparture arrivalDeparture = environment.getArgument("arrivalDeparture");
            boolean includeCancelledTrips = environment.getArgument("includeCancelledTrips");
            int numberOfDepartures = environment.getArgument("numberOfDepartures");
            Integer departuresPerLineAndDestinationDisplay = environment.getArgument(
              "numberOfDeparturesPerLineAndDestinationDisplay"
            );
            int timeRangeInput = getPositiveNonNullIntegerArgument(environment, "timeRange");
            Duration timeRange = Duration.ofSeconds(timeRangeInput);
            StopLocation stop = environment.getSource();

            JourneyWhiteListed whiteListed = new JourneyWhiteListed(environment, idMapper);
            Collection<TransitMode> transitModes = environment.getArgument("whiteListedModes");

            Long startTimeInput = environment.getArgument("startTime");
            Instant startTime = startTimeInput != null
              ? Instant.ofEpochMilli(startTimeInput)
              : Instant.now();

            return StopPlaceType.getTripTimesForStop(
              stop,
              startTime,
              timeRange,
              arrivalDeparture,
              includeCancelledTrips,
              numberOfDepartures,
              departuresPerLineAndDestinationDisplay,
              whiteListed.authorityIds,
              whiteListed.lineIds,
              transitModes,
              environment
            )
              .sorted(TripTimeOnDate.compareByDeparture())
              .distinct()
              .limit(numberOfDepartures)
              .toList();
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .description("Get all situations active for the quay.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(env ->
            GqlUtil.getTransitService(env)
              .getTransitAlertService()
              .getStopAlerts(((StopLocation) env.getSource()).getId())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopType")
          .type(Scalars.GraphQLString)
          .dataFetcher(env ->
            StopTypeMapper.getStopType(((StopLocation) env.getSource()).getStopType())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("flexibleArea")
          .description("Geometry for flexible area.")
          .type(GeoJSONCoordinatesScalar.getGraphQGeoJSONCoordinatesScalar())
          .dataFetcher(env -> {
            StopLocation stopLocation = env.getSource();
            return stopLocation
              .getEncompassingAreaGeometry()
              .map(Geometry::getCoordinates)
              .orElse(null);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("flexibleGroup")
          .description("the Quays part of a flexible group.")
          .type(GraphQLList.list(REF))
          .dataFetcher(env -> ((StopLocation) env.getSource()).getChildLocations())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tariffZones")
          .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
          .dataFetcher(env -> ((StopLocation) env.getSource()).getFareZones())
          .build()
      )
      .build();
  }
}
