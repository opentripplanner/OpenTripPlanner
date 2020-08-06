package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;

public class LegacyGraphQLplaceAtDistanceImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLPlaceAtDistance {

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment -> {
      PlaceAtDistance placeAtDistance = getSource(environment);
      Object place = placeAtDistance.place;
      TypeResolver typeResolver = new LegacyGraphQLPlaceInterfaceTypeResolver();

      GraphQLInterfaceType placeInterface = (GraphQLInterfaceType) environment.getGraphQLSchema().getType("PlaceInterface");

      GraphQLObjectType placeType = typeResolver.getType(new TypeResolutionEnvironment(
          place,
          environment.getArguments(),
          environment.getMergedField(),
          placeInterface,
          environment.getGraphQLSchema(),
          environment.getContext()
      ));

      Relay.ResolvedGlobalId globalId = (Relay.ResolvedGlobalId) environment
          .getGraphQLSchema()
          .getCodeRegistry()
          .getDataFetcher(
          FieldCoordinates.coordinates(placeType.getName(), "id"),
          placeInterface.getFieldDefinition("id")
      ).get(
          DataFetchingEnvironmentImpl.newDataFetchingEnvironment(environment).source(place).build());

      return new Relay.ResolvedGlobalId(
          "placeAtDistance",
          placeAtDistance.distance + ";" + new Relay().toGlobalId(globalId.getType(), globalId.getId())
      );
    };
  }

  @Override
  public DataFetcher<Object> place() {
    return environment -> getSource(environment).place;
  }

  @Override
  public DataFetcher<Integer> distance() {
    return environment -> (int) getSource(environment).distance;
  }

  private PlaceAtDistance getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
