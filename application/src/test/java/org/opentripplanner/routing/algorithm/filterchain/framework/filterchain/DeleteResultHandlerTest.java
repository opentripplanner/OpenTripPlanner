package org.opentripplanner.routing.algorithm.filterchain.framework.filterchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.D1m;
import static org.opentripplanner.model.plan.PlanTestConstants.D2m;
import static org.opentripplanner.model.plan.PlanTestConstants.E;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_06;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_09;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_33;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.LIMIT_TO_NUM_OF_ITINERARIES;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.LIMIT_TO_SEARCH_WINDOW;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.LIST_ALL;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.OFF;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.NumItinerariesFilter;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.OutsideSearchWindowFilter;

class DeleteResultHandlerTest {

  private static final String NO_TAG = null;
  private static final String ANY_TAG = "any-tag";
  private static final String OUT_SW_TAG = OutsideSearchWindowFilter.TAG;
  private static final String MAX_N_TAG = NumItinerariesFilter.TAG;
  private static final List<Boolean> EXP_ALL = List.of(true, true, true);
  private static final List<Boolean> EXP_1_AND_2 = List.of(true, true, false);
  private static final List<Boolean> EXP_1 = List.of(true, false, false);

  public static List<TestCase> filterOffTestCases() {
    return List.of(
      new TestCase(10, NO_TAG, NO_TAG, EXP_ALL),
      new TestCase(10, NO_TAG, ANY_TAG, EXP_1_AND_2),
      new TestCase(2, NO_TAG, MAX_N_TAG, EXP_1_AND_2),
      new TestCase(10, ANY_TAG, ANY_TAG, EXP_1),
      new TestCase(1, MAX_N_TAG, MAX_N_TAG, EXP_1)
    );
  }

  @ParameterizedTest
  @MethodSource("filterOffTestCases")
  void filterOff(TestCase tc) {
    var result = new DeleteResultHandler(OFF, tc.numItineraries).filter(tc.input());
    assertEquals(Itinerary.toStr(tc.expected), Itinerary.toStr(result));
  }

  public static List<TestCase> filterListAllTestCases() {
    return List.of(
      new TestCase(10, NO_TAG, NO_TAG, EXP_ALL),
      new TestCase(10, NO_TAG, ANY_TAG, EXP_ALL),
      new TestCase(10, ANY_TAG, ANY_TAG, EXP_ALL),
      new TestCase(2, NO_TAG, MAX_N_TAG, EXP_ALL),
      new TestCase(1, MAX_N_TAG, MAX_N_TAG, EXP_ALL)
    );
  }

  @ParameterizedTest
  @MethodSource("filterListAllTestCases")
  void filterListAll(TestCase tc) {
    var result = new DeleteResultHandler(LIST_ALL, tc.numItineraries).filter(tc.input());
    assertEquals(Itinerary.toStr(tc.expected), Itinerary.toStr(result));
  }

  public static List<TestCase> filterLimitToSearchWindowTestCases() {
    return List.of(
      // numItineraries should not have any effect, only the OutsideSearchWindowFilter.TAG
      new TestCase(1, NO_TAG, NO_TAG, EXP_ALL),
      new TestCase(1, NO_TAG, ANY_TAG, EXP_ALL),
      new TestCase(1, ANY_TAG, ANY_TAG, EXP_ALL),
      new TestCase(10, NO_TAG, OUT_SW_TAG, EXP_1_AND_2),
      new TestCase(10, OUT_SW_TAG, OUT_SW_TAG, EXP_1)
    );
  }

  @ParameterizedTest
  @MethodSource("filterLimitToSearchWindowTestCases")
  void filterLimitToSearchWindow(TestCase tc) {
    var result = new DeleteResultHandler(LIMIT_TO_SEARCH_WINDOW, tc.numItineraries).filter(
      tc.input()
    );
    assertEquals(Itinerary.toStr(tc.expected), Itinerary.toStr(result));
  }

  public static List<TestCase> filterLimitToNumOfItinerariesTestCases() {
    return List.of(
      new TestCase(10, NO_TAG, NO_TAG, EXP_ALL),
      new TestCase(10, NO_TAG, ANY_TAG, EXP_ALL),
      new TestCase(10, ANY_TAG, ANY_TAG, EXP_ALL),
      // limit to 2 results independent of tags
      new TestCase(2, NO_TAG, NO_TAG, EXP_1_AND_2),
      new TestCase(2, NO_TAG, ANY_TAG, EXP_1_AND_2),
      // limit to 1 results independent of tags
      new TestCase(1, NO_TAG, NO_TAG, EXP_1),
      new TestCase(1, ANY_TAG, ANY_TAG, EXP_1)
    );
  }

  @ParameterizedTest
  @MethodSource("filterLimitToNumOfItinerariesTestCases")
  void filterLimitToNumOfItineraries(TestCase tc) {
    var result = new DeleteResultHandler(LIMIT_TO_NUM_OF_ITINERARIES, tc.numItineraries).filter(
      tc.input()
    );
    assertEquals(Itinerary.toStr(tc.expected), Itinerary.toStr(result));
  }

  static class TestCase {

    final int numItineraries;
    final List<Itinerary> expected;
    final Itinerary i1;
    final Itinerary i2;
    final Itinerary i3;

    TestCase(
      int numItineraries,
      @Nullable String i2Tag,
      @Nullable String i3Tag,
      List<Boolean> expected
    ) {
      this.numItineraries = numItineraries;
      this.i1 = newItinerary(A, T11_06).walk(D2m, E).build();
      this.i2 = newItinerary(A).bus(21, T11_06, T11_09, E).build();
      this.i3 = newItinerary(A).bus(20, T11_33, T11_33 + D1m, E).build();
      addSystemNotice(i2, i2Tag);
      addSystemNotice(i3, i3Tag);
      this.expected = expected(List.of(i1, i2, i3), expected);
    }

    List<Itinerary> input() {
      return List.of(i1, i2, i3);
    }

    private static List<Itinerary> expected(List<Itinerary> all, List<Boolean> index) {
      var list = new ArrayList<Itinerary>();
      for (int i = 0; i < 3; i++) {
        if (index.get(i)) {
          list.add(all.get(i));
        }
      }
      return list;
    }

    @Override
    public String toString() {
      // Include test input, not expected
      return ("n:" + numItineraries + " tags: na " + tag(i2) + " " + tag(i3));
    }

    private static void addSystemNotice(Itinerary it, String tag) {
      if (tag == null) {
        return;
      }
      it.flagForDeletion(new SystemNotice(tag, "Any Text"));
    }

    private String tag(Itinerary it) {
      return it.getSystemNotices().stream().map(SystemNotice::tag).findFirst().orElse("na");
    }
  }
}
