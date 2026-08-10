package org.opentripplanner.updater.trip.siri.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.org.siri.siri21.DataFrameRefStructure;
import uk.org.siri.siri21.DatedVehicleJourneyRef;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri21.VehicleRef;

class TripReferenceHelperTest {

  @Test
  void framedVehicleJourneyRefWithServiceDateIsPreferred() {
    var journey = new EstimatedVehicleJourney();
    journey.setFramedVehicleJourneyRef(framed("SJ:1", "2026-06-29"));
    journey.setDatedVehicleJourneyRef(datedRef("DSJ:1"));
    assertEquals("SJ:1 (2026-06-29)", TripReferenceHelper.tripReference(journey));
  }

  @Test
  void framedVehicleJourneyRefWithoutServiceDate() {
    var journey = new EstimatedVehicleJourney();
    journey.setFramedVehicleJourneyRef(framed("SJ:1", null));
    assertEquals("SJ:1", TripReferenceHelper.tripReference(journey));
  }

  @Test
  void fallBackToDatedVehicleJourneyRef() {
    var journey = new EstimatedVehicleJourney();
    journey.setDatedVehicleJourneyRef(datedRef("DSJ:1"));
    assertEquals("DSJ:1", TripReferenceHelper.tripReference(journey));
  }

  @Test
  void fallBackToEstimatedVehicleJourneyCode() {
    var journey = new EstimatedVehicleJourney();
    journey.setEstimatedVehicleJourneyCode("EVJ:1");
    assertEquals("EVJ:1", TripReferenceHelper.tripReference(journey));
  }

  @Test
  void fallBackToVehicleRef() {
    var journey = new EstimatedVehicleJourney();
    journey.setVehicleRef(vehicleRef("2051"));
    assertEquals("2051", TripReferenceHelper.tripReference(journey));
  }

  @Test
  void noReferenceAtAll() {
    assertNull(TripReferenceHelper.tripReference(new EstimatedVehicleJourney()));
    assertNull(TripReferenceHelper.tripReference(null));
  }

  private static FramedVehicleJourneyRefStructure framed(String vehicleJourneyRef, String date) {
    var framed = new FramedVehicleJourneyRefStructure();
    framed.setDatedVehicleJourneyRef(vehicleJourneyRef);
    if (date != null) {
      var dataFrameRef = new DataFrameRefStructure();
      dataFrameRef.setValue(date);
      framed.setDataFrameRef(dataFrameRef);
    }
    return framed;
  }

  private static DatedVehicleJourneyRef datedRef(String value) {
    var ref = new DatedVehicleJourneyRef();
    ref.setValue(value);
    return ref;
  }

  private static VehicleRef vehicleRef(String value) {
    var ref = new VehicleRef();
    ref.setValue(value);
    return ref;
  }
}
