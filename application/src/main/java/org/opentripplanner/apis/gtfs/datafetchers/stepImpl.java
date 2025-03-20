package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.framework.graphql.GraphQLUtils.getLocale;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.DirectionMapper;
import org.opentripplanner.apis.gtfs.mapping.StreetNoteMapper;
import org.opentripplanner.model.plan.ElevationProfile.Step;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class stepImpl implements GraphQLDataFetchers.GraphQLStep {

  @Override
  public DataFetcher<GraphQLTypes.GraphQLAbsoluteDirection> absoluteDirection() {
    return environment ->
      getSource(environment).getAbsoluteDirection().map(DirectionMapper::map).orElse(null);
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment ->
      getSource(environment)
        .getStreetNotes()
        .stream()
        .map(StreetNoteMapper::mapStreetNoteToAlert)
        .toList();
  }

  @Override
  public DataFetcher<Boolean> area() {
    return environment -> getSource(environment).getArea();
  }

  @Override
  public DataFetcher<Boolean> bogusName() {
    return environment -> getSource(environment).nameIsDerived();
  }

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).getDistance();
  }

  @Override
  public DataFetcher<Iterable<Step>> elevationProfile() {
    return environment -> getSource(environment).getElevationProfile().steps();
  }

  @Override
  public DataFetcher<String> exit() {
    return environment -> getSource(environment).highwayExit().orElse(null);
  }

  @Override
  public DataFetcher<Object> feature() {
    return environment -> getSource(environment).entrance().orElse(null);
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).getStartLocation().latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).getStartLocation().longitude();
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLRelativeDirection> relativeDirection() {
    return environment -> DirectionMapper.map(getSource(environment).getRelativeDirection());
  }

  @Override
  public DataFetcher<Boolean> stayOn() {
    return environment -> getSource(environment).isStayOn();
  }

  @Override
  public DataFetcher<String> streetName() {
    return environment ->
      getSource(environment).getDirectionText().toString(getLocale(environment));
  }

  @Override
  public DataFetcher<Boolean> walkingBike() {
    return environment -> getSource(environment).isWalkingBike();
  }

  private WalkStep getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
