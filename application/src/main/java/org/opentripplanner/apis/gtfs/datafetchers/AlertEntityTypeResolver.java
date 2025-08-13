package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.apis.gtfs.model.RouteTypeModel;
import org.opentripplanner.apis.gtfs.model.StopOnRouteModel;
import org.opentripplanner.apis.gtfs.model.StopOnTripModel;
import org.opentripplanner.apis.gtfs.model.UnknownModel;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.model.timetable.Trip;

public class AlertEntityTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof StopLocation || o instanceof StopLocationsGroup) {
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
