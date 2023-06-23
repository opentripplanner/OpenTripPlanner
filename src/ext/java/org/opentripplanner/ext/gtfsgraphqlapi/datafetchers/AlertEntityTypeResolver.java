package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.ext.gtfsgraphqlapi.model.RouteTypeModel;
import org.opentripplanner.ext.gtfsgraphqlapi.model.StopOnRouteModel;
import org.opentripplanner.ext.gtfsgraphqlapi.model.StopOnTripModel;
import org.opentripplanner.ext.gtfsgraphqlapi.model.UnknownModel;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;

public class AlertEntityTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof RegularStop) {
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
    if (o instanceof StopOnRouteModel) {
      return schema.getObjectType("StopOnRoute");
    }
    if (o instanceof StopOnTripModel) {
      return schema.getObjectType("StopOnTrip");
    }
    if (o instanceof RouteTypeModel) {
      return schema.getObjectType("RouteType");
    }
    if (o instanceof UnknownModel) {
      return schema.getObjectType("Unknown");
    }

    return null;
  }
}
