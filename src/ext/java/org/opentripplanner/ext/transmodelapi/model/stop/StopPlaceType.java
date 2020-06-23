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
import org.opentripplanner.ext.transmodelapi.model.base.GqlUtil;
import org.opentripplanner.ext.transmodelapi.model.route.JourneyWhiteListed;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.RoutingService;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_SUBMODE;

public class StopPlaceType {
  public static final String NAME = "StopPlace";

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
            .name("wheelchairBoarding")
            .description("Whether this stop place is suitable for wheelchair boarding.")
            .type(EnumTypes.WHEELCHAIR_BOARDING)
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
            .description("The transport mode serviced by this stop place.  NOT IMPLEMENTED")
            .type(TRANSPORT_MODE)
            .dataFetcher(environment -> "unknown")
            .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("transportSubmode")
            .description("The transport submode serviced by this stop place. NOT IMPLEMENTED")
            .type(TRANSPORT_SUBMODE)
            .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
            .build())
        //                .field(GraphQLFieldDefinition.newFieldDefinition()
        //                        .name("adjacentSites")
        //                        .description("This stop place's adjacent sites")
        //                        .type(new GraphQLList(Scalars.GraphQLString))
        //                        .dataFetcher(environment -> ((MonoOrMultiModalStation) environment.getSource()).getAdjacentSites())
        //                        .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
            .name("timezone")
            .type(new GraphQLNonNull(Scalars.GraphQLString))
            .build())
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
              if (Boolean.TRUE.equals(environment.getArgument("filterByInUse"))) {
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
            .dataFetcher(environment -> {
              boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
              int numberOfDepartures = environment.getArgument("numberOfDepartures");
              Integer departuresPerLineAndDestinationDisplay = environment.getArgument("numberOfDeparturesPerLineAndDestinationDisplay");
              int timeRage = environment.getArgument("timeRange");

              MonoOrMultiModalStation monoOrMultiModalStation = environment.getSource();
              JourneyWhiteListed whiteListed = new JourneyWhiteListed(environment);

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
      Set<FeedScopedId> authorityIdsWhiteListed,
      Set<FeedScopedId> lineIdsWhiteListed,
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
        omitNonBoarding
    );

    Stream<TripTimeShort> tripTimesStream = stopTimesInPatterns
        .stream()
        .flatMap(p -> p.times.stream());

    tripTimesStream = JourneyWhiteListed.whiteListAuthoritiesAndOrLines(tripTimesStream,
        authorityIdsWhiteListed,
        lineIdsWhiteListed,
        routingService
    );

    if (!limitOnDestinationDisplay) {
      return tripTimesStream;
    }
    // Group by line and destination display, limit departures per group and merge
    return tripTimesStream
        .collect(Collectors.groupingBy(t -> destinationDisplayPerLine(
            ((TripTimeShort) t),
            routingService
        )))
        .values()
        .stream()
        .flatMap(tripTimes -> tripTimes
            .stream()
            .sorted(TripTimeShort.compareByDeparture())
            .distinct()
            .limit(departuresPerLineAndDestinationDisplay));
  }

  private static String destinationDisplayPerLine(TripTimeShort t, RoutingService routingService) {
    Trip trip = routingService.getTripForId().get(t.tripId);
    return trip == null ? t.headsign : trip.getRoute().getId() + "|" + t.headsign;
  }
}
