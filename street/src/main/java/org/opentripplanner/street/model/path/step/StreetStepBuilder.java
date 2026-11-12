package org.opentripplanner.street.model.path.step;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.lang.IntUtils;

public class StreetStepBuilder {

  private I18NString directionText;
  private WgsCoordinate startLocation;
  private boolean nameIsDerived = false;
  private double angle;
  private boolean walkingBike = false;
  private boolean area = false;
  private AbsoluteDirection absoluteDirection;
  private RelativeDirection relativeDirection;
  private ElevationProfile elevationProfile;
  private String highwayExit;

  @Nullable
  private StepEntrance entrance;

  @Nullable
  private VerticalTransportation verticalTransportation;

  private boolean stayOn = false;
  private boolean crossing;
  private double distance = 0;
  private final List<Edge> edges = new ArrayList<>();

  StreetStepBuilder() {}

  public StreetStepBuilder withDirectionText(I18NString streetName) {
    this.directionText = streetName;
    return this;
  }

  public StreetStepBuilder withStartLocation(WgsCoordinate startLocation) {
    this.startLocation = startLocation;
    return this;
  }

  public StreetStepBuilder withNameIsDerived(boolean nameIsDerived) {
    this.nameIsDerived = nameIsDerived;
    return this;
  }

  public StreetStepBuilder withAngle(double angle) {
    this.angle = angle;
    return this;
  }

  public StreetStepBuilder withWalkingBike(boolean walkingBike) {
    this.walkingBike = walkingBike;
    return this;
  }

  public StreetStepBuilder withArea(boolean area) {
    this.area = area;
    return this;
  }

  public StreetStepBuilder withRelativeDirection(RelativeDirection direction) {
    this.relativeDirection = direction;
    return this;
  }

  public StreetStepBuilder withExit(String exit) {
    this.highwayExit = exit;
    return this;
  }

  public StreetStepBuilder withEntrance(@Nullable StepEntrance entrance) {
    this.entrance = entrance;
    return this;
  }

  public StreetStepBuilder withVerticalTransportation(
    @Nullable VerticalTransportation verticalTransportation
  ) {
    this.verticalTransportation = verticalTransportation;
    return this;
  }

  public StreetStepBuilder withStayOn(boolean stayOn) {
    this.stayOn = stayOn;
    return this;
  }

  public StreetStepBuilder withCrossing(boolean crossing) {
    this.crossing = crossing;
    return this;
  }

  public StreetStepBuilder withDirections(double lastAngle, double thisAngle, boolean roundabout) {
    relativeDirection = RelativeDirection.calculate(lastAngle, thisAngle, roundabout);
    withAbsoluteDirection(thisAngle);
    return this;
  }

  public StreetStepBuilder withAbsoluteDirection(double thisAngle) {
    int octant = (8 + IntUtils.round((thisAngle * 8) / (Math.PI * 2))) % 8;
    absoluteDirection = AbsoluteDirection.values()[octant];
    return this;
  }

  public StreetStepBuilder addDistance(double distance) {
    this.distance = DoubleUtils.roundTo2Decimals(this.distance + distance);
    return this;
  }

  public StreetStepBuilder addElevation(ElevationProfile other) {
    if (elevationProfile == null) {
      elevationProfile = other;
    } else {
      elevationProfile = elevationProfile.add(other);
    }
    return this;
  }

  public StreetStepBuilder addEdge(Edge edge) {
    this.edges.add(edge);
    return this;
  }

  @Nullable
  public String directionTextNoParens() {
    var str = directionText.toString();
    if (str == null) {
      // Avoid null reference exceptions with pathways which don't have names
      return null;
    }
    int idx = str.indexOf('(');
    if (idx > 0) {
      return str.substring(0, idx - 1);
    }
    return str;
  }

  public boolean hasEntrance() {
    return entrance != null;
  }

  public I18NString directionText() {
    return directionText;
  }

  /**
   * @see Edge#nameIsDerived()
   */
  public boolean nameIsDerived() {
    return nameIsDerived;
  }

  public RelativeDirection relativeDirection() {
    return relativeDirection;
  }

  public boolean isCrossing() {
    return crossing;
  }

  public ElevationProfile elevationProfile() {
    return elevationProfile;
  }

  public double distance() {
    return distance;
  }

  @Nullable
  public VerticalTransportation verticalTransportation() {
    return verticalTransportation;
  }

  WgsCoordinate startLocation() {
    return startLocation;
  }

  double angle() {
    return angle;
  }

  boolean walkingBike() {
    return walkingBike;
  }

  boolean area() {
    return area;
  }

  AbsoluteDirection absoluteDirection() {
    return absoluteDirection;
  }

  String highwayExit() {
    return highwayExit;
  }

  StepEntrance entrance() {
    return entrance;
  }

  boolean stayOn() {
    return stayOn;
  }

  List<Edge> edges() {
    return edges;
  }

  public StreetStep build() {
    return new StreetStep(this);
  }
}
