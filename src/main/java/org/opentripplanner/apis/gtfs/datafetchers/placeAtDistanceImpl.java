package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.execution.TypeResolutionParameters;
import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;

public class placeAtDistanceImpl implements GraphQLDataFetchers.GraphQLPlaceAtDistance {

  @Override
  public DataFetcher<Integer> distance() {
    return environment -> (int) getSource(environment).distance();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> {
      PlaceAtDistance placeAtDistance = getSource(environment);
      Object place = placeAtDistance.place();
      TypeResolver typeResolver = new PlaceInterfaceTypeResolver();

      GraphQLInterfaceType placeInterface = (GraphQLInterfaceType) environment
        .getGraphQLSchema()
        .getType("PlaceInterface");

      var resolution = new TypeResolutionParameters.Builder()
        .value(place)
        .argumentValues(environment::getArguments)
        .field(environment.getMergedField())
        .fieldType(placeInterface)
        .schema(environment.getGraphQLSchema())
        .graphQLContext(environment.getGraphQlContext())
        .build();

      GraphQLObjectType placeType = typeResolver.getType(resolution);

      Relay.ResolvedGlobalId globalId = (Relay.ResolvedGlobalId) environment
        .getGraphQLSchema()
        .getCodeRegistry()
        .getDataFetcher(
          FieldCoordinates.coordinates(placeType.getName(), "id"),
          placeInterface.getFieldDefinition("id")
        )
        .get(
          DataFetchingEnvironmentImpl.newDataFetchingEnvironment(environment).source(place).build()
        );

      return new Relay.ResolvedGlobalId(
        "placeAtDistance",
        placeAtDistance.distance() +
        ";" +
        new Relay().toGlobalId(globalId.getType(), globalId.getId())
      );
    };
  }

  @Override
  public DataFetcher<Object> place() {
    return environment -> getSource(environment).place();
  }

  private PlaceAtDistance getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
