package org.opentripplanner.ext.flex.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.StopLocation;

class FlexTripEdgeTest {

  private static final FlexPath FLEX_PATH = new FlexPath(1, 0, () ->
    GeometryUtils.makeLineString(0, 0, 1, 1)
  );
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 1);

  private static final TimetableRepositoryForTest model = TimetableRepositoryForTest.of();
  private static final StopLocation AREA_STOP = model.areaStop("1").build();
  private static final StopLocation REGULAR_STOP = model.stop("2").build();

  public static List<Arguments> cases() {
    return List.of(
      Arguments.of(AREA_STOP, AREA_STOP, 0, 0, 600),
      Arguments.of(AREA_STOP, AREA_STOP, 100, 0, 700),
      Arguments.of(AREA_STOP, AREA_STOP, 0, 100, 700),
      Arguments.of(AREA_STOP, AREA_STOP, 100, 100, 800),
      Arguments.of(AREA_STOP, REGULAR_STOP, 100, 100, 700),
      Arguments.of(REGULAR_STOP, AREA_STOP, 100, 100, 700),
      Arguments.of(REGULAR_STOP, REGULAR_STOP, 100, 100, 600),
      Arguments.of(REGULAR_STOP, REGULAR_STOP, 0, 0, 600)
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  void applyCost(
    StopLocation boardStop,
    StopLocation alightStop,
    int boardCost,
    int alightCost,
    int expectedWeight
  ) {
    State state = TestStateBuilder.ofWalking().build();
    assertEquals(0, state.getWeight());
    var edge = new FlexTripEdge(
      state.getVertex(),
      new SimpleVertex("v2", 1, 1),
      boardStop,
      alightStop,
      null,
      0,
      1,
      SERVICE_DATE,
      FLEX_PATH,
      flexParameters(boardCost, alightCost)
    );
    State[] result = edge.traverse(state);

    assertEquals(1, result.length);

    var s = result[0];
    assertEquals(expectedWeight, s.getWeight());
  }

  private FlexParameters flexParameters(int boardCost, int alightCost) {
    return new FlexParameters() {
      @Override
      public Duration maxTransferDuration() {
        return null;
      }

      @Override
      public Duration maxFlexTripDuration() {
        return null;
      }

      @Override
      public Duration maxAccessWalkDuration() {
        return null;
      }

      @Override
      public Duration maxEgressWalkDuration() {
        return null;
      }

      @Override
      public Cost areaStopBoardCost() {
        return Cost.costOfSeconds(boardCost);
      }

      @Override
      public Cost areaStopAlightCost() {
        return Cost.costOfSeconds(alightCost);
      }
    };
  }
}
