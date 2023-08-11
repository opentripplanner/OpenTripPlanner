package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLAbsoluteDirection;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes.GraphQLRelativeDirection;
import org.opentripplanner.ext.gtfsgraphqlapi.mapping.StreetNoteMapper;
import org.opentripplanner.model.plan.ElevationProfile.Step;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.alertpatch.TransitAlert;

public class stepImpl implements GraphQLDataFetchers.GraphQLStep {

  @Override
  public DataFetcher<GraphQLAbsoluteDirection> absoluteDirection() {
    return environment ->
      getSource(environment)
        .getAbsoluteDirection()
        .map(dir ->
          switch (dir) {
            case NORTH -> GraphQLAbsoluteDirection.NORTH;
            case NORTHEAST -> GraphQLAbsoluteDirection.NORTHEAST;
            case EAST -> GraphQLAbsoluteDirection.EAST;
            case SOUTHEAST -> GraphQLAbsoluteDirection.SOUTHEAST;
            case SOUTH -> GraphQLAbsoluteDirection.SOUTH;
            case SOUTHWEST -> GraphQLAbsoluteDirection.SOUTHWEST;
            case WEST -> GraphQLAbsoluteDirection.WEST;
            case NORTHWEST -> GraphQLAbsoluteDirection.NORTHWEST;
          }
        )
        .orElse(null);
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
    return environment -> getSource(environment).getBogusName();
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
    return environment -> getSource(environment).getExit();
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
  public DataFetcher<GraphQLRelativeDirection> relativeDirection() {
    return environment ->
      switch (getSource(environment).getRelativeDirection()) {
        case DEPART -> GraphQLRelativeDirection.DEPART;
        case HARD_LEFT -> GraphQLRelativeDirection.HARD_LEFT;
        case LEFT -> GraphQLRelativeDirection.LEFT;
        case SLIGHTLY_LEFT -> GraphQLRelativeDirection.SLIGHTLY_LEFT;
        case CONTINUE -> GraphQLRelativeDirection.CONTINUE;
        case SLIGHTLY_RIGHT -> GraphQLRelativeDirection.SLIGHTLY_RIGHT;
        case RIGHT -> GraphQLRelativeDirection.RIGHT;
        case HARD_RIGHT -> GraphQLRelativeDirection.HARD_RIGHT;
        case CIRCLE_CLOCKWISE -> GraphQLRelativeDirection.CIRCLE_CLOCKWISE;
        case CIRCLE_COUNTERCLOCKWISE -> GraphQLRelativeDirection.CIRCLE_COUNTERCLOCKWISE;
        case ELEVATOR -> GraphQLRelativeDirection.ELEVATOR;
        case UTURN_LEFT -> GraphQLRelativeDirection.UTURN_LEFT;
        case UTURN_RIGHT -> GraphQLRelativeDirection.UTURN_RIGHT;
        case ENTER_STATION -> GraphQLRelativeDirection.ENTER_STATION;
        case EXIT_STATION -> GraphQLRelativeDirection.EXIT_STATION;
        case FOLLOW_SIGNS -> GraphQLRelativeDirection.FOLLOW_SIGNS;
      };
  }

  @Override
  public DataFetcher<Boolean> stayOn() {
    return environment -> getSource(environment).getStayOn();
  }

  @Override
  public DataFetcher<String> streetName() {
    return environment -> getSource(environment).getStreetName().toString(environment.getLocale());
  }

  @Override
  public DataFetcher<Boolean> walkingBike() {
    return environment -> getSource(environment).isWalkingBike();
  }

  private WalkStep getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
