package org.opentripplanner.street.model.path;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.model.path.step.StatesToStreetStepsMapper;
import org.opentripplanner.street.model.path.step.StreetStep;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * A {@link StreetLeg} backed by the states of a sub-path of a {@link StreetPath}.
 * <p>
 * The expensive parts, like the turn-by-turn directions, are computed lazily on first access.
 */
final class DefaultStreetLeg implements StreetLeg {

  private final StreetPath path;

  /**
   * The state the leg starts at. This is usually the first state of the path, but if the leg
   * starts by leaving a vehicle parking, it is the state before parking. This way the time and
   * cost for parking is included in the leg.
   */
  private final State startTimeState;

  @Nullable
  private final DefaultStreetLeg previous;

  private final double ellipsoidToGeoidDifference;

  private List<StreetStep> steps;

  /**
   * @param previous The street leg immediately preceding this one, if any. It is used to compute
   *                 the relative direction of the first step correctly.
   */
  DefaultStreetLeg(
    StreetPath path,
    @Nullable DefaultStreetLeg previous,
    double ellipsoidToGeoidDifference
  ) {
    this.path = path;
    this.previous = previous;
    this.ellipsoidToGeoidDifference = ellipsoidToGeoidDifference;

    var firstState = path.states().getFirst();
    var previousStateIsVehicleParking =
      firstState.getBackState() != null && firstState.getBackEdge() instanceof VehicleParkingEdge;
    this.startTimeState = previousStateIsVehicleParking ? firstState.getBackState() : firstState;
  }

  @Override
  public TraverseMode mode() {
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
    return startTimeState.getTime();
  }

  @Override
  public Instant endTime() {
    return path.endTime();
  }

  @Override
  public StreetLegPlace from() {
    return place(path.states().getFirst());
  }

  @Override
  public StreetLegPlace to() {
    return place(path.lastState());
  }

  @Override
  public double distanceMeters() {
    return path.distanceMeters();
  }

  @Override
  public int generalizedCost() {
    var extraWeight = path.states().getFirst().getWeight() - startTimeState.getWeight();
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

  private static StreetLegPlace place(State state) {
    Vertex vertex = state.getVertex();
    I18NString name = vertex.getName();

    // This gets nicer names instead of osm:node:id when changing mode of transport.
    // Names are generated from all the streets in a corner, same as names in origin and
    // destination. We use the name in TemporaryStreetLocation since this name generation already
    // happened when the temporary location was generated.
    if (
      vertex instanceof StreetVertex streetVertex && !(vertex instanceof TemporaryStreetLocation)
    ) {
      name = streetVertex.getIntersectionName();
    }

    boolean realTimeVehicleParking =
      vertex instanceof VehicleParkingEntranceVertex parkingVertex &&
      hasRealTimeVehicleParkingData(parkingVertex, state);

    return new StreetLegPlace(vertex, name, realTimeVehicleParking);
  }

  private static boolean hasRealTimeVehicleParkingData(
    VehicleParkingEntranceVertex vertex,
    State state
  ) {
    TraverseMode traverseMode = null;
    var request = state.getRequest();
    if (request.mode().includesDriving()) {
      traverseMode = TraverseMode.CAR;
    } else if (request.mode().includesBiking()) {
      traverseMode = TraverseMode.BICYCLE;
    }
    return vertex
      .getVehicleParking()
      .hasRealTimeDataForMode(traverseMode, request.wheelchairEnabled());
  }
}
