package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ViaTripType {

  public static GraphQLOutputType create(
    GraphQLObjectType tripPatternType,
    GraphQLObjectType routingErrorType
  ) {
    var viaTripPatternSegment = GraphQLObjectType
      .newObject()
      .name("ViaTripPatternSegment")
      .description(
        "A segment of the via search. The first segment is from the start location to the first " +
        "entry in the locations list and the last is from the last entry in the locations list " +
        "to the end location."
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("tripPatterns")
          .description("A list of trip patterns for this segment of the search")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(tripPatternType))))
          .dataFetcher(DataFetchingEnvironment::getSource)
          .build()
      )
      .build();

    var viaTripPatternCombinations = GraphQLObjectType
      .newObject()
      .name("ViaConnection")
      .description(
        "An acceptable combination of trip patterns between two segments of the via search"
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("from")
          .description("The index of the trip pattern in the segment before the via point")
          .type(Scalars.GraphQLInt)
          .dataFetcher(dataFetchingEnvironment ->
            ((ViaTripPatternPair) dataFetchingEnvironment.getSource()).from()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("to")
          .description("The index of the trip pattern in the segment after the via point")
          .type(Scalars.GraphQLInt)
          .dataFetcher(dataFetchingEnvironment ->
            ((ViaTripPatternPair) dataFetchingEnvironment.getSource()).to()
          )
          .build()
      )
      .build();

    return GraphQLObjectType
      .newObject()
      .name("ViaTrip")
      .description("Description of a travel between three or more places.")
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("tripPatternsPerSegment")
          .description(
            "A list of segments of the via search. The first segment is from the start location " +
            "to the first entry in the locations list and the last is from the last entry in the " +
            "locations list to the end location."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(viaTripPatternSegment))))
          .dataFetcher(env -> ((ViaPlanResponse) env.getSource()).viaJourneys())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("tripPatternCombinations")
          .description(
            "A list of the acceptable combinations of the trip patterns in this segment and the " +
            "next segment."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(viaTripPatternCombinations))))
          .dataFetcher(env ->
            ((ViaPlanResponse) env.getSource()).viaJourneyConnections()
              .stream()
              .flatMap(ViaTripPatternPair::map)
              .toList()
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("routingErrors")
          .description("A list of routing errors, and fields which caused them")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(routingErrorType))))
          .dataFetcher(env -> ((ViaPlanResponse) env.getSource()).routingErrors())
          .build()
      )
      .build();
  }

  private record ViaTripPatternPair(int from, int to) {
    /**
     * Convert a list supplied by {@link ViaPlanResponse#viaJourneyConnections()} to a stream of
     * {@link ViaTripPatternPair}s
     */
    private static Stream<ViaTripPatternPair> map(List<List<Integer>> connections) {
      var ret = new ArrayList<ViaTripPatternPair>();
      for (int from = 0; from < connections.size(); from++) {
        for (int to : connections.get(from)) {
          ret.add(new ViaTripPatternPair(from, to));
        }
      }

      return ret.stream();
    }
  }
}
