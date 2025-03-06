package org.opentripplanner.model.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

public class TripSearchMetadataTest {

  private static final Duration SEARCH_WINDOW_USED = Duration.ofMinutes(30);

  @Test
  void createMetadataForArriveWithSearchWindowOnly() {
    TripSearchMetadata subject = TripSearchMetadata.createForArriveBy(
      Instant.parse("2020-05-17T10:20:00Z"),
      SEARCH_WINDOW_USED,
      null
    );
    assertEquals(SEARCH_WINDOW_USED, subject.searchWindowUsed);
    assertEquals("2020-05-17T09:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:50:00Z", subject.nextDateTime.toString());
  }

  @Test
  void createMetadataForArriveByWithTimeGiven() {
    TripSearchMetadata subject;

    // New arrival-time with seconds, 10:35:01, should be rounded up to 10:36:00
    subject = TripSearchMetadata.createForArriveBy(
      Instant.parse("2020-05-17T10:20:00Z"),
      SEARCH_WINDOW_USED,
      Instant.parse("2020-05-17T10:35:00Z")
    );
    assertEquals(SEARCH_WINDOW_USED, subject.searchWindowUsed);
    assertEquals("2020-05-17T10:04:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:50:00Z", subject.nextDateTime.toString());

    // New arrival-time without seconds, 10:36:00, should stay the same: 10:36:00
    subject = TripSearchMetadata.createForArriveBy(
      Instant.parse("2020-05-17T11:20:00Z"),
      SEARCH_WINDOW_USED,
      Instant.parse("2020-05-17T11:35:59Z")
    );
    assertEquals("2020-05-17T11:04:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T11:50:00Z", subject.nextDateTime.toString());
  }

  @Test
  void createMetadataForDepartAfterWithSearchWindowOnly() {
    TripSearchMetadata subject = TripSearchMetadata.createForDepartAfter(
      Instant.parse("2020-05-17T10:20:00Z"),
      SEARCH_WINDOW_USED,
      null
    );
    assertEquals(SEARCH_WINDOW_USED, subject.searchWindowUsed);
    assertEquals("2020-05-17T09:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:50:00Z", subject.nextDateTime.toString());
  }

  @Test
  void createMetadataForDepartAfterWithTimeGiven() {
    TripSearchMetadata subject;

    // New departure-time, 10:35:00, should be rounded up to 10:36:00
    subject = TripSearchMetadata.createForDepartAfter(
      Instant.parse("2020-05-17T10:20:00Z"),
      SEARCH_WINDOW_USED,
      Instant.parse("2020-05-17T10:35:00Z")
    );
    assertEquals(SEARCH_WINDOW_USED, subject.searchWindowUsed);
    assertEquals("2020-05-17T09:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:36:00Z", subject.nextDateTime.toString());

    // New departure-time, 11:35:59, should be rounded up to 11:36:00
    subject = TripSearchMetadata.createForDepartAfter(
      Instant.parse("2020-05-17T11:20:00Z"),
      SEARCH_WINDOW_USED,
      Instant.parse("2020-05-17T11:35:59Z")
    );
    assertEquals("2020-05-17T10:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T11:36:00Z", subject.nextDateTime.toString());
  }
}
