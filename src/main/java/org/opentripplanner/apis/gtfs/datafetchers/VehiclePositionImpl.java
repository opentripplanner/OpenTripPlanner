package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle.StopRelationship;
import org.opentripplanner.transit.model.timetable.Trip;

public class VehiclePositionImpl implements GraphQLDataFetchers.GraphQLVehiclePosition {

  @Override
  public DataFetcher<Double> heading() {
    return env -> getSource(env).heading();
  }

  @Override
  public DataFetcher<String> label() {
    return env -> getSource(env).label();
  }

  @Override
  public DataFetcher<Long> lastUpdated() {
    return env -> getSource(env).time() != null ? getSource(env).time().getEpochSecond() : null;
  }

  @Override
  public DataFetcher<Double> lat() {
    return env ->
      getSource(env).coordinates() != null ? getSource(env).coordinates().latitude() : null;
  }

  @Override
  public DataFetcher<Double> lon() {
    return env ->
      getSource(env).coordinates() != null ? getSource(env).coordinates().longitude() : null;
  }

  @Override
  public DataFetcher<Double> speed() {
    return env -> getSource(env).speed();
  }

  @Override
  public DataFetcher<StopRelationship> stopRelationship() {
    return env -> getSource(env).stop();
  }

  @Override
  public DataFetcher<Trip> trip() {
    return env -> getSource(env).trip();
  }

  @Override
  public DataFetcher<String> vehicleId() {
    return env -> getSource(env).vehicleId() != null ? getSource(env).vehicleId().toString() : null;
  }

  private RealtimeVehicle getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
