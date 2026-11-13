package org.opentripplanner.updater.elevator.siri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;
import uk.org.siri.siri21.AccessFacilityEnumeration;
import uk.org.siri.siri21.AllFacilitiesFeatureStructure;
import uk.org.siri.siri21.FacilityConditionStructure;
import uk.org.siri.siri21.Siri;

/**
 * Maps the facility conditions of a SIRI-FM delivery to the status of the elevators.
 * <p>
 * The {@code FacilityRef} is the elevator id, see {@code ElevatorHopEdge#id()}. A condition whose
 * facility features are given, and do not include a lift, is not an elevator and is skipped. The
 * {@code FacilityStatus} is mapped like this:
 * <ul>
 *   <li>{@code available} and {@code added}: in service</li>
 *   <li>{@code notAvailable} and {@code removed}: out of service</li>
 *   <li>{@code unknown}, {@code partiallyAvailable} and a missing status: see
 *   {@link UnknownElevatorStatus}</li>
 * </ul>
 * The mapper is stateful: it keeps the last known status of each elevator for
 * {@link UnknownElevatorStatus#KEEP_PREVIOUS}. It is not thread-safe.
 */
class SiriFmElevatorMapper {

  private final UnknownElevatorStatus unknownStatus;
  private Map<String, Boolean> lastKnownInService = Map.of();

  SiriFmElevatorMapper(UnknownElevatorStatus unknownStatus) {
    this.unknownStatus = unknownStatus;
  }

  /** The status of all the elevators in the delivery. */
  List<ElevatorStatus> map(Siri siri) {
    var result = new ArrayList<ElevatorStatus>();
    var known = new HashMap<String, Boolean>();

    conditions(siri)
      .filter(SiriFmElevatorMapper::isElevator)
      .forEach(condition -> {
        var id = condition.getFacilityRef().getValue();
        var inService = inService(condition);
        if (inService.isPresent()) {
          known.put(id, inService.get());
          result.add(new ElevatorStatus(id, inService.get()));
        } else {
          result.add(new ElevatorStatus(id, inServiceIfUnknown(id)));
        }
      });

    if (unknownStatus == UnknownElevatorStatus.KEEP_PREVIOUS) {
      // An elevator that is unknown now keeps its last known status for the next delivery too
      var next = new HashMap<>(lastKnownInService);
      next.keySet().retainAll(result.stream().map(ElevatorStatus::elevatorId).toList());
      next.putAll(known);
      lastKnownInService = Map.copyOf(next);
    }
    return result;
  }

  private boolean inServiceIfUnknown(String id) {
    return switch (unknownStatus) {
      case IN_SERVICE -> true;
      case OUT_OF_SERVICE -> false;
      case KEEP_PREVIOUS -> lastKnownInService.getOrDefault(id, true);
    };
  }

  private static Stream<FacilityConditionStructure> conditions(Siri siri) {
    return Stream.ofNullable(siri.getServiceDelivery())
      .flatMap(delivery -> delivery.getFacilityMonitoringDeliveries().stream())
      .flatMap(delivery -> delivery.getFacilityConditions().stream());
  }

  private static boolean isElevator(FacilityConditionStructure condition) {
    if (condition.getFacilityRef() == null || condition.getFacilityRef().getValue() == null) {
      return false;
    }
    var facility = condition.getFacility();
    if (
      facility == null ||
      facility.getFeatures() == null ||
      facility.getFeatures().getFeatures().isEmpty()
    ) {
      return true;
    }
    return facility
      .getFeatures()
      .getFeatures()
      .stream()
      .map(AllFacilitiesFeatureStructure::getAccessFacility)
      .anyMatch(AccessFacilityEnumeration.LIFT::equals);
  }

  /** Whether the elevator is in service, empty if the status is unknown. */
  private static Optional<Boolean> inService(FacilityConditionStructure condition) {
    var status =
      condition.getFacilityStatus() == null ? null : condition.getFacilityStatus().getStatus();
    if (status == null) {
      return Optional.empty();
    }
    return switch (status) {
      case AVAILABLE, ADDED -> Optional.of(true);
      case NOT_AVAILABLE, REMOVED -> Optional.of(false);
      case UNKNOWN, PARTIALLY_AVAILABLE -> Optional.empty();
    };
  }
}
