package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.OffsetDateTime;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle.StopRelationship;
import org.opentripplanner.transit.model.timetable.Trip;

public class VehiclePositionImpl implements GraphQLDataFetchers.GraphQLVehiclePosition {

  @Override
  public DataFetcher<Double> heading() {
    return env -> getSource(env).heading().orElse(null);
  }

  @Override
  public DataFetcher<String> label() {
    return env -> getSource(env).label().orElse(null);
  }

  @Override
  public DataFetcher<OffsetDateTime> lastUpdate() {
    return env -> {
      var zoneId = env.<GraphQLRequestContext>getContext().transitService().getTimeZone();
      return getSource(env).time().map(time -> OffsetDateTime.ofInstant(time, zoneId)).orElse(null);
    };
  }

  @Override
  public DataFetcher<Long> lastUpdated() {
    return env -> getSource(env).time().map(time -> time.getEpochSecond()).orElse(null);
  }

  @Override
  public DataFetcher<Double> lat() {
    return env ->
      getSource(env).coordinates().map(coordinates -> coordinates.latitude()).orElse(null);
  }

  @Override
  public DataFetcher<Double> lon() {
    return env ->
      getSource(env).coordinates().map(coordinates -> coordinates.longitude()).orElse(null);
  }

  @Override
  public DataFetcher<Double> speed() {
    return env -> getSource(env).speed().orElse(null);
  }

  @Override
  public DataFetcher<StopRelationship> stopRelationship() {
    return env -> getSource(env).stop().orElse(null);
  }

  @Override
  public DataFetcher<Trip> trip() {
    return env -> getSource(env).trip();
  }

  @Override
  public DataFetcher<String> vehicleId() {
    return env -> getSource(env).vehicleId().map(vehicleId -> vehicleId.toString()).orElse(null);
  }

  private RealtimeVehicle getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
