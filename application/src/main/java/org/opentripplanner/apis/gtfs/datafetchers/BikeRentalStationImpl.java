package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.framework.graphql.GraphQLUtils.getLocale;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStationUris;

public class BikeRentalStationImpl implements GraphQLDataFetchers.GraphQLBikeRentalStation {

  final String STATE_ON = "Station on";
  final String STATE_OFF = "Station off";

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
  public DataFetcher<Integer> bikesAvailable() {
    return environment -> getSource(environment).vehiclesAvailable();
  }

  @Override
  public DataFetcher<Integer> capacity() {
    return environment -> getSource(environment).capacity();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("BikeRentalStation", getSource(environment).id().toString());
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
  public DataFetcher<Iterable<String>> networks() {
    return environment -> List.of(getSource(environment).network());
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
    return environment -> getSource(environment).rentalUris();
  }

  @Override
  public DataFetcher<Integer> spacesAvailable() {
    return environment -> getSource(environment).spacesAvailable();
  }

  @Override
  public DataFetcher<String> state() {
    return environment -> {
      var rentalPlace = getSource(environment);

      if (rentalPlace.isFloatingVehicle() && rentalPlace.isAllowPickup()) {
        return STATE_ON;
      } else if (rentalPlace.isAllowDropoff() && rentalPlace.isAllowPickup()) {
        return STATE_ON;
      } else {
        return STATE_OFF;
      }
    };
  }

  @Override
  public DataFetcher<String> stationId() {
    return environment -> getSource(environment).stationId();
  }

  private VehicleRentalPlace getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
