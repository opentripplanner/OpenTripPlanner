package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingBoundaryExtension;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * Integration test for geofencing zone enforcement with floating scooter rentals.
 *
 * <pre>
 * Graph layout:
 *   T_origin --- A --- B --- C --- D --- E --- T_dest
 *                      |         |
 *                   scooter   zone boundary (between C and D)
 *
 * C is outside the zone, D is inside. Scooter at B.
 * Expected: pick up at B, ride to C, drop off at C, walk to destination.
 * </pre>
 */
public class ScooterRentalGeofencingTest extends GraphRoutingTest {

  static final String NETWORK = "scooter-network";
  static final GeofencingZone NO_DROP_OFF_ZONE = TestGeofencingZoneBuilder.of(
    NETWORK,
    "no-dropoff-zone"
  )
    .noDropOff()
    .build();

  private StreetVertex A, B, C, D, E;
  private TemporaryStreetLocation T_ORIGIN, T_DEST;
  private VehicleRentalPlaceVertex SCOOTER_VERTEX;

  @BeforeEach
  public void setUp() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.913, 10.740);
          E = intersection("E", 59.914, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9145, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter1", 59.911, 10.7401);

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 200, perm);
          street(B, C, 2000, perm);
          street(C, D, 200, perm);
          street(D, E, 200, perm);

          biLink(B, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          C.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));
          D.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, false));
        }
      }
    );
  }

  @Test
  public void forwardSearchDropsOffOutsideNoDropOffZone() {
    var descriptor = runSearch(T_ORIGIN, T_DEST, false);
    assertNotNull(descriptor, "forward search should find a path");
    assertNoRentingInsideZone(descriptor, "Forward");
  }

  @Test
  public void arriveBySearchDropsOffOutsideNoDropOffZone() {
    var descriptor = runSearch(T_ORIGIN, T_DEST, true);
    assertNotNull(descriptor, "arriveBy search should find a path");
    assertNoRentingInsideZone(descriptor, "ArriveBy");
  }

  @Test
  public void forwardAndArriveByBothFindPath() {
    var forward = runSearch(T_ORIGIN, T_DEST, false);
    var arriveBy = runSearch(T_ORIGIN, T_DEST, true);

    assertNotNull(forward, "forward should find a path");
    assertNotNull(arriveBy, "arriveBy should find a path");

    // Neither path should have RENTING_FLOATING inside the zone
    assertNoRentingInsideZone(forward, "Forward");
    assertNoRentingInsideZone(arriveBy, "ArriveBy");
  }

  @Test
  public void forwardSearchWithScooterInsideZone() {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.913, 10.740);
          E = intersection("E", 59.914, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9145, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter-inside", 59.913, 10.7401);
          SCOOTER_VERTEX.setInitialGeofencingZones(Set.of(NO_DROP_OFF_ZONE));

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 100, perm);
          street(B, C, 100, perm);
          street(C, D, 100, perm);
          street(D, E, 100, perm);

          biLink(D, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          C.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, true));
          D.addGeofencingBoundary(new GeofencingBoundaryExtension(NO_DROP_OFF_ZONE, false));
        }
      }
    );

    var forward = runSearch(T_ORIGIN, T_DEST, false);
    assertNotNull(forward, "forward should find a path");
    assertNoRentingInsideZone(forward, "Forward (scooter inside zone)");
  }

  @Test
  public void forwardSearchBlocksRidingIntoNoTraversalZone() {
    var noTraversalZone = TestGeofencingZoneBuilder.of(NETWORK, "no-traversal-zone")
      .noTraversal()
      .build();

    setupNoTraversalGraph(noTraversalZone);

    var forward = runSearch(T_ORIGIN, T_DEST, false);
    assertNotNull(forward, "forward should find a path");
    assertNoRentingInsideZone(forward, "Forward (no-traversal)");
  }

  @Test
  public void arriveBySearchBlocksRidingIntoNoTraversalZone() {
    var noTraversalZone = TestGeofencingZoneBuilder.of(NETWORK, "no-traversal-zone")
      .noTraversal()
      .build();

    setupNoTraversalGraph(noTraversalZone);

    var builder = StreetSearchRequest.of()
      .withArriveBy(true)
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withScooter(s ->
        s.withRental(r ->
          r
            .withPickupTime(Duration.ofSeconds(30))
            .withPickupCost(Cost.costOfSeconds(30))
            .withDropOffTime(Duration.ofSeconds(15))
            .withDropOffCost(Cost.costOfSeconds(15))
        )
      )
      .withArriveByDestinationZones(Set.of(noTraversalZone));

    var request = builder.build();

    var tree = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withFrom(T_ORIGIN)
      .withTo(T_DEST)
      .getShortestPathTree();

    var path = tree.getPath(T_ORIGIN);
    assertNotNull(path, "arriveBy should find a path");

    var descriptor = path.states
      .stream()
      .filter(s -> s.getBackEdge() != null)
      .map(s -> formatState(s))
      .collect(Collectors.toList());

    assertNoRentingInsideZone(descriptor, "ArriveBy (no-traversal)");
    assertTrue(
      descriptor.stream().anyMatch(d -> d.contains("RENTING_FLOATING")),
      "ArriveBy should use a scooter"
    );
  }

  @Test
  public void forwardSearchDoesNotDropOffBetweenAdjacentNoDropOffZones() {
    var zoneA = TestGeofencingZoneBuilder.of(NETWORK, "zone-a").noDropOff().build();
    var zoneB = TestGeofencingZoneBuilder.of(NETWORK, "zone-b").noDropOff().build();

    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.913, 10.740);
          E = intersection("E", 59.914, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9145, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter1", 59.911, 10.7401);

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 100, perm);
          street(B, C, 100, perm);
          street(C, D, 100, perm);
          street(D, E, 100, perm);

          biLink(B, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          B.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneA, true));
          C.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneA, false));
          D.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneB, true));
          E.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneB, false));
        }
      }
    );

    var forward = runSearch(T_ORIGIN, T_DEST, false);
    assertNotNull(forward, "forward should find a path");
    assertNoRentingInsideZone(forward, "Forward (adjacent zones)");
  }

  @Test
  public void forwardSearchRidesAroundNoTraversalZoneInsteadOfWalkingThrough() {
    var noTraversalZone = TestGeofencingZoneBuilder.of(NETWORK, "no-traversal-zone")
      .noTraversal()
      .build();

    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.912, 10.770);
          E = intersection("E", 59.913, 10.740);
          var F = intersection("F", 59.9125, 10.740);
          var G = intersection("G", 59.9128, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9135, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter1", 59.911, 10.7401);

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 100, perm);
          street(B, C, 100, perm);
          street(C, D, 1500, perm);
          street(D, E, 1500, perm);
          street(C, F, 800, perm);
          street(F, G, 400, perm);
          street(G, E, 800, perm);

          biLink(B, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          C.addGeofencingBoundary(new GeofencingBoundaryExtension(noTraversalZone, true));
          F.addGeofencingBoundary(new GeofencingBoundaryExtension(noTraversalZone, false));
        }
      }
    );

    var forward = runSearch(T_ORIGIN, T_DEST, false);
    assertNotNull(forward, "should find a path");
    var allStates = String.join("\n  ", forward);

    boolean ridesAroundZone = forward
      .stream()
      .anyMatch(d -> d.contains("CD street") && d.contains("RENTING_FLOATING"));
    assertTrue(
      ridesAroundZone,
      "Rider should ride around the zone (C->D->E), not walk through it.\nPath:\n  " + allStates
    );
  }

  /**
   * ArriveBy with adjacent no-drop-off zones (same network). Destination inside zone A,
   * zone B is adjacent. Drop-off must happen outside BOTH zones.
   *
   * <pre>
   *   T_origin -- A -- B(scooter) -- C -- D -- E -- T_dest
   *                                  ^    ^
   *                                zone B  zone A
   *                               boundary boundary
   *
   * Zone A: D, E inside (boundary C/D: C entering=true, D entering=false)
   * Zone B: C, D inside (boundary B/C: B entering=true, C entering=false)
   * D is inside both zones.
   * </pre>
   */
  @Test
  public void arriveByAdjacentNoDropOffZonesDropsOutsideBothZones() {
    var zoneA = TestGeofencingZoneBuilder.of(NETWORK, "zone-a").noDropOff().build();
    var zoneB = TestGeofencingZoneBuilder.of(NETWORK, "zone-b").noDropOff().build();

    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.913, 10.740);
          E = intersection("E", 59.914, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9145, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter1", 59.911, 10.7401);

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 200, perm);
          street(B, C, 2000, perm);
          street(C, D, 500, perm);
          street(D, E, 500, perm);

          biLink(B, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          // Zone A boundary: C outside (entering=true), D inside (entering=false)
          C.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneA, true));
          D.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneA, false));
          // Zone B boundary: B outside (entering=true), C inside (entering=false)
          B.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneB, true));
          C.addGeofencingBoundary(new GeofencingBoundaryExtension(zoneB, false));
        }
      }
    );

    // ArriveBy: destination inside zone A only (not zone B)
    var builder = StreetSearchRequest.of()
      .withArriveBy(true)
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withScooter(s ->
        s.withRental(r ->
          r
            .withPickupTime(Duration.ofSeconds(30))
            .withPickupCost(Cost.costOfSeconds(30))
            .withDropOffTime(Duration.ofSeconds(15))
            .withDropOffCost(Cost.costOfSeconds(15))
        )
      )
      .withArriveByDestinationZones(Set.of(zoneA));

    var request = builder.build();

    var tree = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withFrom(T_ORIGIN)
      .withTo(T_DEST)
      .getShortestPathTree();

    var path = tree.getPath(T_ORIGIN);
    assertNotNull(path, "arriveBy should find a path");

    var descriptor = path.states
      .stream()
      .filter(s -> s.getBackEdge() != null)
      .map(this::formatState)
      .collect(Collectors.toList());

    var allStates = String.join("\n  ", descriptor);

    // The rider should NOT be RENTING_FLOATING on CD or DE edges (inside zones)
    for (var d : descriptor) {
      assertFalse(
        (d.contains("CD street") || d.contains("DE street")) && d.contains("RENTING_FLOATING"),
        "ArriveBy: rider should not be renting inside adjacent zones.\nPath:\n  " + allStates
      );
    }

    // The rider should use a scooter somewhere in the path
    assertTrue(
      descriptor.stream().anyMatch(d -> d.contains("RENTING_FLOATING")),
      "ArriveBy should use a scooter.\nPath:\n  " + allStates
    );
  }

  private void setupNoTraversalGraph(GeofencingZone noTraversalZone) {
    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.913, 10.740);
          E = intersection("E", 59.914, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9145, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter1", 59.911, 10.7401);

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 500, perm);
          street(B, C, 1000, perm);
          street(C, D, 1000, perm);
          street(D, E, 500, perm);

          biLink(B, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          C.addGeofencingBoundary(new GeofencingBoundaryExtension(noTraversalZone, true));
          D.addGeofencingBoundary(new GeofencingBoundaryExtension(noTraversalZone, false));
        }
      }
    );
  }

  /**
   * Business area: origin and scooter inside BA, destination outside.
   * Forward: rider should drop off at BA boundary (C), walk C→D→E→dest.
   * Rider must NOT be RENTING_FLOATING on DE or E→dest edges (outside BA).
   *
   * <pre>
   *   T_origin --- A --- B(scooter) --- C --- D --- E --- T_dest
   *                                     ^    ^
   *                                  BA exit BA enter
   *                               (inside)  (outside)
   * </pre>
   */
  @Test
  public void forwardSearchDropsOffInsideBusinessArea() {
    var businessArea = TestGeofencingZoneBuilder.of(NETWORK, "business-area")
      .asBusinessArea()
      .build();

    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.913, 10.740);
          E = intersection("E", 59.914, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9145, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter1", 59.911, 10.7401);

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 200, perm);
          street(B, C, 200, perm);
          street(C, D, 200, perm);
          street(D, E, 200, perm);

          biLink(B, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          // C is last vertex inside BA (entering=false), D is first outside (entering=true)
          C.addGeofencingBoundary(new GeofencingBoundaryExtension(businessArea, false));
          D.addGeofencingBoundary(new GeofencingBoundaryExtension(businessArea, true));
        }
      }
    );

    var forward = runSearch(T_ORIGIN, T_DEST, false);
    assertNotNull(forward, "forward should find a path");
    assertNoRentingOutsideBA(forward, "Forward BA");
  }

  /**
   * Business area arrive-by: destination outside BA. The search should find a path that
   * drops off inside the BA and walks to destination — NOT ride to destination and drop outside.
   */
  @Test
  public void arriveBySearchDropsOffInsideBusinessArea() {
    var businessArea = TestGeofencingZoneBuilder.of(NETWORK, "business-area")
      .asBusinessArea()
      .build();

    modelOf(
      new Builder() {
        @Override
        public void build() {
          A = intersection("A", 59.910, 10.740);
          B = intersection("B", 59.911, 10.740);
          C = intersection("C", 59.912, 10.740);
          D = intersection("D", 59.913, 10.740);
          E = intersection("E", 59.914, 10.740);

          T_ORIGIN = streetLocation("origin", 59.9095, 10.740);
          T_DEST = streetLocation("dest", 59.9145, 10.740);

          SCOOTER_VERTEX = createFloatingScooter("scooter1", 59.911, 10.7401);

          var perm = StreetTraversalPermission.ALL;
          street(A, B, 200, perm);
          street(B, C, 200, perm);
          street(C, D, 200, perm);
          street(D, E, 200, perm);

          biLink(B, SCOOTER_VERTEX);
          link(T_ORIGIN, A);
          link(E, T_DEST);

          C.addGeofencingBoundary(new GeofencingBoundaryExtension(businessArea, false));
          D.addGeofencingBoundary(new GeofencingBoundaryExtension(businessArea, true));
        }
      }
    );

    // Destination is outside BA — arriveByDestinationZones must be empty (not the
    // default NO_DROP_OFF_ZONE from runSearch). Use a custom search.
    var request = StreetSearchRequest.of()
      .withArriveBy(true)
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withScooter(s ->
        s.withRental(r ->
          r
            .withPickupTime(Duration.ofSeconds(30))
            .withPickupCost(Cost.costOfSeconds(30))
            .withDropOffTime(Duration.ofSeconds(15))
            .withDropOffCost(Cost.costOfSeconds(15))
        )
      )
      .build();

    var tree = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withFrom(T_ORIGIN)
      .withTo(T_DEST)
      .getShortestPathTree();

    var path = tree.getPath(T_ORIGIN);
    assertNotNull(path, "arriveBy should find a path");

    var arriveBy = path.states
      .stream()
      .filter(s -> s.getBackEdge() != null)
      .map(this::formatState)
      .collect(Collectors.toList());

    assertNoRentingOutsideBA(arriveBy, "ArriveBy BA");
  }

  private void assertNoRentingOutsideBA(List<String> descriptor, String label) {
    var allStates = String.join("\n  ", descriptor);
    for (var d : descriptor) {
      assertFalse(
        (d.contains("DE street") || d.contains("E ")) && d.contains("RENTING_FLOATING"),
        label + ": rider should not be renting outside BA (DE/E→dest edges).\nPath:\n  " + allStates
      );
    }
    assertTrue(
      descriptor.stream().anyMatch(d -> d.contains("RENTING_FLOATING")),
      label + " should use a scooter.\nPath:\n  " + allStates
    );
  }

  private void assertNoRentingInsideZone(List<String> descriptor, String label) {
    var allStates = String.join("\n  ", descriptor);
    for (var d : descriptor) {
      assertFalse(
        (d.contains("CD street") || d.contains("DE street")) && d.contains("RENTING_FLOATING"),
        label + ": rider should not be renting inside zone (CD/DE edges).\nPath:\n  " + allStates
      );
    }
  }

  private List<String> runSearch(Vertex from, Vertex to, boolean arriveBy) {
    var builder = StreetSearchRequest.of()
      .withArriveBy(arriveBy)
      .withMode(StreetMode.SCOOTER_RENTAL)
      .withScooter(s ->
        s.withRental(r ->
          r
            .withPickupTime(Duration.ofSeconds(30))
            .withPickupCost(Cost.costOfSeconds(30))
            .withDropOffTime(Duration.ofSeconds(15))
            .withDropOffCost(Cost.costOfSeconds(15))
        )
      );

    if (arriveBy) {
      builder.withArriveByDestinationZones(Set.of(NO_DROP_OFF_ZONE));
    }

    var request = builder.build();

    var tree = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withFrom(from)
      .withTo(to)
      .getShortestPathTree();

    var path = tree.getPath(arriveBy ? from : to);
    if (path == null) {
      return null;
    }

    return path.states
      .stream()
      .filter(s -> s.getBackEdge() != null)
      .map(this::formatState)
      .collect(Collectors.toList());
  }

  private String formatState(org.opentripplanner.street.search.state.State s) {
    return String.format(
      Locale.ROOT,
      "%s - %s - %s (%,.2f, %d) zones=%s",
      s.getBackMode(),
      s.getVehicleRentalState(),
      s.getBackEdge().getDefaultName(),
      s.getWeight(),
      s.getElapsedTimeSeconds(),
      s.getCurrentGeofencingZones()
    );
  }

  private VehicleRentalPlaceVertex createFloatingScooter(String id, double lat, double lon) {
    var vehicle = VehicleRentalVehicle.of()
      .withId(new FeedScopedId(NETWORK, id))
      .withName(new NonLocalizedString(id))
      .withLatitude(lat)
      .withLongitude(lon)
      .withVehicleType(
        RentalVehicleType.of()
          .withId(new FeedScopedId(NETWORK, "scooter-type"))
          .withFormFactor(RentalFormFactor.SCOOTER)
          .withPropulsionType(RentalVehicleType.PropulsionType.ELECTRIC)
          .withMaxRangeMeters(50000d)
          .build()
      )
      .build();

    var vertex = new VehicleRentalPlaceVertex(vehicle);
    VehicleRentalEdge.createVehicleRentalEdge(vertex, RentalFormFactor.SCOOTER);
    return vertex;
  }
}
