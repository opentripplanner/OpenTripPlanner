package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.transit.model.timetable.VehicleAssignment;

/**
 * References to the vehicle assigned to operate a journey.
 */
public class VehicleAssignmentType {

  private static final String NAME = "VehicleAssignment";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  private VehicleAssignmentType() {}

  public static GraphQLObjectType create(FeedScopedIdMapper idMapper) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description("References to the vehicle assigned to operate a journey.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedVehicleId")
          .description(
            "Expected id of the vehicle. Updated with real time information if available."
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> {
            var id = vehicleAssignment(environment).vehicleId();
            return id == null ? null : idMapper.mapToApi(id);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedVehicleTypeId")
          .description("Expected id of the type of vehicle.")
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
