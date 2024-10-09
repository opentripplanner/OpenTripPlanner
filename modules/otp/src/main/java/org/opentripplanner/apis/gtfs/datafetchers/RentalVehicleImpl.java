package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;

public class RentalVehicleImpl implements GraphQLDataFetchers.GraphQLRentalVehicle {

  @Override
  public DataFetcher<Boolean> allowPickupNow() {
    return environment -> getSource(environment).allowPickupNow();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("RentalVehicle", getSource(environment).getId().toString());
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).getLatitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).getLongitude();
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).getName().toString(environment.getLocale());
  }

  @Override
  public DataFetcher<String> network() {
    return environment -> getSource(environment).getNetwork();
  }

  @Override
  public DataFetcher<Boolean> operative() {
    return environment -> getSource(environment).isAllowPickup();
  }

  @Override
  public DataFetcher<VehicleRentalStationUris> rentalUris() {
    return environment -> getSource(environment).getRentalUris();
  }

  @Override
  public DataFetcher<String> vehicleId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<RentalVehicleType> vehicleType() {
    return environment -> getSource(environment).vehicleType;
  }

  @Override
  public DataFetcher<VehicleRentalSystem> rentalNetwork() {
    return environment -> getSource(environment).getVehicleRentalSystem();
  }

  private VehicleRentalVehicle getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
