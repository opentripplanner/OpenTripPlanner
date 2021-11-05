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
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

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
    return environment -> getSource(environment).place.stop;
  }

  @Override
  public DataFetcher<VehicleRentalPlace> bikeRentalStation() {
    return environment -> {
      Place place = getSource(environment).place;

      if (!place.vertexType.equals(VertexType.BIKESHARE)) { return null; }

      return place.vehicleRentalStation;
    };
  }

  @Override
  public DataFetcher<VehicleRentalStation> vehicleRentalStation() {
    return environment -> {
      Place place = getSource(environment).place;

      if (!place.vertexType.equals(VertexType.BIKESHARE)
              || !(place.vehicleRentalStation instanceof VehicleRentalStation)) {
        return null;
      }

      return (VehicleRentalStation) place.vehicleRentalStation;
    };
  }

  @Override
  public DataFetcher<VehicleRentalVehicle> rentalVehicle() {
    return environment -> {
      Place place = getSource(environment).place;

      if (!place.vertexType.equals(VertexType.BIKESHARE)
              || !(place.vehicleRentalStation instanceof VehicleRentalVehicle)) {
        return null;
      }

      return (VehicleRentalVehicle) place.vehicleRentalStation;
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
