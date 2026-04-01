package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.utils.collection.ListSection;

public class NumItinerariesFilterTest {

  private static final Itinerary I1 = newItinerary(A, 6).walk(1, B).build();
  private static final Itinerary I2 = newItinerary(A).bicycle(6, 8, B).build();
  private static final Itinerary I3 = newItinerary(A).bus(21, 7, 9, B).build();

  @Test
  public void name() {
    NumItinerariesFilter subject = new NumItinerariesFilter(3, ListSection.TAIL);
    assertEquals("number-of-itineraries-filter", subject.name());
  }

  @Test
  public void testCropHead() {
    NumItinerariesFilter subject = new NumItinerariesFilter(1, ListSection.HEAD);
    List<Itinerary> itineraries = List.of(I1, I2, I3);
    var result = subject.removeMatchesForTest(itineraries);
    assertEquals(toStr(List.of(I3)), toStr(result));
  }

  @Test
  public void testCropTailAndSubscribe() {
    var subject = new NumItinerariesFilter(2, ListSection.TAIL);
    var itineraries = List.of(I1, I2, I3);

    var processedList = subject.removeMatchesForTest(itineraries);

    assertEquals(
      I3.startTime().toInstant().toString(),
      subject.getNumItinerariesFilterResult().earliestRemovedDeparture().toString()
    );

    assertEquals(
      I3.startTime().toInstant().toString(),
      subject.getNumItinerariesFilterResult().latestRemovedDeparture().toString()
    );

    assertEquals(I2.keyAsString(), subject.getNumItinerariesFilterResult().pageCut().keyAsString());

    assertEquals(toStr(List.of(I1, I2)), toStr(processedList));
  }

  @Test
  public void testCropHeadAndSubscribe() {
    var subject = new NumItinerariesFilter(1, ListSection.HEAD);
    var itineraries = List.of(I1, I2, I3);

    var processedList = subject.removeMatchesForTest(itineraries);

    assertEquals(
      I2.startTime().toInstant().toString(),
      subject.getNumItinerariesFilterResult().earliestRemovedDeparture().toString()
    );

    assertEquals(
      I2.startTime().toInstant().toString(),
      subject.getNumItinerariesFilterResult().latestRemovedDeparture().toString()
    );

    assertEquals(I3.keyAsString(), subject.getNumItinerariesFilterResult().pageCut().keyAsString());

    assertEquals(toStr(List.of(I3)), toStr(processedList));
  }
}
