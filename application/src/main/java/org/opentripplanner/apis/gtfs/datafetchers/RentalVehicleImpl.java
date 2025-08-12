package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.framework.graphql.GraphQLUtils.getLocale;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.OffsetDateTime;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleFuel;
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
  public DataFetcher<RentalVehicleFuel> fuel() {
    return environment -> getSource(environment).fuel();
  }

  @Override
  public DataFetcher<OffsetDateTime> availableUntil() {
    return environment -> getSource(environment).getAvailableUntil();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("RentalVehicle", getSource(environment).id().toString());
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).longitude();
  }

  @Override
  public DataFetcher<String> name() {
    return environment -> getSource(environment).name().toString(getLocale(environment));
  }

  @Override
  public DataFetcher<String> network() {
    return environment -> getSource(environment).network();
  }

  @Override
  public DataFetcher<Boolean> operative() {
    return environment -> getSource(environment).isAllowPickup();
  }

  @Override
  public DataFetcher<VehicleRentalStationUris> rentalUris() {
    return environment -> getSource(environment).rentalUris();
  }

  @Override
  public DataFetcher<String> vehicleId() {
    return environment -> getSource(environment).id().toString();
  }

  @Override
  public DataFetcher<RentalVehicleType> vehicleType() {
    return environment -> getSource(environment).vehicleType();
  }

  @Override
  public DataFetcher<VehicleRentalSystem> rentalNetwork() {
    return environment -> getSource(environment).vehicleRentalSystem();
  }

  private VehicleRentalVehicle getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
