package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.FareRuleSet;

public class LegacyGraphQLNodeTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof Agency) { return schema.getObjectType("Agency"); }
    if (o instanceof TransitAlert) { return schema.getObjectType("Alert"); }
    if (o instanceof BikePark) { return schema.getObjectType("BikePark"); }
    if (o instanceof BikeRentalStation) { return schema.getObjectType("BikeRentalStation"); }
    // if (o instanceof CarPark) { return schema.getObjectType("CarPark"); }
    // if (o instanceof Cluster) { return schema.getObjectType("Cluster"); }
    if (o instanceof PatternAtStop) { return schema.getObjectType("DepartureRow"); }
    if (o instanceof TripPattern) { return schema.getObjectType("Pattern"); }
    if (o instanceof PlaceAtDistance) { return schema.getObjectType("placeAtDistance"); }
    if (o instanceof Route) { return schema.getObjectType("Route"); }
    if (o instanceof Stop) { return schema.getObjectType("Stop"); }
    if (o instanceof Station) { return schema.getObjectType("Stop"); }
    if (o instanceof TripTimeShort) { return schema.getObjectType("Stoptime"); }
    if (o instanceof NearbyStop) { return schema.getObjectType("stopAtDistance"); }
    if (o instanceof FareRuleSet) { return schema.getObjectType("TicketType"); }
    if (o instanceof Trip) { return schema.getObjectType("Trip"); }
    return null;
  }
}
