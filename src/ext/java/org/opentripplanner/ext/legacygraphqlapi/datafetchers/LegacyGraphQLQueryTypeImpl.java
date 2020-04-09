package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.FareRuleSet;

public class LegacyGraphQLQueryTypeImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLQueryType {

  @Override
  public DataFetcher<Object> node() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<FeedInfo>> feeds() {
    return environment -> environment.<LegacyGraphQLRequestContext>getContext()
        .getRoutingService()
        .getFeedInfoForId()
        .values();
  }

  @Override
  public DataFetcher<Iterable<Agency>> agencies() {
    return environment -> environment.<LegacyGraphQLRequestContext>getContext()
        .getRoutingService()
        .getAllAgencies();
  }

  @Override
  public DataFetcher<Iterable<FareRuleSet>> ticketTypes() {
    return null;
  }

  @Override
  public DataFetcher<Agency> agency() {
    return environment -> {
      FeedScopedId id = GtfsLibrary.convertIdFromString(
          new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAgencyArgs(
              environment.getArguments()).getLegacyGraphQLId());

      return environment.<LegacyGraphQLRequestContext>getContext()
          .getRoutingService()
          .getAgencies(id.getFeedId()).stream().;
    };
  }

  @Override
  public DataFetcher<Iterable<Stop>> stops() {
    return environment -> environment.<LegacyGraphQLRequestContext>getContext()
        .getRoutingService()
        .getStopForId()
        .values();
  }

  @Override
  public DataFetcher<Iterable<Stop>> stopsByBbox() {
    return null;
  }

  @Override
  public DataFetcher<Object> stopsByRadius() {
    return null;
  }

  @Override
  public DataFetcher<Object> nearest() {
    return null;
  }

  @Override
  public DataFetcher<Object> departureRow() {
    return null;
  }

  @Override
  public DataFetcher<Stop> stop() {
    return null;
  }

  @Override
  public DataFetcher<Stop> station() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Stop>> stations() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return null;
  }

  @Override
  public DataFetcher<Route> route() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Trip>> trips() {
    return null;
  }

  @Override
  public DataFetcher<Trip> trip() {
    return null;
  }

  @Override
  public DataFetcher<Trip> fuzzyTrip() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<TripTimeShort>> cancelledTripTimes() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<TripPattern>> patterns() {
    return null;
  }

  @Override
  public DataFetcher<TripPattern> pattern() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Object>> clusters() {
    return null;
  }

  @Override
  public DataFetcher<Object> cluster() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Alert>> alerts() {
    return null;
  }

  @Override
  public DataFetcher<Object> serviceTimeRange() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<BikeRentalStation>> bikeRentalStations() {
    return null;
  }

  @Override
  public DataFetcher<BikeRentalStation> bikeRentalStation() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<BikePark>> bikeParks() {
    return null;
  }

  @Override
  public DataFetcher<BikePark> bikePark() {
    return null;
  }

  @Override
  public DataFetcher<Iterable<Object>> carParks() {
    return null;
  }

  @Override
  public DataFetcher<Object> carPark() {
    return null;
  }

  @Override
  public DataFetcher<Object> viewer() {
    return null;
  }

  @Override
  public DataFetcher<TripPlan> plan() {
    return null;
  }
}
