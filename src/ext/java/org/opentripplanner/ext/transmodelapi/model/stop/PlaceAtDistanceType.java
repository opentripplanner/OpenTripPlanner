package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlaceAtDistanceType {

  public static final String NAME = "PlaceAtDistance";

  public static GraphQLObjectType create(Relay relay, GraphQLInterfaceType placeInterface) {
    return GraphQLObjectType
        .newObject()
        .name(NAME)
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("id")
            .type(new GraphQLNonNull(Scalars.GraphQLID))
            .deprecate("Id is not referable or meaningful and will be removed")
            .dataFetcher(environment -> relay.toGlobalId(NAME, "N/A"))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("place")
            .type(placeInterface)
            .dataFetcher(environment -> ((PlaceAtDistance) environment.getSource()).place)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("distance")
            .type(Scalars.GraphQLFloat)
            .dataFetcher(environment -> ((PlaceAtDistance) environment.getSource()).distance)
            .build())
        .build();
  }

  /**
   * Create PlaceAndDistance objects for all unique stopPlaces according to specified
   * multiModalMode if client has requested stopPlace type.
   * <p>
   * Necessary because nearest does not support StopPlace (stations), so we need to fetch quays
   * instead and map the response.
   * <p>
   * Remove PlaceAndDistance objects for quays if client has not requested these.
   */
  public static List<PlaceAtDistance> convertQuaysToStopPlaces(
      List<TransmodelPlaceType> placeTypes,
      List<PlaceAtDistance> places,
      String multiModalMode,
      RoutingService routingService
  ) {
      if (placeTypes == null || placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
          // Convert quays to stop places
          List<PlaceAtDistance> stations = places
              .stream()
              // Find all stops
              .filter(p -> p.place instanceof Stop)
              // Get their parent stations (possibly including multimodal parents)
              .flatMap(p -> getStopPlaces(p, multiModalMode, routingService))
              // Sort by distance
              .sorted(Comparator.comparing(p -> p.distance))
              // Make sure each parent appears exactly once
              .filter(new SeenPlacePredicate())
              .collect(Collectors.toList());

          places.addAll(stations);

      if (placeTypes != null && !placeTypes.contains(TransmodelPlaceType.QUAY)) {
        // Remove quays if only stop places are requested
        places = places.stream().filter(p -> !(p.place instanceof Stop)).collect(Collectors.toList());
      }

    }
    places.sort(Comparator.comparing(p -> p.distance));

    Set<Object> uniquePlaces= new HashSet<>();
    return places.stream().filter(s -> uniquePlaces.add(s.place)).collect(Collectors.toList());
  }

    private static Stream<PlaceAtDistance> getStopPlaces(
        PlaceAtDistance p,
        String multiModalMode,
        RoutingService routingService
    ) {
        Station stopPlace = ((Stop) p.place).getParentStation();

        if (stopPlace == null) {
            return Stream.of();
        }

        List<PlaceAtDistance> res = new ArrayList<>();

        MultiModalStation multiModalStation = routingService
            .getMultiModalStationForStations()
            .get(stopPlace);

        if ("child".equals(multiModalMode) ||
            "all".equals(multiModalMode) ||
            multiModalStation == null
        ) {
            res.add(new PlaceAtDistance(
                new MonoOrMultiModalStation(stopPlace, multiModalStation),
                p.distance
            ));
        }

        if (multiModalStation == null) {
            return res.stream();
        }

        if ("parent".equals(multiModalMode) || "all".equals(multiModalMode)) {
            res.add(new PlaceAtDistance(new MonoOrMultiModalStation(multiModalStation), p.distance));
        }
        return res.stream();
    }

    static class SeenPlacePredicate implements Predicate<PlaceAtDistance> {

      Set<Object> seen = new HashSet<>();

      @Override
      public boolean test(PlaceAtDistance p) {
          return seen.add(p.place);
      }
  }
}
