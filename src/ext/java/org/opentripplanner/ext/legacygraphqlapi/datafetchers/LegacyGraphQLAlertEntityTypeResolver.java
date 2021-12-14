package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLRouteTypeModel;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnRouteModel;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLStopOnTripModel;
import org.opentripplanner.ext.legacygraphqlapi.model.LegacyGraphQLUnknownModel;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;

public class LegacyGraphQLAlertEntityTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof Stop) {
      return schema.getObjectType("Stop");
    }
    if (o instanceof Agency) {
      return schema.getObjectType("Agency");
    }
    if (o instanceof Route) {
      return schema.getObjectType("Route");
    }
    if (o instanceof Trip) {
      return schema.getObjectType("Trip");
    }
    if (o instanceof TripPattern) {
      return schema.getObjectType("Pattern");
    }
    if (o instanceof LegacyGraphQLStopOnRouteModel) {
      return schema.getObjectType("StopOnRoute");
    }
    if (o instanceof LegacyGraphQLStopOnTripModel) {
      return schema.getObjectType("StopOnTrip");
    }
    if (o instanceof LegacyGraphQLRouteTypeModel) {
      return schema.getObjectType("RouteType");
    }
    if (o instanceof LegacyGraphQLUnknownModel) {
      return schema.getObjectType("Unknown");
    }

    return null;
  }
}
