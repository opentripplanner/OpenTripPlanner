package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers.LegacyGraphQLVehiclePosition;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition;
import org.opentripplanner.service.vehiclepositions.model.RealtimeVehiclePosition.StopRelationship;
import org.opentripplanner.transit.model.timetable.Trip;

public class VehiclePositionImpl implements LegacyGraphQLVehiclePosition {

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
    return env -> getSource(env).time().getEpochSecond();
  }

  @Override
  public DataFetcher<Double> lat() {
    return env -> getSource(env).coordinates().latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return env -> getSource(env).coordinates().longitude();
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
    return env -> getSource(env).vehicleId().toString();
  }

  private RealtimeVehiclePosition getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
