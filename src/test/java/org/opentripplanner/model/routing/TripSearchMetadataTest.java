package org.opentripplanner.model.routing;

import org.junit.Test;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class TripSearchMetadataTest {

  public static final Duration THIRTY_MINUTES = Duration.ofMinutes(30);
  public static final int SEARCH_WINDOW_USED = (int) THIRTY_MINUTES.toSeconds();

  @Test
  public void createMetadataForArriveWithSearchWindowOnly() {
    TripSearchMetadata subject = TripSearchMetadata.createForArriveBy(
        Instant.parse("2020-05-17T10:20:00Z"), SEARCH_WINDOW_USED, null
    );
    assertEquals(THIRTY_MINUTES, subject.searchWindowUsed);
    assertEquals("2020-05-17T09:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:50:00Z", subject.nextDateTime.toString());
  }

  @Test
  public void createMetadataForArriveByWithTimeGiven() {
    TripSearchMetadata subject;

    // New arrival-time with seconds, 10:05:15, should be rounded up to 10:06:00
    subject= TripSearchMetadata.createForArriveBy(
        Instant.parse("2020-05-17T10:20:00Z"),
        SEARCH_WINDOW_USED,
        Instant.parse("2020-05-17T10:05:15Z")
    );
    assertEquals(THIRTY_MINUTES, subject.searchWindowUsed);
    assertEquals("2020-05-17T10:06:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:50:00Z", subject.nextDateTime.toString());


    // New arrival-time without seconds, 10:05:00, should stay the same: 10:05:00
    subject = TripSearchMetadata.createForArriveBy(
        Instant.parse("2020-05-17T11:20:00Z"),
        SEARCH_WINDOW_USED,
        Instant.parse("2020-05-17T11:05:00Z")
    );
    assertEquals("2020-05-17T11:05:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T11:50:00Z", subject.nextDateTime.toString());
  }

  @Test
  public void createMetadataForDepartAfterWithSearchWindowOnly() {
    TripSearchMetadata subject = TripSearchMetadata.createForDepartAfter(
        Instant.parse("2020-05-17T10:20:00Z"), SEARCH_WINDOW_USED, null
    );
    assertEquals(THIRTY_MINUTES, subject.searchWindowUsed);
    assertEquals("2020-05-17T09:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:50:00Z", subject.nextDateTime.toString());
  }

  @Test
  public void createMetadataForDepartAfterWithTimeGiven() {
    TripSearchMetadata subject;

    // New departure-time with seconds, 10:35:15, should be rounded down to 10:35:00
    subject= TripSearchMetadata.createForDepartAfter(
        Instant.parse("2020-05-17T10:20:00Z"),
        SEARCH_WINDOW_USED,
        Instant.parse("2020-05-17T10:35:15Z")
    );
    assertEquals(THIRTY_MINUTES, subject.searchWindowUsed);
    assertEquals("2020-05-17T09:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T10:35:00Z", subject.nextDateTime.toString());


    // New departure-time without seconds, 11:35:00, should stay the same: 11:35:00
    subject = TripSearchMetadata.createForDepartAfter(
        Instant.parse("2020-05-17T11:20:00Z"),
        SEARCH_WINDOW_USED,
        Instant.parse("2020-05-17T11:35:00Z")
    );
    assertEquals("2020-05-17T10:50:00Z", subject.prevDateTime.toString());
    assertEquals("2020-05-17T11:35:00Z", subject.nextDateTime.toString());
  }
}