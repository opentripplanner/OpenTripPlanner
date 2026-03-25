package org.opentripplanner.netex.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.config.SwissProfile;
import org.opentripplanner.netex.issues.InterchangeMaxWaitTimeNotGuaranteed;
import org.opentripplanner.netex.issues.InterchangePointMappingFailed;
import org.opentripplanner.netex.issues.InterchangeWithoutConstraint;
import org.opentripplanner.netex.issues.ObjectNotFound;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;
import org.opentripplanner.transfer.constrained.model.TransferConstraint;
import org.opentripplanner.transfer.constrained.model.TransferPriority;
import org.opentripplanner.transfer.constrained.model.TripTransferPoint;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.timetable.Trip;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

public class TransferMapper {

  private final FeedScopedIdFactory idFactory;
  private final DataImportIssueStore issueStore;
  private final Map<String, List<String>> scheduledStopPointsIndex;
  private final EntityById<Trip> trips;

  public TransferMapper(
    FeedScopedIdFactory idFactory,
    DataImportIssueStore issueStore,
    Map<String, List<String>> scheduledStopPointsIndex,
    EntityById<Trip> trips
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.scheduledStopPointsIndex = scheduledStopPointsIndex;
    this.trips = trips;
  }

  /**
   * Map a NeTEx ServiceJourneyInterchange to one or more constrained transfers. When a trip visits
   * the same ScheduledStopPoint multiple times (loop pattern), a transfer is created for each
   * occurrence so the interchange is findable at any stop position during routing.
   */
  public List<ConstrainedTransfer> mapToTransfers(ServiceJourneyInterchange it) {
    var id = it.getId();
    var fromPoints = mapPoints(Label.FROM, id, it.getFromJourneyRef(), it.getFromPointRef());
    var toPoints = mapPoints(Label.TO, id, it.getToJourneyRef(), it.getToPointRef());

    if (fromPoints.isEmpty() || toPoints.isEmpty()) {
      issueStore.add(
        "InvalidInterchange",
        "Interchange %s contains invalid from/to refs. from=%s, to=%s",
        it,
        fromPoints,
        toPoints
      );
      return List.of();
    }

    var c = mapConstraint(it);
    var feedScopedId = idFactory.createId(it.getId());

    // The constraint is the same for all position combinations, so check once
    if (c.isRegularTransfer()) {
      var tx = new ConstrainedTransfer(feedScopedId, fromPoints.getFirst(), toPoints.getFirst(), c);
      issueStore.add(new InterchangeWithoutConstraint(tx));
      return List.of();
    }

    var result = new ArrayList<ConstrainedTransfer>();
    for (var from : fromPoints) {
      for (var to : toPoints) {
        result.add(new ConstrainedTransfer(feedScopedId, from, to, c));
      }
    }
    return result;
  }

  private List<TripTransferPoint> mapPoints(
    Label label,
    String interchangeId,
    VehicleJourneyRefStructure sjRef,
    ScheduledStopPointRefStructure pointRef
  ) {
    if (isInvalid(sjRef)) {
      return List.of();
    }
    var sjId = sjRef.getRef();
    var trip = findTrip(label, "Journey", interchangeId, sjId);
    if (trip == null) {
      return List.of();
    }
    return findAllStopPositions(interchangeId, label, "Point", sjId, pointRef)
      .stream()
      .map(pos -> new TripTransferPoint(trip, pos))
      .toList();
  }

  @Nullable
  private Trip findTrip(Label label, String fieldName, String rootId, String sjId) {
    var tripId = createId(sjId);
    Trip trip = trips.get(tripId);
    return assertRefExist(label.label(fieldName), rootId, sjId, trip) ? trip : null;
  }

  private TransferConstraint mapConstraint(ServiceJourneyInterchange it) {
    var cBuilder = TransferConstraint.of();

    if (it.isStaySeated() != null) {
      cBuilder.staySeated(it.isStaySeated());
    }
    if (it.isGuaranteed() != null) {
      cBuilder.guaranteed(it.isGuaranteed());
    }
    if (it.getPriority() != null) {
      cBuilder.priority(mapPriority(it.getPriority()));
    }
    if (it.getMaximumWaitTime() != null) {
      if (it.isGuaranteed()) {
        cBuilder.maxWaitTime((int) it.getMaximumWaitTime().toSeconds());
      } else {
        // Add a warning here and keep the interchange
        issueStore.add(new InterchangeMaxWaitTimeNotGuaranteed(it));
      }
    }
    return cBuilder.build();
  }

  private TransferPriority mapPriority(Number pri) {
    switch (pri.intValue()) {
      case -1:
        return TransferPriority.NOT_ALLOWED;
      case 0:
        return TransferPriority.ALLOWED;
      case 1:
        return TransferPriority.RECOMMENDED;
      case 2:
        return TransferPriority.PREFERRED;
      default:
        throw new IllegalArgumentException("Interchange priority unknown: " + pri);
    }
  }

  private List<Integer> findAllStopPositions(
    String interchangeId,
    Label label,
    String fieldName,
    String sjId,
    ScheduledStopPointRefStructure scheduledStopPointRef
  ) {
    String sspId = scheduledStopPointRef.getRef();
    var scheduledStopPoints = scheduledStopPointsIndex.get(sjId);
    String errorMessage;

    if (scheduledStopPoints != null) {
      var positions = new ArrayList<Integer>();
      for (int i = 0; i < scheduledStopPoints.size(); i++) {
        if (scheduledStopPoints.get(i).equals(sspId)) {
          positions.add(i);
        }
      }
      if (!positions.isEmpty()) {
        return positions;
      }

      errorMessage = "Scheduled-stop-point-ref not found";
    } else {
      errorMessage = "Service-journey not found";
    }

    issueStore.add(
      new InterchangePointMappingFailed(
        errorMessage,
        interchangeId,
        label.label(fieldName),
        sjId,
        sspId
      )
    );
    return List.of();
  }

  private FeedScopedId createId(String id) {
    return idFactory.createId(id);
  }

  private <T> boolean assertRefExist(
    String fieldName,
    String interchangeId,
    String id,
    T instance
  ) {
    if (instance == null) {
      issueStore.add(new ObjectNotFound("Interchange", interchangeId, fieldName, id));
      return false;
    }
    return true;
  }

  @SwissProfile
  private static boolean isInvalid(VehicleJourneyRefStructure sjRef) {
    return sjRef == null || sjRef.getRef() == null;
  }

  private enum Label {
    FROM("from"),
    TO("to");

    private String label;

    Label(String label) {
      this.label = label;
    }

    String label(String fieldName) {
      return label + fieldName;
    }
  }
}
