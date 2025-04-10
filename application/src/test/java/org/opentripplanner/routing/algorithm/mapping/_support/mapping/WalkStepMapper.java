package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import static org.opentripplanner.routing.algorithm.mapping._support.mapping.ElevationMapper.mapElevation;
import static org.opentripplanner.routing.algorithm.mapping._support.mapping.RelativeDirectionMapper.mapRelativeDirection;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.algorithm.mapping._support.model.ApiWalkStep;

@Deprecated
class WalkStepMapper {

  private final Locale locale;

  public WalkStepMapper(Locale locale) {
    this.locale = locale;
  }

  public List<ApiWalkStep> mapWalkSteps(Collection<WalkStep> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(this::mapWalkStep).collect(Collectors.toList());
  }

  public ApiWalkStep mapWalkStep(WalkStep domain) {
    if (domain == null) {
      return null;
    }
    ApiWalkStep api = new ApiWalkStep();

    api.distance = domain.getDistance();
    api.relativeDirection = mapRelativeDirection(domain.getRelativeDirection());
    api.streetName = domain.getDirectionText().toString(locale);
    api.absoluteDirection = domain
      .getAbsoluteDirection()
      .map(AbsoluteDirectionMapper::mapAbsoluteDirection)
      .orElse(null);
    api.exit = domain.highwayExit().orElse(null);
    api.stayOn = domain.isStayOn();
    api.area = domain.getArea();
    api.bogusName = domain.nameIsDerived();
    if (domain.getStartLocation() != null) {
      api.lon = domain.getStartLocation().longitude();
      api.lat = domain.getStartLocation().latitude();
    }
    api.elevation = mapElevation(domain.getElevationProfile());
    api.walkingBike = domain.isWalkingBike();

    return api;
  }
}
