package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;

public class RentalVehicleTypeType {

    public static final String NAME = "RentalVehicleType";

    public static GraphQLObjectType create() {
        return GraphQLObjectType.newObject()
                .name(NAME)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("vehicleTypeId")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .dataFetcher(environment -> ((RentalVehicleType) environment.getSource()).id.getId())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((RentalVehicleType) environment.getSource()).name)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("formFactor")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .dataFetcher(environment -> ((RentalVehicleType) environment.getSource()).formFactor)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("propulsionType")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .dataFetcher(environment -> ((RentalVehicleType) environment.getSource()).propulsionType)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("maxRangeMeters")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RentalVehicleType) environment.getSource()).maxRangeMeters)
                        .build())
                .build();
    }
}
