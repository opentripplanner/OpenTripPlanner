package org.opentripplanner.netex.validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.StopPlace;

/**
 * Ensure stop place exists for PassengerStopAssignment.
 */
class PassengerStopAssignmentToStopPlaceWithoutQuays
  extends AbstractHMapValidationRule<String, String>
{

  @Override
  public Status validate(String stopPlaceRef) {
    var stopPlace = index.getStopPlaceById().lookupLastVersionById(stopPlaceRef);
    if (hasAtLeastOneQuay(stopPlace)) {
      return Status.OK;
    } else {
      return Status.DISCARD;
    }
  }

  private static boolean hasAtLeastOneQuay(StopPlace stopPlace) {
    return Optional.ofNullable(stopPlace.getQuays())
      .map(Quays_RelStructure::getQuayRefOrQuay)
      .filter(Predicate.not(List::isEmpty))
      .isPresent();
  }

  @Override
  public DataImportIssue logMessage(String stopPointRef, String stopPlaceRef) {
    return Issue.issue(
      "InvalidPassengerStopAssignment",
      "ScheduledStopPoint %s is assigned to StopPlace %s, which contains no quays.",
      stopPointRef,
      stopPlaceRef
    );
  }
}
