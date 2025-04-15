package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.framework.graphql.GraphQLUtils.getLocale;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleEntityCounts;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;

public class VehicleRentalStationImpl implements GraphQLDataFetchers.GraphQLVehicleRentalStation {

  @Override
  public DataFetcher<Boolean> allowDropoff() {
    return environment -> getSource(environment).isAllowDropoff();
  }

  @Override
  public DataFetcher<Boolean> allowDropoffNow() {
    return environment -> getSource(environment).allowDropoffNow();
  }

  @Override
  public DataFetcher<Boolean> allowOverloading() {
    return environment -> getSource(environment).overloadingAllowed();
  }

  @Override
  public DataFetcher<Boolean> allowPickup() {
    return environment -> getSource(environment).isAllowPickup();
  }

  @Override
  public DataFetcher<Boolean> allowPickupNow() {
    return environment -> getSource(environment).allowPickupNow();
  }

  @Override
  public DataFetcher<Integer> capacity() {
    return environment -> getSource(environment).getCapacity();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("VehicleRentalStation", getSource(environment).getId().toString());
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
    return environment -> getSource(environment).getName().toString(getLocale(environment));
  }

  @Override
  public DataFetcher<String> network() {
    return environment -> getSource(environment).getNetwork();
  }

  @Override
  public DataFetcher<Boolean> operative() {
    return environment ->
      getSource(environment).isAllowPickup() && getSource(environment).isAllowDropoff();
  }

  @Override
  public DataFetcher<Boolean> realtime() {
    return environment -> getSource(environment).isRealTimeData();
  }

  @Override
  public DataFetcher<VehicleRentalStationUris> rentalUris() {
    return environment -> getSource(environment).getRentalUris();
  }

  @Override
  public DataFetcher<Integer> spacesAvailable() {
    return environment -> getSource(environment).getSpacesAvailable();
  }

  @Override
  public DataFetcher<String> stationId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Integer> vehiclesAvailable() {
    return environment -> getSource(environment).getVehiclesAvailable();
  }

  @Override
  public DataFetcher<RentalVehicleEntityCounts> availableVehicles() {
    return environment -> getSource(environment).getVehicleTypeCounts();
  }

  @Override
  public DataFetcher<RentalVehicleEntityCounts> availableSpaces() {
    return environment -> getSource(environment).getVehicleSpaceCounts();
  }

  @Override
  public DataFetcher<VehicleRentalSystem> rentalNetwork() {
    return environment -> getSource(environment).getVehicleRentalSystem();
  }

  private VehicleRentalStation getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
