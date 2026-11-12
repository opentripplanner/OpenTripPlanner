package org.opentripplanner.netex.validation;

import jakarta.xml.bind.JAXBElement;
import java.util.Optional;
import org.entur.netex2.model.LocationStructure;
import org.entur.netex2.model.Quay;
import org.entur.netex2.model.Quays_RelStructure;
import org.entur.netex2.model.SimplePoint_VersionStructure;
import org.entur.netex2.model.StopPlace;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Ensure stop place exists for PassengerStopAssignment.
 */
class PassengerStopAssignmentToQuaylessStopPlace
  extends AbstractHMapValidationRule<String, String>
{

  @Override
  public Status validate(String stopPlaceRef) {
    var stopPlace = index.getStopPlaceById().lookupLastVersionById(stopPlaceRef);
    if (hasAtLeastOneUsableQuay(stopPlace)) {
      return Status.OK;
    } else {
      return Status.DISCARD;
    }
  }

  @Override
  public DataImportIssue logMessage(String stopPointRef, String stopPlaceRef) {
    return Issue.issue(
      "PassengerStopAssignmentToQuaylessStopPlace",
      "ScheduledStopPoint %s is assigned to StopPlace %s, which contains no usable quays.",
      stopPointRef,
      stopPlaceRef
    );
  }

  private static boolean hasAtLeastOneUsableQuay(@Nullable StopPlace stopPlace) {
    return Optional.ofNullable(stopPlace)
      .map(sp -> sp.getQuays())
      .map(Quays_RelStructure::getQuayRefOrQuay)
      .filter(quays -> quays.stream().anyMatch(q -> isUsable(q)))
      .isPresent();
  }

  private static boolean isUsable(JAXBElement<?> element) {
    if (element.getValue() instanceof Quay q) {
      return Optional.ofNullable(q.getCentroid())
        .map(SimplePoint_VersionStructure::getLocation)
        .filter(PassengerStopAssignmentToQuaylessStopPlace::hasValidCoordinate)
        .isPresent();
    }
    // could be a QuayRef which we don't support (yet)
    return false;
  }

  /**
   * Mirrors the coordinate rules of the stop mapping (see {@code WgsCoordinateMapper}): a location
   * is usable if it has both latitude and longitude, or a GML position with 2 or 3 values, and the
   * values are within the WGS84 range.
   */
  private static boolean hasValidCoordinate(LocationStructure location) {
    if (location.getLatitude() != null && location.getLongitude() != null) {
      return WgsCoordinate.isValid(
        location.getLatitude().doubleValue(),
        location.getLongitude().doubleValue()
      );
    }
    var pos = location.getPos();
    if (pos == null || (pos.getValue().size() != 2 && pos.getValue().size() != 3)) {
      return false;
    }
    return WgsCoordinate.isValid(pos.getValue().getFirst(), pos.getValue().get(1));
  }
}
