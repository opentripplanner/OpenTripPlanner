package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.search.TraverseMode.SCOOTER;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.BusinessAreaBorder;
import org.opentripplanner.service.vehiclerental.street.CompositeRentalRestrictionExtension;
import org.opentripplanner.service.vehiclerental.street.GeofencingZoneExtension;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class RentalRestrictionExtensionTest {

  String network = "tier-oslo";
  StreetVertex V1 = intersectionVertex("V1", 0, 0);
  StreetVertex V2 = intersectionVertex("V2", 1, 1);
  StreetVertex V3 = intersectionVertex("V3", 2, 2);
  StreetVertex V4 = intersectionVertex("V4", 3, 3);

  @Test
  public void leaveBusinessAreaOnFoot() {
    var edge1 = streetEdge(V1, V2);
    var ext = new BusinessAreaBorder(network);
    V2.addRentalRestriction(ext);

    var states = traverse(edge1);
    assertEquals(1, states.length);
    assertEquals(HAVE_RENTED, states[0].getVehicleRentalState());
    assertEquals(TraverseMode.WALK, states[0].getBackMode());
  }

  @Test
  public void dontEnterGeofencingZoneOnFoot() {
    var edge = streetEdge(V1, V2);
    V2.addRentalRestriction(
      new GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, null, true, true)
      )
    );
    var result = traverse(edge)[0];
    assertEquals(WALK, result.getBackMode());
    assertEquals(HAVE_RENTED, result.getVehicleRentalState());
  }

  @Test
  public void forkStateWhenEnteringNoDropOffZone() {
    var edge1 = streetEdge(V4, V1);
    var edge2 = streetEdge(V2, V3);
    var restrictedEdge = streetEdge(V1, V2);

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(edge1.getFromVertex(), req);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER, network, false);
    restrictedEdge.addRentalRestriction(
      new GeofencingZoneExtension(
        new GeofencingZone(new FeedScopedId(network, "a-park"), null, null, true, false)
      )
    );

    var states = edge1.traverse(editor.makeState());

    var continueOnFoot = states[0];
    var continueRenting = states[1];

    assertEquals(HAVE_RENTED, continueOnFoot.getVehicleRentalState());
    assertEquals(WALK, continueOnFoot.getBackMode());

    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
    assertEquals(SCOOTER, continueRenting.getBackMode());
    assertTrue(continueRenting.isInsideNoRentalDropOffArea());

    var insideZone = restrictedEdge.traverse(continueRenting)[0];

    var leftNoDropOff = edge2.traverse(insideZone)[0];
    assertFalse(leftNoDropOff.isInsideNoRentalDropOffArea());
    assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
  }

  @Test
  public void dontFinishInNoDropOffZone() {
    var edge = streetEdge(V1, V2);
    var ext = new GeofencingZoneExtension(
      new GeofencingZone(new FeedScopedId(network, "a-park"), null, null, true, false)
    );
    V2.addRentalRestriction(ext);
    edge.addRentalRestriction(ext);
    State result = traverse(edge)[0];
    assertFalse(result.isFinal());
  }

  @Test
  public void finishInEdgeWithoutRestrictions() {
    var edge = streetEdge(V1, V2);
    State result = traverse(edge)[0];
    assertTrue(result.isFinal());
  }

  @Test
  public void addTwoExtensions() {
    var edge = streetEdge(V1, V2);
    edge.addRentalRestriction(new BusinessAreaBorder("a"));
    edge.addRentalRestriction(new BusinessAreaBorder("b"));

    assertTrue(edge.fromv.rentalTraversalBanned(state("a")));
    assertTrue(edge.fromv.rentalTraversalBanned(state("b")));
  }

  @Test
  public void removeExtensions() {
    var edge = streetEdge(V1, V2);
    var a = new BusinessAreaBorder("a");
    var b = new BusinessAreaBorder("b");
    var c = new BusinessAreaBorder("c");

    edge.addRentalRestriction(a);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("a")));

    edge.addRentalRestriction(b);
    edge.addRentalRestriction(c);

    edge.removeRentalExtension(a);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("b")));
    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("c")));

    edge.removeRentalExtension(b);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(state("c")));
  }

  @Test
  public void checkNetwork() {
    var edge = streetEdge(V1, V2);
    edge.addRentalRestriction(new BusinessAreaBorder("a"));

    var states = traverse(edge);

    assertEquals(1, states.length);

    assertEquals(RENTING_FLOATING, states[0].getVehicleRentalState());
  }

  private State[] traverse(StreetEdge edge) {
    var state = state(network);
    return edge.traverse(state);
  }

  private State state(String network) {
    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();
    var editor = new StateEditor(V1, req);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER, network, false);
    return editor.makeState();
  }

  @Nested
  class Composition {

    RentalRestrictionExtension a = new BusinessAreaBorder("a");
    RentalRestrictionExtension b = new BusinessAreaBorder("b");
    RentalRestrictionExtension c = new GeofencingZoneExtension(
      new GeofencingZone(new FeedScopedId(network, "a-park"), null, null, true, false)
    );

    @Test
    void addToBase() {
      var newA = RentalRestrictionExtension.NO_RESTRICTION.add(a);
      assertSame(a, newA);
      assertEquals(1, newA.toList().size());
    }

    @Test
    void addToItself() {
      var unchanged = a.add(a);
      assertSame(a, unchanged);
    }

    @Test
    void add() {
      var composite = a.add(b);
      assertInstanceOf(CompositeRentalRestrictionExtension.class, composite);
    }

    @Test
    void differentType() {
      var composite = a.add(c);
      assertInstanceOf(CompositeRentalRestrictionExtension.class, composite);
    }

    @Test
    void composite() {
      var composite = a.add(b);
      assertInstanceOf(CompositeRentalRestrictionExtension.class, composite);
      var newComposite = composite.add(c);
      assertInstanceOf(CompositeRentalRestrictionExtension.class, newComposite);

      var c1 = (CompositeRentalRestrictionExtension) newComposite;
      var exts = c1.toList();
      assertEquals(3, exts.size());

      var c2 = (CompositeRentalRestrictionExtension) c1.add(a);
      assertEquals(3, c2.toList().size());
      // convert to sets so the order doesn't matter
      assertEquals(Set.of(a, b, c), Set.copyOf(c2.toList()));
    }
  }
}
