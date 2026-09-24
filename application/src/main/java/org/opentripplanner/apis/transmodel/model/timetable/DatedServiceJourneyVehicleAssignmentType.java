package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.transit.model.timetable.VehicleAssignmentOnServiceDate;

/**
 * The vehicle and vehicle type aimed to operate a dated service journey, and the vehicle expected
 * to operate it based on real-time data.
 */
public class DatedServiceJourneyVehicleAssignmentType {

  private static final String NAME = "DatedServiceJourneyVehicleAssignment";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  private DatedServiceJourneyVehicleAssignmentType() {}

  public static GraphQLObjectType create(FeedScopedIdMapper idMapper) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "The vehicle and vehicle type aimed to operate a dated service journey, and the vehicle " +
          "expected to operate it based on real-time data."
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedVehicleId")
          .description("Id of the vehicle aimed to operate the dated service journey.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> {
            var id = vehicleAssignment(environment).scheduledVehicleId();
            return id == null ? null : idMapper.mapToApi(id);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("aimedVehicleTypeId")
          .description("Id of the type of vehicle aimed to operate the dated service journey.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> vehicleAssignment(environment).scheduledVehicleTypeId())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("expectedVehicleId")
          .description(
            "Id of the vehicle expected to operate the dated service journey. Updated with " +
              "real-time information if available."
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> {
            var id = vehicleAssignment(environment).realtimeVehicleId();
            return id == null ? null : idMapper.mapToApi(id);
          })
          .build()
      )
      .build();
  }

  private static VehicleAssignmentOnServiceDate vehicleAssignment(
    DataFetchingEnvironment environment
  ) {
    return environment.getSource();
  }
}
