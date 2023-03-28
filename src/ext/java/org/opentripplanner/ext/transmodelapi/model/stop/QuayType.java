package org.opentripplanner.ext.transmodelapi.model.stop;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.plan.JourneyWhiteListed;
import org.opentripplanner.ext.transmodelapi.model.scalars.GeoJSONCoordinatesScalar;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;

public class QuayType {

  private static final String NAME = "Quay";
  public static final GraphQLOutputType REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
    GraphQLInterfaceType placeInterface,
    GraphQLOutputType stopPlaceType,
    GraphQLOutputType lineType,
    GraphQLOutputType journeyPatternType,
    GraphQLOutputType estimatedCallType,
    GraphQLOutputType ptSituationElementType,
    GraphQLOutputType tariffZoneType,
    GqlUtil gqlUtil
  ) {
    return GraphQLObjectType
      .newObject()
      .name(NAME)
      .description(
        "A place such as platform, stance, or quayside where passengers have access to PT vehicles."
      )
      .withInterface(placeInterface)
      .field(GqlUtil.newTransitIdField())
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("name")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("lang")
              .description(
                "Fetch the name in the language given. The language should be represented as a ISO-639 language code. If the translation does not exits, the default name is returned."
              )
              .type(Scalars.GraphQLString)
              .build()
          )
          .dataFetcher(environment -> {
            String lang = environment.getArgument("lang");
            Locale locale = lang != null ? new Locale(lang) : null;
            return (((StopLocation) environment.getSource()).getName().toString(locale));
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("latitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment -> (((StopLocation) environment.getSource()).getLat()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("longitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment -> (((StopLocation) environment.getSource()).getLon()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("description")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment ->
            GraphQLUtils.getTranslation(
              ((StopLocation) environment.getSource()).getDescription(),
              environment
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("stopPlace")
          .description("The stop place to which this quay belongs to.")
          .type(stopPlaceType)
          .dataFetcher(environment -> {
            Station station = ((StopLocation) environment.getSource()).getParentStation();
            if (station != null) {
              return new MonoOrMultiModalStation(
                station,
                GqlUtil.getTransitService(environment).getMultiModalStationForStation(station)
              );
            } else {
              return null;
            }
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("wheelchairAccessible")
          .type(EnumTypes.WHEELCHAIR_BOARDING)
          .description("Whether this quay is suitable for wheelchair boarding.")
          .dataFetcher(environment ->
            Objects.requireNonNullElse(
              (((StopLocation) environment.getSource()).getWheelchairAccessibility()),
              Accessibility.NO_INFORMATION
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("timeZone")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment ->
            Optional
              .ofNullable(((StopLocation) environment.getSource()).getTimeZone())
              .map(ZoneId::getId)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("publicCode")
          .type(Scalars.GraphQLString)
          .description(
            "Public code used to identify this quay within the stop place. For instance a platform code."
          )
          .dataFetcher(environment -> (((StopLocation) environment.getSource()).getPlatformCode()))
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("lines")
          .withDirective(gqlUtil.timingData)
          .description("List of lines servicing this quay")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(lineType))))
          .dataFetcher(environment -> {
            return GqlUtil
              .getTransitService(environment)
              .getPatternsForStop(environment.getSource(), true)
              .stream()
              .map(pattern -> pattern.getRoute())
              .distinct()
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("journeyPatterns")
          .withDirective(gqlUtil.timingData)
          .description("List of journey patterns servicing this quay")
          .type(new GraphQLNonNull(new GraphQLList(journeyPatternType)))
          .dataFetcher(environment -> {
            return GqlUtil
              .getTransitService(environment)
              .getPatternsForStop(environment.getSource(), true);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("estimatedCalls")
          .withDirective(gqlUtil.timingData)
          .description("List of visits to this quay as part of vehicle journeys.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(estimatedCallType))))
          .argument(
            GraphQLArgument
              .newArgument()
              .name("startTime")
              .type(gqlUtil.dateTimeScalar)
              .description(
                "DateTime for when to fetch estimated calls from. Default value is current time"
              )
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("timeRange")
              .type(Scalars.GraphQLInt)
              .defaultValue(24 * 60 * 60)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("numberOfDepartures")
              .description("Limit the total number of departures returned.")
              .type(Scalars.GraphQLInt)
              .defaultValue(5)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("numberOfDeparturesPerLineAndDestinationDisplay")
              .description(
                "Limit the number of departures per line and destination display returned. The parameter is only applied " +
                "when the value is between 1 and 'numberOfDepartures'."
              )
              .type(Scalars.GraphQLInt)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("omitNonBoarding")
              .type(Scalars.GraphQLBoolean)
              .deprecate("Non-functional. Use arrivalDeparture instead.")
              .defaultValue(false)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
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
            GraphQLArgument
              .newArgument()
              .name("whiteListed")
              .description("Whitelisted")
              .description(
                "Parameters for indicating the only authorities and/or lines or quays to list estimatedCalls for"
              )
              .type(JourneyWhiteListed.INPUT_TYPE)
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("whiteListedModes")
              .description("Only show estimated calls for selected modes.")
              .type(GraphQLList.list(TRANSPORT_MODE))
              .build()
          )
          .argument(
            GraphQLArgument
              .newArgument()
              .name("includeCancelledTrips")
              .description("Indicates that realtime-cancelled trips should also be included.")
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
            Integer timeRangeInput = environment.getArgument("timeRange");
            Duration timeRange = Duration.ofSeconds(timeRangeInput.longValue());
            RegularStop stop = environment.getSource();

            JourneyWhiteListed whiteListed = new JourneyWhiteListed(environment);
            Collection<TransitMode> transitModes = environment.getArgument("whiteListedModes");

            Instant startTime = environment.containsArgument("startTime")
              ? Instant.ofEpochMilli(environment.getArgument("startTime"))
              : Instant.now();

            return StopPlaceType
              .getTripTimesForStop(
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
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("situations")
          .description("Get all situations active for the quay.")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(env -> {
            return GqlUtil
              .getTransitService(env)
              .getTransitAlertService()
              .getStopAlerts(((StopLocation) env.getSource()).getId());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("stopType")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> {
            StopLocation stopLocation = environment.getSource();
            if (stopLocation instanceof RegularStop) {
              return "regular";
            } else if (stopLocation instanceof AreaStop) {
              return "flexible_area";
            } else if (stopLocation instanceof GroupStop) {
              return "flexible_group";
            }
            return null;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("flexibleArea")
          .description("Geometry for flexible area.")
          .type(GeoJSONCoordinatesScalar.getGraphQGeoJSONCoordinatesScalar())
          .dataFetcher(environment ->
            (
              environment.getSource() instanceof AreaStop areaStop
                ? areaStop.getGeometry().getCoordinates()
                : null
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("flexibleGroup")
          .description("the Quays part of an flexible group.")
          .type(GraphQLList.list(REF))
          .dataFetcher(environment ->
            (
              environment.getSource() instanceof GroupStop groupStop
                ? groupStop.getLocations()
                : null
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("tariffZones")
          .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
          .dataFetcher(environment -> ((StopLocation) environment.getSource()).getFareZones())
          .build()
      )
      .build();
  }
}
