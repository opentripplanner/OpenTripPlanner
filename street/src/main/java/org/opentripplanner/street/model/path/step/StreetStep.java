package org.opentripplanner.street.model.path.step;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents one instruction in the turn-by-turn directions of a {@link
 * org.opentripplanner.street.model.path.StreetLeg}, like "turn right onto Broadway" or "take the
 * elevator".
 * <p>
 * This is the street model's representation of a step. Information which is not part of the
 * street graph (entrance details, levels) is only referenced, see {@link StepEntrance} and
 * {@link VerticalTransportation}.
 */
public final class StreetStep {

  private final WgsCoordinate startLocation;
  private final double distance;
  private final RelativeDirection relativeDirection;
  private final I18NString directionText;

  @Nullable
  private final AbsoluteDirection absoluteDirection;

  private final boolean area;
  private final boolean nameIsDerived;
  private final double angle;
  private final boolean walkingBike;

  @Nullable
  private final String highwayExit;

  @Nullable
  private final StepEntrance entrance;

  @Nullable
  private final VerticalTransportation verticalTransportation;

  @Nullable
  private final ElevationProfile elevationProfile;

  private final boolean stayOn;

  private final List<Edge> edges;

  StreetStep(StreetStepBuilder builder) {
    this.startLocation = Objects.requireNonNull(builder.startLocation());
    this.distance = builder.distance();
    this.relativeDirection = Objects.requireNonNull(builder.relativeDirection());
    this.directionText = builder.directionText();
    this.absoluteDirection = builder.absoluteDirection();
    this.area = builder.area();
    this.nameIsDerived = builder.nameIsDerived();
    this.angle = DoubleUtils.roundTo2Decimals(builder.angle());
    this.walkingBike = builder.walkingBike();
    this.highwayExit = builder.highwayExit();
    this.entrance = builder.entrance();
    this.verticalTransportation = builder.verticalTransportation();
    this.elevationProfile = builder.elevationProfile();
    this.stayOn = builder.stayOn();
    this.edges = List.copyOf(builder.edges());
  }

  public static StreetStepBuilder builder() {
    return new StreetStepBuilder();
  }

  /**
   * The coordinate of start of the step.
   */
  public WgsCoordinate startLocation() {
    return startLocation;
  }

  /**
   * The distance in meters that this step takes.
   */
  public double distance() {
    return distance;
  }

  public RelativeDirection relativeDirection() {
    return relativeDirection;
  }

  /**
   * A piece of information that {@link #relativeDirection()} relates to. This could be the name
   * of the street, but also a station entrance or what is on a sign.
   */
  public I18NString directionText() {
    return directionText;
  }

  /**
   * The absolute direction of this step. Steps like riding an elevator do not have one.
   */
  @Nullable
  public AbsoluteDirection absoluteDirection() {
    return absoluteDirection;
  }

  /**
   * This step is on an open area, such as a plaza or train platform.
   */
  public boolean area() {
    return area;
  }

  /**
   * @see Edge#nameIsDerived()
   */
  public boolean nameIsDerived() {
    return nameIsDerived;
  }

  public double angle() {
    return angle;
  }

  /**
   * Is this step walking with a bike?
   */
  public boolean walkingBike() {
    return walkingBike;
  }

  /**
   * When exiting a highway or traffic circle, the exit name/number.
   */
  @Nullable
  public String highwayExit() {
    return highwayExit;
  }

  @Nullable
  public StepEntrance entrance() {
    return entrance;
  }

  @Nullable
  public VerticalTransportation verticalTransportation() {
    return verticalTransportation;
  }

  @Nullable
  public ElevationProfile elevationProfile() {
    return elevationProfile;
  }

  /**
   * Indicates whether a street changes direction at an intersection.
   */
  public boolean stayOn() {
    return stayOn;
  }

  /**
   * The edges that make up this step.
   */
  public List<Edge> edges() {
    return edges;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(StreetStep.class)
      .addEnum("absoluteDirection", absoluteDirection)
      .addEnum("relativeDirection", relativeDirection)
      .addStr("directionText", directionText == null ? null : directionText.toString())
      .addNum("distance", distance)
      .toString();
  }
}
