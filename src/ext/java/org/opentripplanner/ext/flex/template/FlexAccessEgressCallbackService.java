package org.opentripplanner.ext.flex.template;

import java.util.Collection;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public interface FlexAccessEgressCallbackService {
  TransitStopVertex getStopVertexForStopId(FeedScopedId id);
  Collection<PathTransfer> getTransfersFromStop(StopLocation stop);
  Collection<PathTransfer> getTransfersToStop(StopLocation stop);
  Collection<FlexTrip<?, ?>> getFlexTripsByStop(StopLocation stopLocation);

  /**
   * Return true if date is an active service date for the given trip, and can be used for
   * the given boarding stop position. The implementation should check that the trip is in
   * service for the given date. It should check other restrictions as well, like booking
   * arrangement constraints.
   */
  boolean isDateActive(FlexServiceDate date, FlexTrip<?, ?> trip);
}
