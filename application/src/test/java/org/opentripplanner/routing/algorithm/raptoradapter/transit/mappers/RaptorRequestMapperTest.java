package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapperTest.RequestFeature.TRANSIT_GROUP_PRIORITY;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapperTest.RequestFeature.VIA_PASS_THROUGH;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapperTest.RequestFeature.VIA_VISIT;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptorlegacy._data.transit.TestAccessEgress;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.model.ViaCoordinateTransfer;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.ListUtils;

class RaptorRequestMapperTest {

  private static final GenericLocation TO = GenericLocation.fromCoordinate(60.0, 10.0);
  private static final GenericLocation FROM = GenericLocation.fromCoordinate(62.0, 12.0);
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final StopLocation STOP_A = TEST_MODEL.stop("Stop:A").build();
  private static final PassThroughViaLocation PASS_THROUGH_VIA_LOCATION =
    new PassThroughViaLocation("Via A", List.of(STOP_A.getId()));
  private static final VisitViaLocation VISIT_VIA_LOCATION = new VisitViaLocation(
    "Via A",
    null,
    List.of(STOP_A.getId()),
    null
  );
  private static final int VIA_FROM_STOP_INDEX = 47;
  private static final int VIA_TO_STOP_INDEX = 123;
  private static final List<RaptorAccessEgress> ACCESS = List.of(TestAccessEgress.walk(12, 45));
  private static final List<RaptorAccessEgress> EGRESS = List.of(TestAccessEgress.walk(144, 54));
  private static final WgsCoordinate VIA_COORDINATE = WgsCoordinate.GREENWICH;
  private static final ViaCoordinateTransfer VIA_TRANSFER = new ViaCoordinateTransfer(
    VIA_COORDINATE,
    VIA_FROM_STOP_INDEX,
    VIA_TO_STOP_INDEX,
    List.of(),
    List.of(),
    10,
    12.0
  );
  private static final VisitViaLocation VISIT_VIA_LOCATION_COORDINATE = new VisitViaLocation(
    "Via coordinate",
    Duration.ofMinutes(10),
    List.of(),
    VIA_COORDINATE
  );

  private static final CostLinearFunction R1 = CostLinearFunction.of("50 + 1.0x");
  private static final CostLinearFunction R2 = CostLinearFunction.of("0 + 1.5x");
  private static final CostLinearFunction R3 = CostLinearFunction.of("30 + 2.0x");

  private static final Map<FeedScopedId, StopLocation> STOPS_MAP = Map.of(STOP_A.getId(), STOP_A);
  private static final CostLinearFunction RELAX_TRANSIT_GROUP_PRIORITY = CostLinearFunction.of(
    "30m + 1.2t"
  );

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
    var req = requestBuilder();
    var minWaitTime = Duration.ofMinutes(13);

    req.withViaLocations(
      List.of(new VisitViaLocation("Via A", minWaitTime, List.of(STOP_A.getId()), null))
    );

    var result = map(req.buildRequest());

    assertTrue(result.searchParams().isVisitViaSearch());
    assertEquals(
      "[RaptorViaLocation{via Via A wait 13m : [(stop 0, 13m)]}]",
      result.searchParams().viaLocations().toString()
    );
  }

  @Test
  void testPassThroughPoints() {
    var req = requestBuilder();

    req.withViaLocations(List.of(new PassThroughViaLocation("Via A", List.of(STOP_A.getId()))));

    var result = map(req.buildRequest());

    assertTrue(result.searchParams().isPassThroughSearch());
    assertEquals(
      "[RaptorViaLocation{pass-through Via A : [(stop " + STOP_A.getIndex() + ")]}]",
      result.searchParams().viaLocations().toString()
    );
  }

  @Test
  void testViaCoordinate() {
    var req = requestBuilder();
    req.withViaLocations(List.of(VISIT_VIA_LOCATION_COORDINATE));

    req.withViaLocations(
      List.of(
        new VisitViaLocation("Via coordinate", Duration.ofMinutes(10), List.of(), VIA_COORDINATE)
      )
    );

    var result = map(req.buildRequest());

    assertFalse(result.searchParams().viaLocations().isEmpty());
    assertEquals(
      "[RaptorViaLocation{via Via coordinate wait 10m : [(stop 47 ~ 123, 10m10s)]}]",
      result.searchParams().viaLocations().toString()
    );
  }

  @Test
  void testTransitGroupPriority() {
    var req = requestBuilder();

    // Set relax transit-group-priority
    req.withPreferences(p ->
      p.withTransit(t -> t.withRelaxTransitGroupPriority(CostLinearFunction.of("30m + 1.2t")))
    );

    var result = map(req.buildRequest());

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
        List.of(VIA_VISIT, VIA_PASS_THROUGH, TRANSIT_GROUP_PRIORITY),
        List.of(),
        "A mix of via-locations and pass-through is not allowed in this version."
      ),
      Arguments.of(
        "VIA_PASS_THROUGH cannot be combined with other features, and other features are dropped",
        List.of(VIA_PASS_THROUGH, TRANSIT_GROUP_PRIORITY),
        List.of(VIA_PASS_THROUGH),
        null
      ),
      Arguments.of(
        "VIA_VISIT can be combined with TRANSIT_GROUP_PRIORITY",
        List.of(VIA_VISIT, TRANSIT_GROUP_PRIORITY),
        List.of(VIA_VISIT, TRANSIT_GROUP_PRIORITY),
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
    var builder = requestBuilder();

    for (RequestFeature it : requestedFeatures) {
      builder = setFeaturesOnRequest(builder, it);
    }

    if (errorMessage == null) {
      var result = map(builder.buildRequest());

      for (var feature : RequestFeature.values()) {
        assertFeatureSet(feature, result, expectedFeatures.contains(feature));
      }
    } else {
      var r = builder.buildRequest();
      var ex = Assertions.assertThrows(IllegalArgumentException.class, () -> map(r));
      assertEquals(errorMessage, ex.getMessage());
    }
  }

  @Test
  void testRaptorDegugRequest() {
    var request = requestBuilder()
      .withJourney(jb ->
        jb.withTransit(tb ->
          tb.withRaptorDebugging(db -> db.withStops(STOP_A.getId().toString()).withPath("2 3* 4"))
        )
      )
      .buildRequest();

    var result = map(request);
    var subject = result.debug();

    assertEquals(List.of(STOP_A.getIndex()), subject.stops());
    assertEquals(List.of(2, 3, 4), subject.path());
    assertEquals(1, subject.debugPathFromStopIndex());
  }

  private static RaptorRequest<TestTripSchedule> map(RouteRequest request) {
    return RaptorRequestMapper.<TestTripSchedule>of(
      request,
      ZonedDateTime.now(),
      false,
      ACCESS,
      EGRESS,
      null,
      new DummyViaCoordinateTransferFactory(),
      id -> IntStream.of(STOPS_MAP.get(id).getIndex()),
      new LinkingContext(
        Map.of(
          VISIT_VIA_LOCATION_COORDINATE.coordinateLocation(),
          Set.of(new LabelledIntersectionVertex("viapoint", 1, 1, false, false))
        ),
        Set.of(),
        Set.of()
      )
    ).mapRaptorRequest();
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
    }
  }

  private static RouteRequestBuilder requestBuilder() {
    return RouteRequest.of().withFrom(FROM).withTo(TO);
  }

  private static RouteRequestBuilder setFeaturesOnRequest(
    RouteRequestBuilder req,
    RequestFeature feature
  ) {
    return switch (feature) {
      case VIA_VISIT -> req.withViaLocations(
        ListUtils.combine(req.buildRequest().listViaLocations(), List.of(VISIT_VIA_LOCATION))
      );
      case VIA_PASS_THROUGH -> req.withViaLocations(
        ListUtils.combine(req.buildRequest().listViaLocations(), List.of(PASS_THROUGH_VIA_LOCATION))
      );
      case TRANSIT_GROUP_PRIORITY -> req.withPreferences(p ->
        p.withTransit(t -> t.withRelaxTransitGroupPriority(RELAX_TRANSIT_GROUP_PRIORITY))
      );
    };
  }

  enum RequestFeature {
    VIA_VISIT,
    VIA_PASS_THROUGH,
    TRANSIT_GROUP_PRIORITY,
  }

  private static class DummyViaCoordinateTransferFactory implements ViaCoordinateTransferFactory {

    @Override
    public List<ViaCoordinateTransfer> createViaTransfers(
      RouteRequest request,
      Vertex viaVertex,
      WgsCoordinate coordinate
    ) {
      // Make sure the input is the expected via-coordinate
      assertEquals(VIA_COORDINATE, coordinate);
      return List.of(VIA_TRANSFER);
    }
  }
}
