package org.opentripplanner.model.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.note.StreetNote;

public class WalkStepBuilder {

  private final Set<StreetNote> streetNotes = new HashSet<>();
  private I18NString directionText;
  private WgsCoordinate startLocation;
  private boolean bogusName = false;
  private double angle;
  private boolean walkingBike = false;
  private boolean area = false;
  private AbsoluteDirection absoluteDirection;
  private RelativeDirection relativeDirection;
  private ElevationProfile elevationProfile;
  private String exit;
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

  public WalkStepBuilder withBogusName(boolean bogusName) {
    this.bogusName = bogusName;
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

  public WalkStepBuilder withStayOn(boolean stayOn) {
    this.stayOn = stayOn;
    return this;
  }

  public WalkStepBuilder withDirections(double lastAngle, double thisAngle, boolean roundabout) {
    relativeDirection = RelativeDirection.calculate(lastAngle, thisAngle, roundabout);
    withAbsoluteDirection(thisAngle);
    return this;
  }

  public WalkStepBuilder withAbsoluteDirection(double thisAngle) {
    int octant = (8 + IntUtils.round(thisAngle * 8 / (Math.PI * 2))) % 8;
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

  public ElevationProfile elevationProfile() {
    return elevationProfile;
  }

  public double distance() {
    return distance;
  }

  public WalkStepBuilder addEdge(Edge edge) {
    this.edges.add(edge);
    return this;
  }

  @Nullable
  public String directionTextNoParens() {
    var str = directionText.toString();
    if (str == null) {
      return null; //Avoid null reference exceptions with pathways which don't have names
    }
    int idx = str.indexOf('(');
    if (idx > 0) {
      return str.substring(0, idx - 1);
    }
    return str;
  }

  public WalkStepBuilder addStreetNotes(Set<StreetNote> notes) {
    this.streetNotes.addAll(notes);
    return this;
  }

  public I18NString directionText() {
    return directionText;
  }

  public boolean bogusName() {
    return bogusName;
  }

  public RelativeDirection relativeDirection() {
    return relativeDirection;
  }

  public WalkStep build() {
    return new WalkStep(
      startLocation,
      relativeDirection,
      absoluteDirection,
      directionText,
      streetNotes,
      exit,
      elevationProfile,
      bogusName,
      walkingBike,
      area,
      stayOn,
      angle,
      distance,
      edges
    );
  }
}
