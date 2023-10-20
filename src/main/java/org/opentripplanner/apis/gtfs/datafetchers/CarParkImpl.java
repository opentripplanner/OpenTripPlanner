package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class CarParkImpl implements GraphQLDataFetchers.GraphQLCarPark {

  @Override
  public DataFetcher<String> carParkId() {
    return environment -> getSource(environment).getId().getId();
  }

  @Override
  public DataFetcher<Relay.ResolvedGlobalId> id() {
    return environment ->
      new Relay.ResolvedGlobalId("CarPark", getSource(environment).getId().toString());
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
  public DataFetcher<Integer> maxCapacity() {
    return environment -> {
      var availability = getSource(environment).getCapacity();
      if (availability == null) {
        return null;
      }
      return availability.getCarSpaces();
    };
  }

  @Override
  public DataFetcher<String> name() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).getName(), environment);
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
  public DataFetcher<Integer> spacesAvailable() {
    return environment -> {
      var availability = getSource(environment).getAvailability();
      if (availability == null) {
        return null;
      }
      return availability.getCarSpaces();
    };
  }

  @Override
  public DataFetcher<Iterable<String>> tags() {
    return environment -> getSource(environment).getTags();
  }

  private VehicleParking getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
