package org.opentripplanner.street.api.path;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.api.path.step.StatesToStreetStepsMapper;
import org.opentripplanner.street.api.path.step.StreetStep;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * A {@link StreetLeg} backed by the states of a sub-path of a {@link StreetPath}.
 * <p>
 * The expensive parts, like the turn-by-turn directions, are computed lazily on first access.
 */
final class DefaultStreetLeg implements StreetLeg {

  private final DefaultStreetPath path;

  /**
   * The time the leg starts. This is usually the time of the first state of the path, but if the
   * leg starts by leaving a vehicle parking, it is the time before parking. This way the time for
   * parking is included in the leg.
   */
  private final Instant startTime;

  /**
   * Weight not part of the path, but added to the leg's cost. This is the cost for parking if the
   * leg starts by leaving a vehicle parking, otherwise zero.
   */
  private final double extraWeight;

  @Nullable
  private final DefaultStreetLeg previous;

  private final double ellipsoidToGeoidDifference;

  private List<StreetStep> steps;

  /**
   * @param startTime   The time the leg starts. Either the time of the first state of the path or,
   *                    if the leg starts by leaving a vehicle parking, the time before parking.
   * @param extraWeight Weight added to the weight of the path, like the cost for parking.
   * @param previous    The street leg immediately preceding this one, if any. It is used to
   *                    compute the relative direction of the first step correctly.
   */
  DefaultStreetLeg(
    DefaultStreetPath path,
    Instant startTime,
    double extraWeight,
    @Nullable DefaultStreetLeg previous,
    double ellipsoidToGeoidDifference
  ) {
    this.path = path;
    this.startTime = startTime;
    this.extraWeight = extraWeight;
    this.previous = previous;
    this.ellipsoidToGeoidDifference = ellipsoidToGeoidDifference;
  }

  @Override
  public TraverseMode computeMode() {
    return (
      path
        .states()
        .stream()
        // The first state is part of the previous leg
        .skip(1)
        .map(state -> {
          var mode = state.currentMode();
          if (mode != null && state.isRentingVehicle()) {
            return state.stateData.rentalVehicleFormFactor.traverseMode;
          }
          return mode;
        })
        .filter(Objects::nonNull)
        .findFirst()
        // Fallback to walking
        .orElse(TraverseMode.WALK)
    );
  }

  @Override
  public Instant startTime() {
    return startTime;
  }

  @Override
  public Instant endTime() {
    return path.endTime();
  }

  @Override
  public StreetLegPlace from() {
    return StreetLegPlace.of(path.states().getFirst());
  }

  @Override
  public StreetLegPlace to() {
    return StreetLegPlace.of(path.lastState());
  }

  @Override
  public double distanceMeters() {
    return path.distanceMeters();
  }

  @Override
  public int generalizedCost() {
    return IntUtils.round(path.weight() + extraWeight);
  }

  @Override
  public LineString geometry() {
    return path.geometry();
  }

  @Override
  @Nullable
  public ElevationProfile elevationProfile() {
    return path.elevation(
      path.states().getFirst().getRequest().geoidElevation(),
      ellipsoidToGeoidDifference
    );
  }

  @Override
  public List<StreetStep> steps() {
    if (steps == null) {
      steps = new StatesToStreetStepsMapper(
        path.states(),
        previousStep(),
        ellipsoidToGeoidDifference
      ).generateSteps();
    }
    return steps;
  }

  @Override
  public boolean rentedVehicle() {
    return path.states().getFirst().isRentingVehicle();
  }

  @Override
  @Nullable
  public String vehicleRentalNetwork() {
    var firstState = path.states().getFirst();
    return firstState.isRentingVehicle() ? firstState.getVehicleRentalNetwork() : null;
  }

  @Nullable
  private StreetStep previousStep() {
    if (previous == null) {
      return null;
    }
    var previousSteps = previous.steps();
    return previousSteps.isEmpty() ? null : previousSteps.getLast();
  }
}
