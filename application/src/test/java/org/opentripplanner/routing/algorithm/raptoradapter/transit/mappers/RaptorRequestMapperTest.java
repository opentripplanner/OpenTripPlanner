package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapperTest.RequestFeature.RELAX_COST_DEST;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapperTest.RequestFeature.TRANSIT_GROUP_PRIORITY;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapperTest.RequestFeature.VIA_PASS_THROUGH;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapperTest.RequestFeature.VIA_VISIT;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.model.ViaCoordinateTransfer;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.ListUtils;

class RaptorRequestMapperTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final StopLocation STOP_A = TEST_MODEL.stop("Stop:A").build();
  public static final PassThroughViaLocation PASS_THROUGH_VIA_LOCATION = new PassThroughViaLocation(
    "Via A",
    List.of(STOP_A.getId())
  );
  public static final VisitViaLocation VISIT_VIA_LOCATION = new VisitViaLocation(
    "Via A",
    null,
    List.of(STOP_A.getId()),
    List.of()
  );
  private static final int VIA_FROM_STOP_INDEX = 47;
  private static final int VIA_TO_STOP_INDEX = 123;
  private static final List<RaptorAccessEgress> ACCESS = List.of(TestAccessEgress.walk(12, 45));
  private static final List<RaptorAccessEgress> EGRESS = List.of(TestAccessEgress.walk(144, 54));
  private static final WgsCoordinate VIA_COORDINATE = WgsCoordinate.GREENWICH;
  public static final ViaCoordinateTransfer VIA_TRANSFER = new ViaCoordinateTransfer(
    VIA_COORDINATE,
    VIA_FROM_STOP_INDEX,
    VIA_TO_STOP_INDEX,
    List.of(),
    List.of(),
    10,
    12.0
  );

  private static final CostLinearFunction R1 = CostLinearFunction.of("50 + 1.0x");
  private static final CostLinearFunction R2 = CostLinearFunction.of("0 + 1.5x");
  private static final CostLinearFunction R3 = CostLinearFunction.of("30 + 2.0x");

  private static final Map<FeedScopedId, StopLocation> STOPS_MAP = Map.of(STOP_A.getId(), STOP_A);
  private static final CostLinearFunction RELAX_TRANSIT_GROUP_PRIORITY = CostLinearFunction.of(
    "30m + 1.2t"
  );
  public static final double RELAX_GENERALIZED_COST_AT_DESTINATION = 2.0;

  static List<Arguments> testCasesRelaxedCost() {
    return List.of(
      Arguments.of(CostLinearFunction.NORMAL, 0, 0),
      Arguments.of(CostLinearFunction.NORMAL, 10, 10),
      Arguments.of(R1, 0, 5000),
      Arguments.of(R1, 7, 5007),
      Arguments.of(R2, 0, 0),
      Arguments.of(R2, 100, 150),
      Arguments.of(R3, 0, 3000),
      Arguments.of(R3, 100, 3200)
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesRelaxedCost")
  void mapRelaxCost(CostLinearFunction input, int cost, int expected) {
    var calcCost = RaptorRequestMapper.mapRelaxCost(input);
    assertEquals(expected, calcCost.relax(cost));
  }

  @Test
  void testViaLocation() {
    var req = new RouteRequest();
    var minWaitTime = Duration.ofMinutes(13);

    req.setViaLocations(
      List.of(new VisitViaLocation("Via A", minWaitTime, List.of(STOP_A.getId()), List.of()))
    );

    var result = map(req);

    assertTrue(result.searchParams().isVisitViaSearch());
    assertEquals(
      "[RaptorViaLocation{via Via A wait 13m : [(stop 0, 13m)]}]",
      result.searchParams().viaLocations().toString()
    );
  }

  @Test
  void testPassThroughPoints() {
    var req = new RouteRequest();

    req.setViaLocations(List.of(new PassThroughViaLocation("Via A", List.of(STOP_A.getId()))));

    var result = map(req);

    assertTrue(result.searchParams().isPassThroughSearch());
    assertEquals(
      "[RaptorViaLocation{pass-through Via A : [(stop " + STOP_A.getIndex() + ")]}]",
      result.searchParams().viaLocations().toString()
    );
  }

  @Test
  void testViaCoordinate() {
    var req = new RouteRequest();
    Duration minimumWaitTime = Duration.ofMinutes(10);

    req.setViaLocations(
      List.of(
        new VisitViaLocation("Via coordinate", minimumWaitTime, List.of(), List.of(VIA_COORDINATE))
      )
    );

    var result = map(req);

    assertFalse(result.searchParams().viaLocations().isEmpty());
    assertEquals(
      "[RaptorViaLocation{via Via coordinate wait 10m : [(stop 47 ~ 123, 10m10s)]}]",
      result.searchParams().viaLocations().toString()
    );
  }

  @Test
  void testTransitGroupPriority() {
    var req = new RouteRequest();

    // Set relax transit-group-priority
    req.withPreferences(p ->
      p.withTransit(t -> t.withRelaxTransitGroupPriority(CostLinearFunction.of("30m + 1.2t")))
    );

    var result = map(req);

    assertTrue(result.multiCriteria().transitPriorityCalculator().isPresent());
  }

  static List<Arguments> testViaAndTransitGroupPriorityCombinationsTestCases() {
    return List.of(
      // If ONE feature is requested, the same feature is expected
      Arguments.of(
        "VIA_PASS_THROUGH only",
        List.of(VIA_PASS_THROUGH),
        List.of(VIA_PASS_THROUGH),
        null
      ),
      Arguments.of("VIA_VISIT only", List.of(VIA_VISIT), List.of(VIA_VISIT), null),
      Arguments.of(
        "TRANSIT_GROUP_PRIORITY only",
        List.of(TRANSIT_GROUP_PRIORITY),
        List.of(TRANSIT_GROUP_PRIORITY),
        null
      ),
      Arguments.of(
        "RELAX_COST_DEST only",
        List.of(RELAX_COST_DEST),
        List.of(RELAX_COST_DEST),
        null
      ),
      Arguments.of(
        "VIA_VISIT is not allowed together VIA_PASS_THROUGH, an error is expected.",
        List.of(VIA_VISIT, VIA_PASS_THROUGH),
        List.of(),
        "A mix of via-locations and pass-through is not allowed in this version."
      ),
      Arguments.of(
        """
        VIA_VISIT is not allowed together VIA_PASS_THROUGH, an error is expected.
        Other features are ignored.
        """,
        List.of(VIA_VISIT, VIA_PASS_THROUGH, TRANSIT_GROUP_PRIORITY, RELAX_COST_DEST),
        List.of(),
        "A mix of via-locations and pass-through is not allowed in this version."
      ),
      Arguments.of(
        "VIA_PASS_THROUGH cannot be combined with other features, and other features are dropped",
        List.of(VIA_PASS_THROUGH, TRANSIT_GROUP_PRIORITY, RELAX_COST_DEST),
        List.of(VIA_PASS_THROUGH),
        null
      ),
      Arguments.of(
        "VIA_VISIT can be combined with TRANSIT_GROUP_PRIORITY",
        List.of(VIA_VISIT, TRANSIT_GROUP_PRIORITY),
        List.of(VIA_VISIT, TRANSIT_GROUP_PRIORITY),
        null
      ),
      Arguments.of(
        """
        VIA_VISIT can only be combined with TRANSIT_GROUP_PRIORITY, and other features are dropped
        VIA_PASS_THROUGH override VIA_VISIT (see above)
        """,
        List.of(VIA_VISIT, TRANSIT_GROUP_PRIORITY, RELAX_COST_DEST),
        List.of(VIA_VISIT, TRANSIT_GROUP_PRIORITY),
        null
      ),
      Arguments.of(
        """
        TRANSIT_GROUP_PRIORITY cannot be combined with other features, override RELAX_COST_DEST
        VIA_PASS_THROUGH and VIA_VISIT override VIA_VISIT (see above)
        """,
        List.of(TRANSIT_GROUP_PRIORITY, RELAX_COST_DEST),
        List.of(TRANSIT_GROUP_PRIORITY),
        null
      )
    );
  }

  @ParameterizedTest(name = "{0}.  {1}  =>  {2}")
  @MethodSource("testViaAndTransitGroupPriorityCombinationsTestCases")
  void testViaAndTransitGroupPriorityCombinations(
    String ignore,
    List<RequestFeature> requestedFeatures,
    List<RequestFeature> expectedFeatures,
    @Nullable String errorMessage
  ) {
    var req = new RouteRequest();

    for (RequestFeature it : requestedFeatures) {
      req = setFeaturesOnRequest(req, it);
    }

    if (errorMessage == null) {
      var result = map(req);

      for (var feature : RequestFeature.values()) {
        assertFeatureSet(feature, result, expectedFeatures.contains(feature));
      }
    } else {
      var r = req;
      var ex = Assertions.assertThrows(IllegalArgumentException.class, () -> map(r));
      assertEquals(errorMessage, ex.getMessage());
    }
  }

  private static RaptorRequest<TestTripSchedule> map(RouteRequest request) {
    return RaptorRequestMapper.mapRequest(
      request,
      ZonedDateTime.now(),
      false,
      ACCESS,
      EGRESS,
      null,
      new DummyViaCoordinateTransferFactory(),
      id -> IntStream.of(STOPS_MAP.get(id).getIndex())
    );
  }

  private static void assertFeatureSet(
    RequestFeature feature,
    RaptorRequest<?> result,
    boolean expected
  ) {
    switch (feature) {
      case VIA_VISIT:
        if (expected) {
          assertTrue(result.searchParams().isVisitViaSearch());
          // One via location exist(no NPE), but it does not allow pass-through
          assertEquals(
            "RaptorViaLocation{via Via A : [(stop 0)]}",
            result.searchParams().viaLocations().get(0).toString()
          );
        }
        break;
      case VIA_PASS_THROUGH:
        if (expected) {
          assertTrue(result.searchParams().isPassThroughSearch());
          assertEquals(
            "RaptorViaLocation{pass-through Via A : [(stop 0)]}",
            result.searchParams().viaLocations().get(0).toString()
          );
        }
        break;
      case TRANSIT_GROUP_PRIORITY:
        assertEquals(expected, result.multiCriteria().transitPriorityCalculator().isPresent());
        if (expected) {
          assertFalse(result.searchParams().isPassThroughSearch());
        }
        break;
      case RELAX_COST_DEST:
        assertEquals(expected, result.multiCriteria().relaxCostAtDestination() != null);
        if (expected) {
          assertFalse(result.searchParams().isPassThroughSearch());
          assertFalse(result.searchParams().isVisitViaSearch());
        }
        break;
    }
  }

  private static RouteRequest setFeaturesOnRequest(RouteRequest req, RequestFeature feature) {
    return switch (feature) {
      case VIA_VISIT -> req.setViaLocations(
        ListUtils.combine(req.getViaLocations(), List.of(VISIT_VIA_LOCATION))
      );
      case VIA_PASS_THROUGH -> req.setViaLocations(
        ListUtils.combine(req.getViaLocations(), List.of(PASS_THROUGH_VIA_LOCATION))
      );
      case TRANSIT_GROUP_PRIORITY -> req.withPreferences(p ->
        p.withTransit(t -> t.withRelaxTransitGroupPriority(RELAX_TRANSIT_GROUP_PRIORITY))
      );
      case RELAX_COST_DEST -> req.withPreferences(p ->
        p.withTransit(t ->
          t.withRaptor(r ->
            r.withRelaxGeneralizedCostAtDestination(RELAX_GENERALIZED_COST_AT_DESTINATION)
          )
        )
      );
    };
  }

  enum RequestFeature {
    VIA_VISIT,
    VIA_PASS_THROUGH,
    TRANSIT_GROUP_PRIORITY,
    RELAX_COST_DEST,
  }

  private static class DummyViaCoordinateTransferFactory implements ViaCoordinateTransferFactory {

    @Override
    public List<ViaCoordinateTransfer> createViaTransfers(
      RouteRequest request,
      String ignore,
      WgsCoordinate coordinate
    ) {
      // Make sure the input is the expected via-coordinate
      assertEquals(VIA_COORDINATE, coordinate);
      return List.of(VIA_TRANSFER);
    }
  }
}
