package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.search.TraverseMode.SCOOTER;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.street.search.state.VehicleRentalState.HAVE_RENTED;
import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.BusinessAreaBorder;
import org.opentripplanner.service.vehiclerental.street.GeofencingZoneExtension;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class StreetEdgeGeofencingTest {

  static String NETWORK_TIER = "tier-oslo";
  static String NETWORK_BIRD = "bird-oslo";
  static RentalRestrictionExtension NO_DROP_OFF_TIER = noDropOffRestriction(NETWORK_TIER);
  static RentalRestrictionExtension NO_TRAVERSAL = new GeofencingZoneExtension(
    new GeofencingZone(new FeedScopedId(NETWORK_TIER, "a-park"), null, false, true)
  );
  StreetVertex V1 = intersectionVertex("V1", 0, 0);
  StreetVertex V2 = intersectionVertex("V2", 1, 1);
  StreetVertex V3 = intersectionVertex("V3", 2, 2);
  StreetVertex V4 = intersectionVertex("V4", 3, 3);

  @Test
  public void addTwoExtensions() {
    var edge = streetEdge(V1, V2);
    edge.addRentalRestriction(new BusinessAreaBorder("a"));
    edge.addRentalRestriction(new BusinessAreaBorder("b"));

    assertTrue(edge.fromv.rentalTraversalBanned(forwardState("a")));
    assertTrue(edge.fromv.rentalTraversalBanned(forwardState("b")));
  }

  @Test
  public void removeExtensions() {
    var edge = streetEdge(V1, V2);
    var a = new BusinessAreaBorder("a");
    var b = new BusinessAreaBorder("b");
    var c = new BusinessAreaBorder("c");

    edge.addRentalRestriction(a);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(forwardState("a")));

    edge.addRentalRestriction(b);
    edge.addRentalRestriction(c);

    edge.removeRentalExtension(a);

    var restrictions = edge.fromv.rentalRestrictions();
    assertTrue(restrictions.traversalBanned(forwardState("b")));
    assertTrue(restrictions.traversalBanned(forwardState("c")));
    assertFalse(restrictions.traversalBanned(forwardState("a")));

    edge.removeRentalExtension(b);

    assertTrue(edge.fromv.rentalRestrictions().traversalBanned(forwardState("c")));
  }

  @Test
  public void checkNetwork() {
    var edge = streetEdge(V1, V2);
    edge.addRentalRestriction(new BusinessAreaBorder("a"));

    var state = traverseFromV1(edge);

    assertEquals(RENTING_FLOATING, state[0].getVehicleRentalState());
    assertEquals(1, state.length);
  }

  @Nested
  class Forward {

    @Test
    public void finishInEdgeWithoutRestrictions() {
      var edge = streetEdge(V1, V2);
      var result = traverseFromV1(edge)[0];
      assertTrue(result.isFinal());
    }

    @Test
    public void leaveBusinessAreaOnFoot() {
      var edge1 = streetEdge(V1, V2);
      var ext = new BusinessAreaBorder(NETWORK_TIER);
      V2.addRentalRestriction(ext);

      var results = traverseFromV1(edge1);

      var onFoot = results[0];
      assertEquals(HAVE_RENTED, onFoot.getVehicleRentalState());
      assertEquals(TraverseMode.WALK, onFoot.getBackMode());
      assertEquals(1, results.length);
    }

    @Test
    public void dontEnterGeofencingZoneOnFoot() {
      var edge = streetEdge(V1, V2);
      V2.addRentalRestriction(
        new GeofencingZoneExtension(
          new GeofencingZone(new FeedScopedId(NETWORK_TIER, "a-park"), null, true, true)
        )
      );
      State result = traverseFromV1(edge)[0];
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
      editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER, NETWORK_TIER, false);
      restrictedEdge.addRentalRestriction(NO_DROP_OFF_TIER);

      var results = edge1.traverse(editor.makeState());

      var continueOnFoot = results[0];
      assertEquals(HAVE_RENTED, continueOnFoot.getVehicleRentalState());
      assertEquals(WALK, continueOnFoot.getBackMode());

      var continueRenting = results[1];
      assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
      assertEquals(SCOOTER, continueRenting.getBackMode());
      assertTrue(continueRenting.isInsideNoRentalDropOffArea());

      var insideZone = restrictedEdge.traverse(continueRenting)[0];

      var leftNoDropOff = edge2.traverse(insideZone)[0];
      assertFalse(leftNoDropOff.isInsideNoRentalDropOffArea());
      assertEquals(RENTING_FLOATING, continueRenting.getVehicleRentalState());
    }

    @Test
    public void forwardDontFinishInNoDropOffZone() {
      var edge = streetEdge(V1, V2);
      V2.addRentalRestriction(NO_DROP_OFF_TIER);
      edge.addRentalRestriction(NO_DROP_OFF_TIER);
      State result = traverseFromV1(edge)[0];
      assertFalse(result.isFinal());
    }
  }

  @Nested
  class Reverse {

    @Test
    public void backwardsRejectWhenEnteringNoDropOffZone() {
      var restrictedEdge = streetEdge(V1, V2);
      V2.addRentalRestriction(NO_DROP_OFF_TIER);

      var req = defaultArriveByRequest();

      var editor = new StateEditor(restrictedEdge.getToVertex(), req);
      editor.dropFloatingVehicle(RentalFormFactor.SCOOTER, NETWORK_TIER, true);

      var s0 = editor.makeState();
      assertEquals(Set.of(NETWORK_TIER), s0.stateData.noRentalDropOffZonesAtStartOfReverseSearch);

      var result = restrictedEdge.traverse(s0);

      assertFalse(State.isEmpty(result));
      assertNotNull(result);
      assertEquals(2, result.length);
    }

    @Test
    public void backwardDontFinishInNoDropOffZone() {
      var edge = streetEdge(V1, V2);
      edge.addRentalRestriction(NO_DROP_OFF_TIER);
      var state = initialState(V2, NETWORK_TIER, true);
      var state2 = edge.traverse(state)[0];
      assertFalse(state2.isFinal());
    }

    @Test
    public void backwardsDontEnterNoTraversalZone() {
      var edge = streetEdge(V1, V2);
      V2.addRentalRestriction(NO_TRAVERSAL);
      var intialState = initialState(V2, NETWORK_TIER, true);
      var result = edge.traverse(intialState);

      assertTrue(State.isEmpty(result));
      assertNotNull(result);
    }

    @Test
    public void pickupFloatingVehicleWhenLeavingAZone() {
      var req = defaultArriveByRequest();

      var haveRentedState = makeHaveRentedState(V2, req);

      var edge = streetEdge(V1, V2);
      V2.addRentalRestriction(NO_TRAVERSAL);
      var states = edge.traverse(haveRentedState);

      // we want to pick up a vehicle
      final State rentalState = states[1];
      assertEquals(RENTING_FLOATING, rentalState.getVehicleRentalState());
      assertEquals(SCOOTER, rentalState.currentMode());

      // but also keep on walking in case we don't find an edge where to leave the vehicle
      var walkingState = states[0];
      assertEquals(HAVE_RENTED, walkingState.getVehicleRentalState());
      assertEquals(WALK, walkingState.currentMode());
    }

    @Test
    public void pickupFloatingVehiclesWhenStartedInNoDropOffZone() {
      var req = defaultArriveByRequest();

      V2.addRentalRestriction(NO_DROP_OFF_TIER);
      V2.addRentalRestriction(noDropOffRestriction(NETWORK_BIRD));

      var haveRentedState = makeHaveRentedState(V2, req);

      var edge = streetEdge(V1, V2);

      var states = edge.traverse(haveRentedState);

      // we return 3 states: one for continuing walking, one for the speculative renting of
      // a vehicle, but with the information of which networks' no-drop-off zones it started in
      assertEquals(3, states.length);

      // first the fallback walk state
      final State walkState = states[0];
      assertEquals(HAVE_RENTED, walkState.getVehicleRentalState());
      assertEquals(WALK, walkState.currentMode());

      // then the speculative renting case for unknown rental network
      final State speculativeRenting = states[2];
      assertEquals(RENTING_FLOATING, speculativeRenting.getVehicleRentalState());
      assertEquals(SCOOTER, speculativeRenting.currentMode());
      // null means that the vehicle has been rented speculatively and the rest of the backwards search
      // needs to check if we really find a vehicle to pick up
      assertNull(speculativeRenting.getVehicleRentalNetwork());
      assertEquals(
        Set.of(NETWORK_TIER, NETWORK_BIRD),
        speculativeRenting.stateData.noRentalDropOffZonesAtStartOfReverseSearch
      );

      // then the speculative renting cases for specific rental networks
      final State tierState = states[1];
      assertEquals(RENTING_FLOATING, tierState.getVehicleRentalState());
      assertEquals(SCOOTER, tierState.currentMode());
      assertEquals(NETWORK_TIER, tierState.getVehicleRentalNetwork());
      assertEquals(Set.of(), tierState.stateData.noRentalDropOffZonesAtStartOfReverseSearch);
    }

    @Test
    public void pickupFloatingVehiclesWhenStartedInNoDropOffZoneAllNetworksAllowedByDefault() {
      var states = runTraverse(Collections.emptySet(), Collections.emptySet());

      // Walking, unknown, Tier, Bird. (Order of last two not guaranteed)
      assertEquals(4, states.length);
      final State walkState = states[0];
      assertEquals(HAVE_RENTED, walkState.getVehicleRentalState());
      assertEquals(WALK, walkState.currentMode());

      final State unknownNetworkState = states[3];
      assertEquals(RENTING_FLOATING, unknownNetworkState.getVehicleRentalState());
      assertEquals(SCOOTER, unknownNetworkState.currentMode());
      assertNull(unknownNetworkState.getVehicleRentalNetwork());

      final State tierState = Arrays.stream(states)
        .filter(s -> NETWORK_TIER.equals(s.getVehicleRentalNetwork()))
        .findFirst()
        .get();
      assertEquals(RENTING_FLOATING, tierState.getVehicleRentalState());
      assertEquals(SCOOTER, tierState.currentMode());

      final State birdState = Arrays.stream(states)
        .filter(s -> NETWORK_BIRD.equals(s.getVehicleRentalNetwork()))
        .findFirst()
        .get();
      assertEquals(RENTING_FLOATING, birdState.getVehicleRentalState());
      assertEquals(SCOOTER, birdState.currentMode());
    }

    @Test
    public void pickupFloatingVehiclesWhenStartedInNoDropOffZoneAllNetworksAllowed() {
      var states = runTraverse(Set.of(NETWORK_TIER, NETWORK_BIRD), Collections.emptySet());

      // Walking, unknown, Tier, Bird. (Order of last two not guaranteed)
      assertEquals(4, states.length);
      final State walkState = states[0];
      assertEquals(HAVE_RENTED, walkState.getVehicleRentalState());
      assertEquals(WALK, walkState.currentMode());

      final State unknownNetworkState = states[3];
      assertEquals(RENTING_FLOATING, unknownNetworkState.getVehicleRentalState());
      assertEquals(SCOOTER, unknownNetworkState.currentMode());
      assertNull(unknownNetworkState.getVehicleRentalNetwork());

      final State tierState = Arrays.stream(states)
        .filter(s -> NETWORK_TIER.equals(s.getVehicleRentalNetwork()))
        .findFirst()
        .get();
      assertEquals(RENTING_FLOATING, tierState.getVehicleRentalState());
      assertEquals(SCOOTER, tierState.currentMode());

      final State birdState = Arrays.stream(states)
        .filter(s -> NETWORK_BIRD.equals(s.getVehicleRentalNetwork()))
        .findFirst()
        .get();
      assertEquals(RENTING_FLOATING, birdState.getVehicleRentalState());
      assertEquals(SCOOTER, birdState.currentMode());
    }

    @Test
    public void pickupFloatingVehiclesWhenStartedInNoDropOffZoneSomeNetworksAllowed() {
      var states = runTraverse(Set.of(NETWORK_TIER), Collections.emptySet());

      // Walking, unknown, Tier.
      assertEquals(3, states.length);
      final State walkState = states[0];
      assertEquals(HAVE_RENTED, walkState.getVehicleRentalState());
      assertEquals(WALK, walkState.currentMode());

      final State unknownNetworkState = states[2];
      assertEquals(RENTING_FLOATING, unknownNetworkState.getVehicleRentalState());
      assertEquals(SCOOTER, unknownNetworkState.currentMode());
      assertNull(unknownNetworkState.getVehicleRentalNetwork());

      final State tierState = states[1];
      assertEquals(RENTING_FLOATING, tierState.getVehicleRentalState());
      assertEquals(SCOOTER, tierState.currentMode());
      assertEquals(NETWORK_TIER, tierState.getVehicleRentalNetwork());
    }

    @Test
    public void pickupFloatingVehiclesWhenStartedInNoDropOffZoneAllNetworksBanned() {
      var states = runTraverse(Collections.emptySet(), Set.of(NETWORK_TIER, NETWORK_BIRD));

      // Should only have a walking state. The unknown network state should only be
      // generated if there are known network states.
      assertEquals(1, states.length);
      final State walkState = states[0];
      assertEquals(HAVE_RENTED, walkState.getVehicleRentalState());
      assertEquals(WALK, walkState.currentMode());
    }

    @Test
    public void pickupFloatingVehiclesWhenStartedInNoDropOffZoneSomeNetworksBanned() {
      var states = runTraverse(Collections.emptySet(), Set.of(NETWORK_BIRD));

      // Walking state, unknown network state, known network state for Tier (which wasn't banned)
      assertEquals(3, states.length);

      final State walkState = states[0];
      assertEquals(HAVE_RENTED, walkState.getVehicleRentalState());
      assertEquals(WALK, walkState.currentMode());

      final State unknownNetworkState = states[2];
      assertEquals(RENTING_FLOATING, unknownNetworkState.getVehicleRentalState());
      assertEquals(SCOOTER, unknownNetworkState.currentMode());
      assertNull(unknownNetworkState.getVehicleRentalNetwork());

      final State tierState = states[1];
      assertEquals(RENTING_FLOATING, tierState.getVehicleRentalState());
      assertEquals(SCOOTER, tierState.currentMode());
      assertEquals(NETWORK_TIER, tierState.getVehicleRentalNetwork());
    }

    private State[] runTraverse(Set<String> allowedNetworks, Set<String> bannedNetworks) {
      var req = makeArriveByRequest(allowedNetworks, bannedNetworks);

      V2.addRentalRestriction(NO_DROP_OFF_TIER);
      V2.addRentalRestriction(noDropOffRestriction(NETWORK_BIRD));

      var haveRentedState = makeHaveRentedState(V2, req);

      var edge = streetEdge(V1, V2);

      return edge.traverse(haveRentedState);
    }

    private static State makeHaveRentedState(Vertex vertex, StreetSearchRequest req) {
      // this is the state that starts inside a restricted zone
      // (no drop off, no traversal or outside business area)
      // and is walking towards finding a rental vehicle
      return State.getInitialStates(Set.of(vertex), req)
        .stream()
        .filter(s -> s.getVehicleRentalState() == HAVE_RENTED)
        .findAny()
        .get();
    }

    private static StreetSearchRequest defaultArriveByRequest() {
      return StreetSearchRequest.of()
        .withPreferences(p ->
          p.withScooter(b -> b.withRental(r -> r.withAllowedNetworks(Set.of(NETWORK_TIER))))
        )
        .withMode(StreetMode.SCOOTER_RENTAL)
        .withArriveBy(true)
        .build();
    }

    private static StreetSearchRequest makeArriveByRequest(
      Set<String> allowedNetworks,
      Set<String> bannedNetworks
    ) {
      return StreetSearchRequest.of()
        .withPreferences(p ->
          p.withScooter(b ->
            b.withRental(r ->
              r.withAllowedNetworks(allowedNetworks).withBannedNetworks(bannedNetworks)
            )
          )
        )
        .withMode(StreetMode.SCOOTER_RENTAL)
        .withArriveBy(true)
        .build();
    }
  }

  private static GeofencingZoneExtension noDropOffRestriction(String networkTier) {
    return new GeofencingZoneExtension(
      new GeofencingZone(new FeedScopedId(networkTier, "a-park"), null, true, false)
    );
  }

  private State[] traverseFromV1(StreetEdge edge) {
    var state = initialState(V1, NETWORK_TIER, false);
    return edge.traverse(state);
  }

  private State forwardState(String network) {
    return initialState(V1, network, false);
  }

  private State initialState(Vertex startVertex, String network, boolean arriveBy) {
    var req = StreetSearchRequest.of()
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withArriveBy(arriveBy)
      .build();
    var editor = new StateEditor(startVertex, req);
    editor.beginFloatingVehicleRenting(RentalFormFactor.SCOOTER, network, false);
    return editor.makeState();
  }
}
