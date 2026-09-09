package org.opentripplanner.model.impl;

import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.model.FeedInfoTestFactory;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

public class TransitDataImportBuilderTest {

  private static final String FEED_ID = TransitRepositoryForTest.FEED_ID;
  private static final FeedScopedId SERVICE_WEEKDAYS_ID = FeedScopedIdForTestFactory.id("weekdays");
  private static final LocalDate EXCLUDED_DATE = LocalDate.of(2017, 8, 31);

  private static TransitDataImportBuilder subject;

  @BeforeAll
  public static void setUpClass() throws IOException {
    subject = createBuilder();
  }

  @Test
  public void testGetAllCalendarDates() {
    // The exclusion date added in createBuilder() should not be an active service date.
    assertFalse(
      subject.tripCalendars().listServiceDates(SERVICE_WEEKDAYS_ID).contains(EXCLUDED_DATE)
    );
  }

  @Test
  public void testGetAllCalendars() {
    assertEquals(
      "[F:alldays, F:weekdays]",
      subject
        .tripCalendars()
        .listServiceIds()
        .stream()
        .map(Object::toString)
        .sorted()
        .toList()
        .toString()
    );
  }

  @Test
  public void testGetAllFrequencies() {
    List<Frequency> frequencies = subject
      .getFrequencies()
      .stream()
      .sorted(frequencyComp())
      .toList();

    assertEquals(2, frequencies.size());

    assertEquals(
      "Frequency{trip: F:15.1, start: 6:00, end: 10:00:01}",
      frequencies.get(0).toString()
    );
  }

  @Test
  public void testGetRoutes() {
    Collection<Route> routes = subject.getRoutes().values();

    assertEquals(19, routes.size());
    assertEquals("Route{F:1 BUS 1}", first(routes).toString());
  }

  @Test
  public void testGetAllShapePoints() {
    var shapePoints = subject
      .getShapePoints()
      .values()
      .stream()
      .flatMap(p -> ImmutableList.copyOf(p).stream())
      .toList();

    assertEquals(9, shapePoints.size());
    assertEquals("1 (41.0, -72.0) dist=0.0", first(shapePoints).toString());
  }

  /* private methods */

  private static TransitDataImportBuilder createBuilder() throws IOException {
    TransitDataImportBuilder builder = contextBuilder(
      FEED_ID,
      ConstantsForTests.SIMPLE_GTFS
    ).getTransitBuilder();

    // Supplement test data with at least one entity in all collections
    builder.tripCalendars().removeServiceDate(SERVICE_WEEKDAYS_ID, EXCLUDED_DATE);
    builder.getFeedInfos().add(FeedInfoTestFactory.dummyForTest(FEED_ID));

    return builder;
  }

  private static <T> T first(Collection<? extends T> c) {
    return c.stream().min(comparing(T::toString)).orElse(null);
  }

  private static Comparator<Frequency> frequencyComp() {
    return (l, r) -> {
      int c;
      c = l.trip().getId().toString().compareTo(r.trip().getId().toString());
      if (c != 0) {
        return c;
      }
      c = l.startTime() - r.startTime();
      if (c != 0) {
        return c;
      }
      return l.endTime() - r.endTime();
    };
  }
}
