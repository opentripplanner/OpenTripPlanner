package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.plan.JourneyWhiteListed;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopCollection;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.RoutingService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_SUBMODE;

public class StopPlaceType {
  public static final String NAME = "StopPlace";
  public static final GraphQLOutputType REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create(
      GraphQLInterfaceType placeInterface,
      GraphQLOutputType quayType,
      GraphQLOutputType tariffZoneType,
      GraphQLOutputType estimatedCallType,
      GqlUtil gqlUtil
  ) {
    return GraphQLObjectType.newObject()
        .name(NAME)
        .description("Named place where public transport may be accessed. May be a building complex (e.g. a station) or an on-street location.")
        .withInterface(placeInterface)
        .field(GqlUtil.newTransitIdField())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("name")
            .type(new GraphQLNonNull(Scalars.GraphQLString))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("latitude")
            .type(Scalars.GraphQLFloat)
            .dataFetcher(environment -> (((MonoOrMultiModalStation) environment.getSource()).getLat()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("longitude")
            .type(Scalars.GraphQLFloat)
            .dataFetcher(environment -> (((MonoOrMultiModalStation) environment.getSource()).getLon()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("description")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> (((MonoOrMultiModalStation) environment.getSource()).getDescription()))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("weighting")
            .description("Relative weighting of this stop with regards to interchanges. NOT IMPLEMENTED")
            .type(EnumTypes.INTERCHANGE_WEIGHTING)
            .dataFetcher(environment -> 0)
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("tariffZones")
            .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
            .description("NOT IMPLEMENTED")
            .dataFetcher(environment -> List.of())
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("transportMode")
            .description("The transport modes of quays under this stop place.")
            .type(new GraphQLList(TRANSPORT_MODE))
            .dataFetcher(environment ->
                ((MonoOrMultiModalStation) environment.getSource()).getChildStops()
                    .stream().map(Stop::getVehicleType).collect(Collectors.toSet())
                )
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("transportSubmode")
            .description("The transport submode serviced by this stop place. NOT IMPLEMENTED")
            .deprecate("Submodes not implemented")
            .type(TRANSPORT_SUBMODE)
            .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
            .build())
        //                .field(GraphQLFieldDefinition.newFieldDefinition()
        //                        .name("adjacentSites")
        //                        .description("This stop place's adjacent sites")
        //                        .type(new GraphQLList(Scalars.GraphQLString))
        //                        .dataFetcher(environment -> ((MonoOrMultiModalStation) environment.getSource()).getAdjacentSites())
        //                        .build())
        // TODO stopPlaceType?

        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("quays")
            .description("Returns all quays that are children of this stop place")
            .type(new GraphQLList(quayType))
            .argument(GraphQLArgument.newArgument()
                .name("filterByInUse")
                .description("If true only quays with at least one visiting line are included.")
                .type(Scalars.GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .build())
            .dataFetcher(environment -> {
              Collection<Stop> quays = ((MonoOrMultiModalStation) environment.getSource()).getChildStops();
              if (TRUE.equals(environment.getArgument("filterByInUse"))) {
                quays=quays.stream().filter(stop -> {
                  return !GqlUtil.getRoutingService(environment)
                      .getPatternsForStop(stop,true).isEmpty();
                }).collect(Collectors.toList());
              }
              return quays;
            })
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("parent")
            .description("Returns parent stop for this stop")
            .type(new GraphQLTypeReference(NAME))
            .dataFetcher(
                environment -> (
                    ((MonoOrMultiModalStation) environment.getSource())
                        .getParentStation()
                ))
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("tariffZones")
            .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
            .dataFetcher(environment ->
                ((MonoOrMultiModalStation) environment.getSource())
                    .getChildStops()
                    .stream()
                    .flatMap(s -> s.getFareZones().stream())
                    .collect(Collectors.toSet()))
            .build()
            )
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("estimatedCalls")
            .description("List of visits to this stop place as part of vehicle journeys.")
            .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
            .argument(GraphQLArgument.newArgument()
                .name("startTime")
                .type(gqlUtil.dateTimeScalar)
                .description("DateTime for when to fetch estimated calls from. Default value is current time")
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("timeRange")
                .type(Scalars.GraphQLInt)
                .defaultValue(24 * 60 * 60)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("numberOfDepartures")
                .description("Limit the total number of departures returned.")
                .type(Scalars.GraphQLInt)
                .defaultValue(5)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("numberOfDeparturesPerLineAndDestinationDisplay")
                .description("Limit the number of departures per line and destination display returned. The parameter is only applied " +
                    "when the value is between 1 and 'numberOfDepartures'.")
                .type(Scalars.GraphQLInt)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("omitNonBoarding")
                .type(Scalars.GraphQLBoolean)
                .defaultValue(false)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("whiteListed")
                .description("Whitelisted")
                .description("Parameters for indicating the only authorities and/or lines or quays to list estimatedCalls for")
                .type(JourneyWhiteListed.INPUT_TYPE)
                .build())
            .argument(GraphQLArgument.newArgument()
                .name("whiteListedModes")
                .description("Only show estimated calls for selected modes.")
                .type(GraphQLList.list(TRANSPORT_MODE))
                .build())
            .dataFetcher(environment -> {
              boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
              int numberOfDepartures = environment.getArgument("numberOfDepartures");
              Integer departuresPerLineAndDestinationDisplay = environment.getArgument("numberOfDeparturesPerLineAndDestinationDisplay");
              int timeRage = environment.getArgument("timeRange");

              MonoOrMultiModalStation monoOrMultiModalStation = environment.getSource();
              JourneyWhiteListed whiteListed = new JourneyWhiteListed(environment);
              Collection<TransitMode> transitModes = environment.getArgument("whiteListedModes");

              Long startTimeMs = environment.getArgument("startTime") == null ? 0L : environment.getArgument("startTime");
              Long startTimeSeconds = startTimeMs / 1000;

              return monoOrMultiModalStation.getChildStops()
                  .stream()
                  .flatMap(singleStop ->
                      getTripTimesForStop(
                          singleStop,
                          startTimeSeconds,
                          timeRage,
                          omitNonBoarding,
                          numberOfDepartures,
                          departuresPerLineAndDestinationDisplay,
                          whiteListed.authorityIds,
                          whiteListed.lineIds,
                          transitModes,
                          environment
                      )
                  )
                  .sorted(TripTimeShort.compareByDeparture())
                  .distinct()
                  .limit(numberOfDepartures)
                  .collect(Collectors.toList());
            })
            .build())
        .build();
  }

  public static Stream<TripTimeShort> getTripTimesForStop(
      Stop stop,
      Long startTimeSeconds,
      int timeRage,
      boolean omitNonBoarding,
      int numberOfDepartures,
      Integer departuresPerLineAndDestinationDisplay,
      Collection<FeedScopedId> authorityIdsWhiteListed,
      Collection<FeedScopedId> lineIdsWhiteListed,
      Collection<TransitMode> transitModes,
      DataFetchingEnvironment environment
  ) {
    RoutingService routingService = GqlUtil.getRoutingService(environment);
    boolean limitOnDestinationDisplay = departuresPerLineAndDestinationDisplay != null
        && departuresPerLineAndDestinationDisplay > 0
        && departuresPerLineAndDestinationDisplay < numberOfDepartures;

    int departuresPerTripPattern = limitOnDestinationDisplay
        ? departuresPerLineAndDestinationDisplay
        : numberOfDepartures;

    List<StopTimesInPattern> stopTimesInPatterns = routingService.stopTimesForStop(
        stop,
        startTimeSeconds,
        timeRage,
        departuresPerTripPattern,
        omitNonBoarding,
        false
    );

    // TODO OTP2 - Applying filters here is not correct - the `departuresPerTripPattern` is used
    //           - to limit the result, and using filters after that point may result in
    //           - loosing valid results.

    Stream<StopTimesInPattern> stopTimesStream = stopTimesInPatterns.stream();

    if(transitModes != null && !transitModes.isEmpty()) {
      stopTimesStream = stopTimesStream.filter(it -> transitModes.contains(it.pattern.getMode()));
    }

    Stream<TripTimeShort> tripTimesStream = stopTimesStream
        .flatMap(p -> p.times.stream());

    tripTimesStream = JourneyWhiteListed.whiteListAuthoritiesAndOrLines(
        tripTimesStream,
        authorityIdsWhiteListed,
        lineIdsWhiteListed
    );

    if (!limitOnDestinationDisplay) {
      return tripTimesStream;
    }
    // Group by line and destination display, limit departures per group and merge
    return tripTimesStream
        .collect(Collectors.groupingBy(t -> destinationDisplayPerLine(
            ((TripTimeShort) t)
        )))
        .values()
        .stream()
        .flatMap(tripTimes -> tripTimes
            .stream()
            .sorted(TripTimeShort.compareByDeparture())
            .distinct()
            .limit(departuresPerLineAndDestinationDisplay));
  }

  public static MonoOrMultiModalStation fetchStopPlaceById(FeedScopedId id, DataFetchingEnvironment environment) {
    RoutingService routingService = GqlUtil.getRoutingService(environment);

    Station station = routingService.getStationById(id);

    if (station != null) {
      return new MonoOrMultiModalStation(station, routingService.getMultiModalStationForStations().get(station));
    }

    MultiModalStation multiModalStation = routingService.getMultiModalStation(id);

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
    final RoutingService routingService = GqlUtil.getRoutingService(environment);

    Stream<Station> stations = routingService
        .getStopsByBoundingBox(minLat, minLon, maxLat, maxLon)
        .stream()
        .map(Stop::getParentStation)
        .filter(Objects::nonNull)
        .distinct();

    if (authority != null) {
      stations = stations.filter(s -> s.getId().getFeedId().equalsIgnoreCase(authority));
    }

    if (TRUE.equals(filterByInUse)) {
      stations = stations.filter(s -> isStopPlaceInUse(s, routingService));
    }

    // "child" - Only mono modal children stop places, not their multi modal parent stop
    if("child".equals(multiModalMode)) {
      return stations
          .map(s -> {
            MultiModalStation parent = routingService.getMultiModalStationForStations().get(s);
            return new MonoOrMultiModalStation(s, parent);
          })
          .collect(Collectors.toList());
    }
    // "all" - Both multiModal parents and their mono modal child stop places
    else if("all".equals(multiModalMode)) {
      Set<MonoOrMultiModalStation> result = new HashSet<>();
      stations.forEach(it -> {
        MultiModalStation p = routingService.getMultiModalStationForStations().get(it);
        result.add(new MonoOrMultiModalStation(it, p));
        if(p != null) {
          result.add(new MonoOrMultiModalStation(p));
        }
      });
      return result;
    }
    // Default "parent" - Multi modal parent stop places without their mono modal children, but add
    // mono modal stop places if they have no parent stop place
    else if("parent".equals(multiModalMode)){
      Set<MonoOrMultiModalStation> result = new HashSet<>();
      stations.forEach(it -> {
        MultiModalStation p = routingService.getMultiModalStationForStations().get(it);
        if(p != null) {
          result.add(new MonoOrMultiModalStation(p));
        } else {
          result.add(new MonoOrMultiModalStation(it, null));
        }
      });
      return result;
    }
    else {
      throw new IllegalArgumentException("Unexpected multiModalMode: " + multiModalMode);
    }
  }

  public static boolean isStopPlaceInUse(StopCollection station, RoutingService routingService) {
    for (Stop quay : station.getChildStops()) {
      if (!routingService.getPatternsForStop(quay, true).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static String destinationDisplayPerLine(TripTimeShort t) {
    Trip trip = t.getTrip();
    return trip == null ? t.getHeadsign() : trip.getRoute().getId() + "|" + t.getHeadsign();
  }
}
