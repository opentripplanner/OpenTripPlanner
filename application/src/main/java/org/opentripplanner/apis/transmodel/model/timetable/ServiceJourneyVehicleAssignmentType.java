package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.transit.model.timetable.VehicleAssignment;

/**
 * The vehicle and vehicle type aimed to operate a service journey, as given in the planned data.
 */
public class ServiceJourneyVehicleAssignmentType {

  private static final String NAME = "ServiceJourneyVehicleAssignment";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  private ServiceJourneyVehicleAssignmentType() {}

  public static GraphQLObjectType create(FeedScopedIdMapper idMapper) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description("The vehicle and vehicle type aimed to operate a service journey.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedVehicleId")
          .description("Id of the vehicle aimed to operate the service journey.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> {
            var id = vehicleAssignment(environment).vehicleId();
            return id == null ? null : idMapper.mapToApi(id);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedVehicleTypeId")
          .description("Id of the type of vehicle aimed to operate the service journey.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> vehicleAssignment(environment).vehicleTypeId())
          .build()
      )
      .build();
  }

  private static VehicleAssignment vehicleAssignment(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
