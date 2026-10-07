package org.opentripplanner.model.plan.walkstep;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.VerticalTransportationUse;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.model.path.step.AbsoluteDirection;
import org.opentripplanner.street.model.path.step.RelativeDirection;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.lang.IntUtils;

public class WalkStepBuilder {

  private I18NString directionText;
  private WgsCoordinate startLocation;
  private boolean nameIsDerived = false;
  private double angle;
  private boolean walkingBike = false;
  private boolean area = false;
  private AbsoluteDirection absoluteDirection;
  private RelativeDirection relativeDirection;
  private ElevationProfile elevationProfile;
  private String exit;

  @Nullable
  private Entrance entrance;

  @Nullable
  private VerticalTransportationUse verticalTransportationUse;

  private boolean stayOn = false;
  /**
   * Distance used for appending elevation profiles
   */
  private double distance = 0;
  private final List<Edge> edges = new ArrayList<>();

  WalkStepBuilder() {}

  public WalkStepBuilder withDirectionText(I18NString streetName) {
    this.directionText = streetName;
    return this;
  }

  public WalkStepBuilder withStartLocation(WgsCoordinate startLocation) {
    this.startLocation = startLocation;
    return this;
  }

  public WalkStepBuilder withNameIsDerived(boolean nameIsDerived) {
    this.nameIsDerived = nameIsDerived;
    return this;
  }

  public WalkStepBuilder withAngle(double angle) {
    this.angle = angle;
    return this;
  }

  public WalkStepBuilder withWalkingBike(boolean walkingBike) {
    this.walkingBike = walkingBike;
    return this;
  }

  public WalkStepBuilder withArea(boolean area) {
    this.area = area;
    return this;
  }

  public WalkStepBuilder withRelativeDirection(RelativeDirection direction) {
    this.relativeDirection = direction;
    return this;
  }

  public WalkStepBuilder withExit(String exit) {
    this.exit = exit;
    return this;
  }

  public WalkStepBuilder withEntrance(@Nullable Entrance entrance) {
    this.entrance = entrance;
    return this;
  }

  public WalkStepBuilder withVerticalTransportationUse(
    @Nullable VerticalTransportationUse verticalTransportationUse
  ) {
    this.verticalTransportationUse = verticalTransportationUse;
    return this;
  }

  public WalkStepBuilder withStayOn(boolean stayOn) {
    this.stayOn = stayOn;
    return this;
  }

  public WalkStepBuilder withDirections(double lastAngle, double thisAngle, boolean roundabout) {
    relativeDirection = RelativeDirection.calculate(lastAngle, thisAngle, roundabout);
    withAbsoluteDirection(thisAngle);
    return this;
  }

  public WalkStepBuilder withAbsoluteDirection(@Nullable AbsoluteDirection absoluteDirection) {
    this.absoluteDirection = absoluteDirection;
    return this;
  }

  public WalkStepBuilder withAbsoluteDirection(double thisAngle) {
    int octant = (8 + IntUtils.round((thisAngle * 8) / (Math.PI * 2))) % 8;
    absoluteDirection = AbsoluteDirection.values()[octant];
    return this;
  }

  public WalkStepBuilder addDistance(double distance) {
    this.distance = DoubleUtils.roundTo2Decimals(this.distance + distance);
    return this;
  }

  public WalkStepBuilder addElevation(ElevationProfile other) {
    if (elevationProfile == null) {
      elevationProfile = other;
    } else {
      elevationProfile = elevationProfile.add(other);
    }
    return this;
  }

  public WalkStepBuilder addEdge(Edge edge) {
    this.edges.add(edge);
    return this;
  }

  public WalkStep build() {
    return new WalkStep(
      startLocation,
      relativeDirection,
      absoluteDirection,
      directionText,
      exit,
      entrance,
      verticalTransportationUse,
      elevationProfile,
      nameIsDerived,
      walkingBike,
      area,
      stayOn,
      angle,
      distance,
      edges
    );
  }
}
