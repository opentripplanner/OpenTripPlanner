package org.opentripplanner.netex.validation;

import jakarta.xml.bind.JAXBElement;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.StopPlace;

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
      "ScheduledStopPoint %s is assigned to StopPlace %s, which contains no quays.",
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
      // we could use the WgsCoordinateMapper to check for more invalid data
      return Optional.ofNullable(q.getCentroid())
        .map(c -> c.getLocation())
        .isPresent();
    }
    // could be a QuayRef which we don't support (yet)
    return false;
  }
}
