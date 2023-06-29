package org.opentripplanner.street.search.state;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.ElevatorOffboardVertex;
import org.opentripplanner.street.model.vertex.ElevatorOnboardVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.transit.model.basic.Accessibility;

/**
 * Builds up a state chain for use in tests.
 */
public class TestStateBuilder {

  private static final Instant DEFAULT_START_TIME = OffsetDateTime
    .parse("2023-04-18T12:00:00+02:00")
    .toInstant();
  private int count = 1;

  private State currentState;

  private TestStateBuilder(StreetMode mode) {
    currentState =
      new State(
        StreetModelForTest.intersectionVertex(count, count),
        StreetSearchRequest.of().withMode(mode).withStartTime(DEFAULT_START_TIME).build()
      );
  }

  /**
   * Create an initial state that starts walking.
   */
  public static TestStateBuilder ofWalking() {
    return new TestStateBuilder(StreetMode.WALK);
  }

  /**
   * Create an initial state that start in a car.
   */
  public static TestStateBuilder ofDriving() {
    return new TestStateBuilder(StreetMode.CAR);
  }

  public static TestStateBuilder ofCarRental() {
    return new TestStateBuilder(StreetMode.CAR_RENTAL);
  }

  /**
   * Traverse a very plain street edge with no special characteristics.
   */
  public TestStateBuilder streetEdge() {
    count++;
    var from = (StreetVertex) currentState.vertex;
    var to = StreetModelForTest.intersectionVertex(count, count);

    var edge = StreetModelForTest.streetEdge(from, to);
    var states = edge.traverse(currentState);
    if (states.length != 1) {
      throw new IllegalStateException("Only single state transitions are supported.");
    }
    currentState = states[0];
    return this;
  }

  /**
   * Traverse a street edge and switch to Car mode
   */
  public TestStateBuilder pickUpCar() {
    count++;

    var station = TestVehicleRentalStationBuilder.of().withVehicleTypeCar().build();

    VehicleRentalPlaceVertex vertex = new VehicleRentalPlaceVertex(null, station);
    var link = new StreetVehicleRentalLink((StreetVertex) currentState.vertex, vertex);

    currentState = link.traverse(currentState)[0];

    var edge = new VehicleRentalEdge(vertex, RentalFormFactor.CAR);

    State[] traverse = edge.traverse(currentState);
    currentState =
      Arrays
        .stream(traverse)
        .filter(it -> it.getNonTransitMode() == TraverseMode.CAR)
        .findFirst()
        .get();

    return this;
  }

  /**
   * Traverse an elevator (onboard, hop and offboard edges).
   */
  public TestStateBuilder elevator() {
    count++;

    var onboard1 = elevatorOnBoard(count, "1");
    var onboard2 = elevatorOnBoard(count, "2");
    var offboard1 = elevatorOffBoard(count, "1");
    var offboard2 = elevatorOffBoard(count, "2");

    var from = (StreetVertex) currentState.vertex;
    var link = StreetModelForTest.streetEdge(from, offboard1);

    var boardEdge = new ElevatorBoardEdge(offboard1, onboard1);

    var hopEdge = new ElevatorHopEdge(
      onboard1,
      onboard2,
      StreetTraversalPermission.PEDESTRIAN,
      Accessibility.POSSIBLE
    );

    var alightEdge = new ElevatorAlightEdge(onboard2, offboard2, new NonLocalizedString("1"));

    currentState =
      EdgeTraverser
        .traverseEdges(currentState, List.of(link, boardEdge, hopEdge, alightEdge))
        .orElseThrow();
    return this;
  }

  @Nonnull
  private static ElevatorOffboardVertex elevatorOffBoard(int count, String suffix) {
    final String label = "elevator_off_board_" + suffix;
    return new ElevatorOffboardVertex(null, label, count, count, new NonLocalizedString(label));
  }

  @Nonnull
  private static ElevatorOnboardVertex elevatorOnBoard(int count, String suffix) {
    final String label = "elevator_on_board_" + suffix;
    return new ElevatorOnboardVertex(null, label, count, count, new NonLocalizedString(label));
  }

  public State build() {
    return currentState;
  }
}
