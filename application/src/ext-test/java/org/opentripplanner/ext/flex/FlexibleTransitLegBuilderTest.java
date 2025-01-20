package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner._support.time.DateTimes.ZONED_DATE_TIME_1;
import static org.opentripplanner._support.time.DateTimes.ZONED_DATE_TIME_2;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.FARE_PRODUCT_USE;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.DateTimes;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model._data.StreetModelForTest;

class FlexibleTransitLegBuilderTest implements PlanTestConstants {

  private static final FlexTripEdge EDGE = new FlexTripEdge(
    StreetModelForTest.intersectionVertex(1,1),
    StreetModelForTest.intersectionVertex(2,2),
    A.stop,
    B.stop,
    null,
    1,
    2,
    LocalDate.of(2025, 1, 15),
    new FlexPath(1000, 600, () -> GeometryUtils.makeLineString(1,1,2,2))
  );
  private static final TransitAlert ALERT = TransitAlert.of(id("alert")).withHeaderText(I18NString.of("alert 1")).build();
  private static final Duration TIME_SHIFT = Duration.ofHours(5);

  @Test
  void listsAreInitialized(){
    var leg = new FlexibleTransitLegBuilder().withStartTime(ZONED_DATE_TIME_1).withEndTime(ZONED_DATE_TIME_2).withFlexTripEdge(EDGE).build();
    assertNotNull(leg.fareProducts());
    assertNotNull(leg.getTransitAlerts());
  }

  @Test
  void everythingIsNonNull(){
    var expectedType = RuntimeException.class;
    assertThrows(expectedType,()-> new FlexibleTransitLegBuilder().withStartTime(null).build());
    assertThrows(expectedType,()-> new FlexibleTransitLegBuilder().withEndTime(null).build());
    assertThrows(expectedType,()-> new FlexibleTransitLegBuilder().withFlexTripEdge(null).build());
    assertThrows(expectedType,()-> new FlexibleTransitLegBuilder().withAlerts(null).build());
    assertThrows(expectedType,()-> new FlexibleTransitLegBuilder().withFareProducts(null).build());
  }

  @Test
  void copy(){
    var leg = new FlexibleTransitLegBuilder().withStartTime(ZONED_DATE_TIME_1).withEndTime(ZONED_DATE_TIME_2).withFlexTripEdge(EDGE)
      .withFareProducts(List.of(FARE_PRODUCT_USE)).withAlerts(Set.of(ALERT)).build();

    var copy = leg.copy().build();

    assertEquals(copy.flexTripEdge(), EDGE);
    assertEquals(copy.getStartTime(), ZONED_DATE_TIME_1);
    assertEquals(copy.getEndTime(), ZONED_DATE_TIME_2);
    assertEquals(copy.getTransitAlerts(), Set.of(ALERT));
    assertEquals(copy.fareProducts(), List.of(FARE_PRODUCT_USE));
  }

  @Test
  void timeShift(){
    var leg = new FlexibleTransitLegBuilder().withStartTime(ZONED_DATE_TIME_1).withEndTime(ZONED_DATE_TIME_2).withFlexTripEdge(EDGE).withFareProducts(List.of(FARE_PRODUCT_USE)).build();

    var shifted = leg.withTimeShift(TIME_SHIFT);

    assertEquals(ZONED_DATE_TIME_1.plus(TIME_SHIFT), shifted.getStartTime());
    assertEquals(ZONED_DATE_TIME_2.plus(TIME_SHIFT), shifted.getEndTime());
    assertEquals(List.of(FARE_PRODUCT_USE), shifted.fareProducts());
  }
}