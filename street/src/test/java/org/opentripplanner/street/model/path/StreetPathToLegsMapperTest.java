package org.opentripplanner.street.model.path;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ExternalLegEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class StreetPathToLegsMapperTest {

  @Test
  void singleLeg() {
    var state = TestStateBuilder.ofWalking().streetEdge().streetEdge().build();

    var legs = new StreetPath(state).legs(0);

    assertThat(legs).hasSize(1);
    var leg = legs.getFirst();
    assertThat(leg.mode()).isEqualTo(TraverseMode.WALK);
    assertThat(leg.steps()).hasSize(2);
    assertThat(leg.distanceMeters()).isEqualTo(200);
    assertThat(leg.rentedVehicle()).isFalse();
  }

  @Test
  void carRentalPickUp() {
    var state = TestStateBuilder.ofCarRental().streetEdge().pickUpCarFromStation().build();

    var legs = new StreetPath(state).legs(0);

    assertThat(legs).hasSize(2);
    var walk = legs.get(0);
    var car = legs.get(1);
    assertThat(walk.mode()).isEqualTo(TraverseMode.WALK);
    assertThat(car.mode()).isEqualTo(TraverseMode.CAR);
    assertThat(car.rentedVehicle()).isTrue();
    assertThat(walk.endTime()).isEqualTo(car.startTime());
  }

  @Test
  void externalEdge() {
    var walk = TestStateBuilder.ofWalking().streetEdge().build();
    var edge = new ExternalTestEdge(walk.getVertex(), StreetModelFactory.V3);
    var state = edge.traverse(walk)[0];

    var legs = new StreetPath(state).legs(0);

    assertThat(legs).hasSize(2);
    assertThat(legs.get(0)).isInstanceOf(DefaultStreetLeg.class);
    var external = (ExternalEdgeLeg) legs.get(1);
    assertThat(external.edge()).isSameInstanceAs(edge);
    assertThat(external.mode()).isEqualTo(TraverseMode.FLEX);
    assertThat(external.from().vertex()).isEqualTo(walk.getVertex());
    assertThat(external.to().vertex()).isEqualTo(StreetModelFactory.V3);
    assertThat(external.startTime()).isEqualTo(walk.getTime());
    assertThat(external.endTime()).isEqualTo(walk.getTime().plusSeconds(600));
    assertThat(external.generalizedCost()).isEqualTo(700);
    assertThat(external.distanceMeters()).isEqualTo(5000);
    assertThat(external.geometry()).isEqualTo(edge.getGeometry());
    assertThat(external.rentedVehicle()).isFalse();
    assertThat(external.vehicleRentalNetwork()).isNull();
    assertThat(external.elevationProfile()).isNull();
    assertThat(external.steps()).isEmpty();
  }

  @Test
  void noLegsForEmptyPath() {
    var state = TestStateBuilder.ofWalking().build();

    assertThat(new StreetPath(state).legs(0)).isEmpty();
  }

  /**
   * An edge which is not part of the street network, like a ride on a flexible transit trip.
   */
  private static class ExternalTestEdge extends Edge implements ExternalLegEdge {

    private ExternalTestEdge(Vertex from, Vertex to) {
      super(from, to);
    }

    @Override
    public State[] traverse(State s0) {
      var editor = s0.edit(this);
      editor.setBackMode(TraverseMode.FLEX);
      editor.incrementTimeInSeconds(600);
      editor.incrementWeight(700);
      return editor.makeStateArray();
    }

    @Override
    public I18NString getName() {
      return I18NString.of("external");
    }

    @Override
    public double getDistanceMeters() {
      return 5000;
    }

    @Override
    public LineString getGeometry() {
      return GeometryUtils.makeLineString(
        getFromVertex().getCoordinate(),
        getToVertex().getCoordinate()
      );
    }
  }
}
