package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
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
    if (placeTypes==null || placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
      // Convert quays to stop places
      List<PlaceAtDistance> stations = places
          .stream()
          .filter(p -> p.place instanceof Stop)
          .map(p -> new PlaceAtDistance(new MonoOrMultiModalStation(((Stop) p.place).getParentStation(),
              null
          ), p.distance))
          .collect(Collectors.toList());

      List<PlaceAtDistance> parentStations = stations.stream()
          .filter(p -> routingService.getMultiModalStationForStations().containsKey(p.place))
          .map(p -> new PlaceAtDistance( routingService.getMultiModalStationForStations().get(p.place), p.distance))
          .collect(Collectors.toList());

      if ("parent".equals(multiModalMode)) {
        // Replace monomodal children with their multimodal parents
        stations = parentStations;
      }
      else if ("all".equals(multiModalMode)) {
        // Add multimodal parents in addition to their monomodal children
        places.addAll(parentStations);
      }

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

}
