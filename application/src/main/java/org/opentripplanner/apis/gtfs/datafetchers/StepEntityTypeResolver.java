package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.apis.gtfs.model.RouteTypeModel;
import org.opentripplanner.apis.gtfs.model.StopOnRouteModel;
import org.opentripplanner.apis.gtfs.model.StopOnTripModel;
import org.opentripplanner.apis.gtfs.model.UnknownModel;
import org.opentripplanner.model.plan.Entrance;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;

public class StepEntityTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof Entrance) {
      return schema.getObjectType("Entrance");
    }
    return null;
  }
}
