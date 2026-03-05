package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.basic.Cost.costOfSeconds;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.street.search.request.filter.ParkingFilter;
import org.opentripplanner.street.search.request.filter.ParkingSelect.TagsSelect;

class ParkingRequestTest {

  private static final Set<String> PREFERRED_TAGS = Set.of("foo");
  private static final Set<String> NOT_PREFERRED_TAGS = Set.of("bar");
  private static final int UNPREFERRED_COST = 360;
  private static final Set<String> REQUIRED_TAGS = Set.of("bar");
  private static final Set<String> BANNED_TAGS = Set.of("not");
  private static final Cost PARKING_COST = Cost.costOfMinutes(4);
  private static final Duration PARKING_TIME = Duration.ofMinutes(2);

  private final ParkingRequest subject = createRequest();

  @Test
  void preferred() {
    assertEquals(tagsToString(PREFERRED_TAGS), subject.preferred().select().toString());
    assertEquals(tagsToString(NOT_PREFERRED_TAGS), subject.preferred().not().toString());
  }

  @Test
  void filter() {
    assertEquals(tagsToString(REQUIRED_TAGS), subject.filter().select().toString());
    assertEquals(tagsToString(BANNED_TAGS), subject.filter().not().toString());
  }

  @Test
  void unpreferredCost() {
    assertEquals(UNPREFERRED_COST, subject.unpreferredVehicleParkingTagCost().toSeconds());
  }

  @Test
  void cost() {
    assertEquals(PARKING_COST, subject.cost());
  }

  @Test
  void time() {
    assertEquals(PARKING_TIME, subject.time());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withCost(Cost.costOfSeconds(10)).build();
    var same = other.copyOf().withCost(PARKING_COST).build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("ParkingRequest{}", ParkingRequest.DEFAULT.toString());
    assertEquals(
      "ParkingRequest{" +
        "unpreferredVehicleParkingTagCost: $360, " +
        "filter: ParkingFilter{not: [tags=[not]], select: [tags=[bar]]}, " +
        "preferred: ParkingFilter{not: [tags=[bar]], select: [tags=[foo]]}, " +
        "cost: $240, " +
        "time: PT2M}",
      subject.toString()
    );
  }

  private static String tagsToString(Set<String> tags) {
    return "[tags=" + tags + "]";
  }

  private ParkingRequest createRequest() {
    return ParkingRequest.of()
      .withPreferred(
        new ParkingFilter(new TagsSelect(NOT_PREFERRED_TAGS), new TagsSelect(PREFERRED_TAGS))
      )
      .withFilter(new ParkingFilter(new TagsSelect(BANNED_TAGS), new TagsSelect(REQUIRED_TAGS)))
      .withCost(PARKING_COST)
      .withUnpreferredTagCost(costOfSeconds(UNPREFERRED_COST))
      .withTime(PARKING_TIME)
      .build();
  }
}
