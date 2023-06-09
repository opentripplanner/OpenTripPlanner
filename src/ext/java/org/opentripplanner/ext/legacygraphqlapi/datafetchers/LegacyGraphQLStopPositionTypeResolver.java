package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLNodeTypeResolver.queryContainsFragment;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.ext.legacygraphqlapi.model.PositionAtStop;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.site.RegularStop;

public class LegacyGraphQLStopPositionTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof PositionAtStop) {
      return schema.getObjectType("PositionAtStop");
    }

    return null;
  }
}
