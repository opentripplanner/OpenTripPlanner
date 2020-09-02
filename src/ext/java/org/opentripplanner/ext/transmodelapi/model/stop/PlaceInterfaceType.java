package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class PlaceInterfaceType {

  public static GraphQLInterfaceType create(
      GraphQLOutputType quayType,
      GraphQLOutputType stationType,
      GraphQLOutputType bikeRentalStationType,
      GraphQLOutputType bikeParkType
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

          // TODO OTP2 - Add support for Station, osv

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
