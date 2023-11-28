package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.collection.ListSection;
import org.opentripplanner.model.plan.Itinerary;

public class NumItinerariesFilterTest {

  private static final Itinerary i1 = newItinerary(A, 6).walk(1, B).build();
  private static final Itinerary i2 = newItinerary(A).bicycle(6, 8, B).build();
  private static final Itinerary i3 = newItinerary(A).bus(21, 7, 9, B).build();

  private NumItinerariesFilterResults subscribeResult = null;

  @Test
  public void name() {
    NumItinerariesFilter subject = new NumItinerariesFilter(3, ListSection.TAIL, null);
    assertEquals("number-of-itineraries-filter", subject.name());
  }

  @Test
  public void testCropHead() {
    NumItinerariesFilter subject = new NumItinerariesFilter(1, ListSection.HEAD, null);
    List<Itinerary> itineraries = List.of(i1, i2, i3);
    var result = DeletionFlaggerTestHelper.process(itineraries, subject);
    assertEquals(toStr(List.of(i3)), toStr(result));
  }

  @Test
  public void testCropTailAndSubscribe() {
    var subject = new NumItinerariesFilter(2, ListSection.TAIL, it -> subscribeResult = it);
    var itineraries = List.of(i1, i2, i3);

    var processedList = DeletionFlaggerTestHelper.process(itineraries, subject);

    assertEquals(
      i3.startTime().toInstant().toString(),
      subscribeResult.earliestRemovedDeparture().toString()
    );

    assertEquals(
      i3.startTime().toInstant().toString(),
      subscribeResult.latestRemovedDeparture().toString()
    );
    assertEquals(
      i3.endTime().toInstant().toString(),
      subscribeResult.latestRemovedArrival().toString()
    );

    assertEquals(
      i3.startTime().toInstant().toString(),
      subscribeResult.firstRemoved().startTimeAsInstant().toString()
    );

    assertEquals(
      i1.endTime().toInstant().toString(),
      subscribeResult.earliestKeptArrival().toString()
    );

    assertEquals(toStr(List.of(i1, i2)), toStr(processedList));
  }

  @Test
  public void testCropHeadAndSubscribe() {
    var subject = new NumItinerariesFilter(1, ListSection.HEAD, it -> subscribeResult = it);
    var itineraries = List.of(i1, i2, i3);

    var processedList = DeletionFlaggerTestHelper.process(itineraries, subject);

    assertEquals(
      i2.startTime().toInstant().toString(),
      subscribeResult.earliestRemovedDeparture().toString()
    );

    assertEquals(
      i2.startTime().toInstant().toString(),
      subscribeResult.latestRemovedDeparture().toString()
    );
    assertEquals(
      i2.endTime().toInstant().toString(),
      subscribeResult.latestRemovedArrival().toString()
    );

    assertEquals(
      i2.startTime().toInstant().toString(),
      subscribeResult.firstRemoved().startTimeAsInstant().toString()
    );

    assertEquals(
      i3.endTime().toInstant().toString(),
      subscribeResult.earliestKeptArrival().toString()
    );

    assertEquals(toStr(List.of(i3)), toStr(processedList));
  }
}
