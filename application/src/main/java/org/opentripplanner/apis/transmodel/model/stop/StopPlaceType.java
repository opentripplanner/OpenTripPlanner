package org.opentripplanner.apis.transmodel.model.stop;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.apis.transmodel.support.GqlUtil.getPositiveNonNullIntegerArgument;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.plan.JourneyWhiteListed;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;

public class StopPlaceType {

  public static final String NAME = "StopPlace";
  public static final GraphQLOutputType REF = new GraphQLTypeReference(NAME);

  private final FeedScopedIdMapper idMapper;

  public StopPlaceType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType create(
    GraphQLInterfaceType placeInterface,
    GraphQLOutputType quayType,
    GraphQLOutputType tariffZoneType,
    GraphQLOutputType estimatedCallType,
    GraphQLOutputType ptSituationElementType,
    GraphQLScalarType dateTimeScalar
  ) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "Named place where public transport may be accessed. May be a building complex (e.g. a station) or an on-street location."
      )
      .withInterface(placeInterface)
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .dataFetcher(env ->
            idMapper.mapToApi(((MonoOrMultiModalStation) env.getSource()).getId())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .type(new GraphQLNonNull(Scalars.GraphQLString))
          .argument(
            GraphQLArgument.newArgument()
              .name("lang")
              .deprecate("Use 'language' instead")
              .description(
                "Fetch the name in the language given. The language should be represented as a ISO-639 language code. If the translation does not exist, the default name is returned."
              )
              .type(Scalars.GraphQLString)
              .build()
          )
          .argument(
            GraphQLArgument.newArgument()
              .name("language")
              .description(
                "Fetch the name in the language given. The language should be represented as a ISO-639 language code. If the translation does not exist, the default name is returned."
              )
              .type(Scalars.GraphQLString)
              .build()
          )
          .dataFetcher(environment ->
            (((MonoOrMultiModalStation) environment.getSource()).getName()
                .toString(GqlUtil.getLocale(environment)))
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("latitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment ->
            (((MonoOrMultiModalStation) environment.getSource()).getLat())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("longitude")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment ->
            (((MonoOrMultiModalStation) environment.getSource()).getLon())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("description")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment ->
            GraphQLUtils.getTranslation(
              ((MonoOrMultiModalStation) environment.getSource()).getDescription(),
              environment
            )
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("weighting")
          .description(
            "Relative weighting of this stop with regards to interchanges. NOT IMPLEMENTED"
          )
          .deprecate("Not implemented. Use stopInterchangePriority")
          .type(EnumTypes.INTERCHANGE_WEIGHTING)
          .dataFetcher(environment -> 0)
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("stopInterchangePriority")
          .description("Specify the priority of interchanges at this stop")
          .type(EnumTypes.STOP_INTERCHANGE_PRIORITY)
          .dataFetcher(environment ->
            ((MonoOrMultiModalStation) environment.getSource()).getPriority()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tariffZones")
          .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
          .description("NOT IMPLEMENTED")
          .dataFetcher(environment -> List.of())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transportMode")
          .description("The transport modes of quays under this stop place.")
          .type(new GraphQLList(EnumTypes.TRANSPORT_MODE))
          .dataFetcher(environment ->
            ((MonoOrMultiModalStation) environment.getSource()).getChildStops()
              .stream()
              .map(StopLocation::getVehicleType)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("transportSubmode")
          .description("The transport submode serviced by this stop place.")
          .type(new GraphQLList(EnumTypes.TRANSPORT_SUBMODE))
          .dataFetcher(environment ->
            ((MonoOrMultiModalStation) environment.getSource()).getChildStops()
              .stream()
              .map(StopLocation::getNetexVehicleSubmode)
              .filter(it -> it != SubMode.UNKNOWN)
              .map(TransmodelTransportSubmode::fromValue)
              .collect(Collectors.toSet())
          )
          .build()
      )
      //                .field(GraphQLFieldDefinition.newFieldDefinition()
      //                        .name("adjacentSites")
      //                        .description("This stop place's adjacent sites")
      //                        .type(new GraphQLList(Scalars.GraphQLString))
      //                        .dataFetcher(environment -> ((MonoOrMultiModalStation) environment.getSource()).getAdjacentSites())
      //                        .build())
      // TODO stopPlaceType?

      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timeZone")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment ->
            Optional.ofNullable(
              ((MonoOrMultiModalStation) environment.getSource()).getTimezone()
            ).map(ZoneId::getId)
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("quays")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("Returns all quays that are children of this stop place")
          .type(new GraphQLList(quayType))
          .argument(
            GraphQLArgument.newArgument()
              .name("filterByInUse")
              .description("If true only quays with at least one visiting line are included.")
              .type(Scalars.GraphQLBoolean)
              .defaultValue(Boolean.FALSE)
              .build()
          )
          .dataFetcher(environment -> {
            var quays = ((MonoOrMultiModalStation) environment.getSource()).getChildStops();
            if (TRUE.equals(environment.getArgument("filterByInUse"))) {
              quays = quays
                .stream()
                .filter(stop -> {
                  return !GqlUtil.getTransitService(environment).findPatterns(stop, true).isEmpty();
                })
                .collect(Collectors.toList());
            }
            return quays;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("parent")
          .description("Returns parent stop for this stop")
          .type(new GraphQLTypeReference(NAME))
          .dataFetcher(environment ->
            (((MonoOrMultiModalStation) environment.getSource()).getParentStation())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tariffZones")
          .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
          .dataFetcher(environment ->
            ((MonoOrMultiModalStation) environment.getSource()).getChildStops()
              .stream()
              .flatMap(s -> s.getFareZones().stream())
              .collect(Collectors.toSet())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("estimatedCalls")
          .withDirective(TransmodelDirectives.TIMING_DATA)
          .description("List of visits to this stop place as part of vehicle journeys.")
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
              .name("arrivalDeparture")
              .type(EnumTypes.ARRIVAL_DEPARTURE)
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

            MonoOrMultiModalStation monoOrMultiModalStation = environment.getSource();
            JourneyWhiteListed whiteListed = new JourneyWhiteListed(environment, idMapper);
            Collection<TransitMode> transitModes = environment.getArgument("whiteListedModes");

            Instant startTime = environment.containsArgument("startTime")
              ? Instant.ofEpochMilli(environment.getArgument("startTime"))
              : Instant.now();

            Stream<TripTimeOnDate> tripTimeOnDateStream = monoOrMultiModalStation
              .getChildStops()
              .stream()
              .flatMap(singleStop ->
                getTripTimesForStop(
                  singleStop,
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
              );

            return limitPerLineAndDestinationDisplay(
              tripTimeOnDateStream,
              departuresPerLineAndDestinationDisplay
            )
              .sorted(TripTimeOnDate.compareByDeparture())
              .distinct()
              .limit(numberOfDepartures)
              .collect(Collectors.toList());
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("situations")
          .description(
            "Get all situations active for the stop place. Situations affecting individual quays are not returned, and should be fetched directly from the quay."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(ptSituationElementType))))
          .dataFetcher(env ->
            GqlUtil.getTransitService(env)
              .getTransitAlertService()
              .getStopAlerts(((MonoOrMultiModalStation) env.getSource()).getId())
          )
          .build()
      )
      .build();
  }

  public static Stream<TripTimeOnDate> getTripTimesForStop(
    StopLocation stop,
    Instant startTimeSeconds,
    Duration timeRange,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips,
    int numberOfDepartures,
    Integer departuresPerLineAndDestinationDisplay,
    Collection<FeedScopedId> authorityIdsWhiteListed,
    Collection<FeedScopedId> lineIdsWhiteListed,
    Collection<TransitMode> transitModes,
    DataFetchingEnvironment environment
  ) {
    TransitService transitService = GqlUtil.getTransitService(environment);

    List<StopTimesInPattern> stopTimesInPatterns = transitService.findStopTimesInPattern(
      stop,
      startTimeSeconds,
      timeRange,
      numberOfDepartures,
      arrivalDeparture,
      includeCancelledTrips
    );

    Stream<StopTimesInPattern> stopTimesStream = stopTimesInPatterns.stream();

    if (transitModes != null && !transitModes.isEmpty()) {
      stopTimesStream = stopTimesStream.filter(it -> transitModes.contains(it.pattern.getMode()));
    }

    Stream<TripTimeOnDate> tripTimesStream = stopTimesStream.flatMap(p -> p.times.stream());

    tripTimesStream = JourneyWhiteListed.whiteListAuthoritiesAndOrLines(
      tripTimesStream,
      authorityIdsWhiteListed,
      lineIdsWhiteListed
    );

    return limitPerLineAndDestinationDisplay(
      tripTimesStream,
      departuresPerLineAndDestinationDisplay
    );
  }

  private static Stream<TripTimeOnDate> limitPerLineAndDestinationDisplay(
    Stream<TripTimeOnDate> tripTimesStream,
    Integer departuresPerLineAndDestinationDisplay
  ) {
    boolean limitOnDestinationDisplay =
      departuresPerLineAndDestinationDisplay != null && departuresPerLineAndDestinationDisplay > 0;

    if (limitOnDestinationDisplay) {
      // Group by line and destination display, limit departures per group and merge
      return tripTimesStream
        .collect(Collectors.groupingBy(StopPlaceType::destinationDisplayPerLine))
        .values()
        .stream()
        .flatMap(tripTimes ->
          tripTimes
            .stream()
            .sorted(TripTimeOnDate.compareByDeparture())
            .distinct()
            .limit(departuresPerLineAndDestinationDisplay)
        );
    } else {
      return tripTimesStream;
    }
  }

  public static MonoOrMultiModalStation fetchStopPlaceById(
    FeedScopedId id,
    DataFetchingEnvironment environment
  ) {
    if (id == null) {
      return null;
    }

    TransitService transitService = GqlUtil.getTransitService(environment);

    Station station = transitService.getStation(id);

    if (station != null) {
      return new MonoOrMultiModalStation(station, transitService.findMultiModalStation(station));
    }

    MultiModalStation multiModalStation = transitService.getMultiModalStation(id);

    if (multiModalStation != null) {
      return new MonoOrMultiModalStation(multiModalStation);
    }
    return null;
  }

  public static Collection<MonoOrMultiModalStation> fetchStopPlaces(
    double minLat,
    double minLon,
    double maxLat,
    double maxLon,
    String authority,
    Boolean filterByInUse,
    String multiModalMode,
    DataFetchingEnvironment environment
  ) {
    final TransitService transitService = GqlUtil.getTransitService(environment);

    Envelope envelope = new Envelope(
      new Coordinate(minLon, minLat),
      new Coordinate(maxLon, maxLat)
    );

    Stream<Station> stations = transitService
      .findRegularStopsByBoundingBox(envelope)
      .stream()
      .map(StopLocation::getParentStation)
      .filter(Objects::nonNull)
      .distinct();

    if (authority != null) {
      stations = stations.filter(s -> s.getId().getFeedId().equalsIgnoreCase(authority));
    }

    if (TRUE.equals(filterByInUse)) {
      stations = stations.filter(s -> isStopPlaceInUse(s, transitService));
    }

    // "child" - Only mono modal children stop places, not their multi modal parent stop
    if ("child".equals(multiModalMode)) {
      return stations
        .map(s -> {
          MultiModalStation parent = transitService.findMultiModalStation(s);
          return new MonoOrMultiModalStation(s, parent);
        })
        .collect(Collectors.toList());
    }
    // "all" - Both multiModal parents and their mono modal child stop places
    else if ("all".equals(multiModalMode)) {
      Set<MonoOrMultiModalStation> result = new HashSet<>();
      stations.forEach(it -> {
        MultiModalStation p = transitService.findMultiModalStation(it);
        result.add(new MonoOrMultiModalStation(it, p));
        if (p != null) {
          result.add(new MonoOrMultiModalStation(p));
        }
      });
      return result;
    }
    // Default "parent" - Multi modal parent stop places without their mono modal children, but add
    // mono modal stop places if they have no parent stop place
    else if ("parent".equals(multiModalMode)) {
      Set<MonoOrMultiModalStation> result = new HashSet<>();
      stations.forEach(it -> {
        MultiModalStation p = transitService.findMultiModalStation(it);
        if (p != null) {
          result.add(new MonoOrMultiModalStation(p));
        } else {
          result.add(new MonoOrMultiModalStation(it, null));
        }
      });
      return result;
    } else {
      throw new IllegalArgumentException("Unexpected multiModalMode: " + multiModalMode);
    }
  }

  public static boolean isStopPlaceInUse(
    StopLocationsGroup station,
    TransitService transitService
  ) {
    for (var quay : station.getChildStops()) {
      if (!transitService.findPatterns(quay, true).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static String destinationDisplayPerLine(TripTimeOnDate t) {
    Trip trip = t.getTrip();
    String headsign = t.getHeadsign() != null ? t.getHeadsign().toString() : null;
    return trip == null ? headsign : trip.getRoute().getId() + "|" + headsign;
  }
}
