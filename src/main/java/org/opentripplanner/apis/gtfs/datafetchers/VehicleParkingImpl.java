package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;

public class VehicleParkingImpl implements GraphQLDataFetchers.GraphQLVehicleParking {

  @Override
  public DataFetcher<Boolean> anyCarPlaces() {
    return environment -> getSource(environment).hasAnyCarPlaces();
  }

  @Override
  public DataFetcher<VehicleParkingSpaces> availability() {
    return environment -> getSource(environment).getAvailability();
  }

  @Override
  public DataFetcher<Boolean> bicyclePlaces() {
    return environment -> getSource(environment).hasBicyclePlaces();
  }

  @Override
  public DataFetcher<VehicleParkingSpaces> capacity() {
    return environment -> getSource(environment).getCapacity();
  }

  @Override
  public DataFetcher<Boolean> carPlaces() {
    return environment -> getSource(environment).hasCarPlaces();
  }

  @Override
  public DataFetcher<String> detailsUrl() {
    return environment -> getSource(environment).getDetailsUrl();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("VehicleParking", getSource(environment).getId().toString());
  }

  @Override
  public DataFetcher<String> imageUrl() {
    return environment -> getSource(environment).getImageUrl();
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).getCoordinate().latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).getCoordinate().longitude();
  }

  @Override
  public DataFetcher<String> name() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).getName(), environment);
  }

  @Override
  public DataFetcher<String> note() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).getNote(), environment);
  }

  @Override
  public DataFetcher<OHCalendar> openingHours() {
    return environment -> getSource(environment).getOpeningHours();
  }

  @Override
  public DataFetcher<Boolean> realtime() {
    return environment -> getSource(environment).hasRealTimeData();
  }

  @Override
  public DataFetcher<VehicleParkingState> state() {
    return environment -> getSource(environment).getState();
  }

  @Override
  public DataFetcher<Iterable<String>> tags() {
    return environment -> getSource(environment).getTags();
  }

  @Override
  public DataFetcher<String> vehicleParkingId() {
    return environment -> getSource(environment).getId().toString();
  }

  @Override
  public DataFetcher<Boolean> wheelchairAccessibleCarPlaces() {
    return environment -> getSource(environment).hasWheelchairAccessibleCarPlaces();
  }

  private VehicleParking getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
