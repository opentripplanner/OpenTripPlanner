package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLSchema;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class PlaceInterfaceType {

  public static GraphQLInterfaceType create(
  ) {
    return GraphQLInterfaceType
        .newInterface()
        .name("PlaceInterface")
        .description("Interface for places, i.e. quays, stop places, parks")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("id")
            .type(new GraphQLNonNull(Scalars.GraphQLID))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("latitude")
            .type(Scalars.GraphQLFloat)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("longitude")
            .type(Scalars.GraphQLFloat)
            .build())
        .typeResolver(typeResolutionEnvironment -> {
          Object o = typeResolutionEnvironment.getObject();
          GraphQLSchema schema = typeResolutionEnvironment.getSchema();

          // Passing in the type itself in the constructor does not work, as the type has not been
          // created yet and you need the actual type and not just a reference to it. That is why
          // we get the type from the schema. This also follows how it is done in the
          // LegacyGraphQLNodeTypeResolver.

          if (o instanceof Stop) {
            return schema.getObjectType("Quay");
          }
          if (o instanceof MonoOrMultiModalStation) {
            return schema.getObjectType("StopPlace");
          }
          if (o instanceof BikeRentalStation) {
            return schema.getObjectType("BikeRentalStation");
          }
          if (o instanceof BikePark) {
            return schema.getObjectType("BikePark");
          }
          //if (o instanceof CarPark) {
          //    return (GraphQLObjectType) carParkType;
          //}
          return null;
        })
        .build();
  }
}
