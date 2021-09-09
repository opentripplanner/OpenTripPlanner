package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLRequestContext;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;

public class LegacyGraphQLPlaceImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLPlace {

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).place.name;
  }

  @Override
  public DataFetcher<String> vertexType() {
    return environment -> getSource(environment).place.vertexType.name();
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).place.coordinate.latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).place.coordinate.longitude();
  }

  @Override
  public DataFetcher<Long> arrivalTime() {
    return environment -> getSource(environment).arrival.getTime().getTime();
  }

  @Override
  public DataFetcher<Long> departureTime() {
    return environment -> getSource(environment).departure.getTime().getTime();
  }

  @Override
  public DataFetcher<Object> stop() {
    return environment -> {
      Place place = getSource(environment).place;
      return place.vertexType.equals(VertexType.TRANSIT) ?
          getRoutingService(environment).getStopForId(place.stopId) : null;
    };
  }

  @Override
  public DataFetcher<VehicleRentalStation> bikeRentalStation() {
    return environment -> {
      Place place = getSource(environment).place;

      if (!place.vertexType.equals(VertexType.BIKESHARE)) { return null; }

      VehicleRentalStationService vehicleRentalStationService = getRoutingService(environment)
          .getVehicleRentalStationService();

      if (vehicleRentalStationService == null) { return null; }

      return vehicleRentalStationService.getVehicleRentalStation(place.bikeShareId);
    };
  }

  // TODO
  @Override
  public DataFetcher<BikePark> bikePark() {
    return environment -> null;
  }

  // TODO
  @Override
  public DataFetcher<Object> carPark() {
    return environment -> null;
  }

  private RoutingService getRoutingService(DataFetchingEnvironment environment) {
    return environment.<LegacyGraphQLRequestContext>getContext().getRoutingService();
  }

  private StopArrival getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
