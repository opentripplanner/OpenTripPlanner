package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.apis.transmodel.model.TransmodelPlaceType;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.TransitService;

public class PlaceAtDistanceType {

  public static final String NAME = "PlaceAtDistance";

  public static GraphQLObjectType create(Relay relay, GraphQLInterfaceType placeInterface) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("id")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .deprecate("Id is not referable or meaningful and will be removed")
          .dataFetcher(environment -> relay.toGlobalId(NAME, "N/A"))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("place")
          .type(placeInterface)
          .dataFetcher(environment -> ((PlaceAtDistance) environment.getSource()).place())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("distance")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(environment -> ((PlaceAtDistance) environment.getSource()).distance())
          .build()
      )
      .build();
  }

  /**
   * Create PlaceAndDistance objects for all unique stopPlaces according to specified multiModalMode
   * if client has requested stopPlace type.
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
    TransitService transitService
  ) {
    // Make sure places is mutable
    places = new ArrayList<>(places);

    if (placeTypes == null || placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
      // Convert quays to stop places
      List<PlaceAtDistance> stations = places
        .stream()
        // Find all stops
        .filter(p -> p.place() instanceof RegularStop)
        // Get their parent stations (possibly including multimodal parents)
        .flatMap(p -> getStopPlaces(p, multiModalMode, transitService))
        // Sort by distance
        .sorted(Comparator.comparing(PlaceAtDistance::distance))
        // Make sure each parent appears exactly once
        .filter(new SeenPlacePredicate())
        .toList();

      places.addAll(stations);

      if (placeTypes != null && !placeTypes.contains(TransmodelPlaceType.QUAY)) {
        // Remove quays if only stop places are requested
        places = places
          .stream()
          .filter(p -> !(p.place() instanceof RegularStop))
          .collect(Collectors.toList());
      }
    }
    places.sort(Comparator.comparing(PlaceAtDistance::distance));

    Set<Object> uniquePlaces = new HashSet<>();
    return places.stream().filter(s -> uniquePlaces.add(s.place())).collect(Collectors.toList());
  }

  private static Stream<PlaceAtDistance> getStopPlaces(
    PlaceAtDistance p,
    String multiModalMode,
    TransitService transitService
  ) {
    Station stopPlace = ((RegularStop) p.place()).getParentStation();

    if (stopPlace == null) {
      return Stream.of();
    }

    List<PlaceAtDistance> res = new ArrayList<>();

    MultiModalStation multiModalStation = transitService.findMultiModalStation(stopPlace);

    if (
      "child".equals(multiModalMode) || "all".equals(multiModalMode) || multiModalStation == null
    ) {
      res.add(
        new PlaceAtDistance(new MonoOrMultiModalStation(stopPlace, multiModalStation), p.distance())
      );
    }

    if (multiModalStation == null) {
      return res.stream();
    }

    if ("parent".equals(multiModalMode) || "all".equals(multiModalMode)) {
      res.add(new PlaceAtDistance(new MonoOrMultiModalStation(multiModalStation), p.distance()));
    }
    return res.stream();
  }

  static class SeenPlacePredicate implements Predicate<PlaceAtDistance> {

    Set<Object> seen = new HashSet<>();

    @Override
    public boolean test(PlaceAtDistance p) {
      return seen.add(p.place());
    }
  }
}
