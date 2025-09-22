package org.opentripplanner.ext.flex.template;

import java.util.Collection;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * To perform access/egress/direct flex searches, this module (this package) needs these
 * services. We do not want to inject the implementations here and create unnecessary
 * hard dependencies. By doing this, we explicitly list all external services needed and make
 * testing easier. This also serves as documentation.
 * <p>
 * The implementation of this interface will for the most part just delegate to the implementing
 * OTP service - look in these services for the documentation.
 */
public interface FlexAccessEgressCallbackAdapter {
  /** Adapter, look at implementing service for documentation.  */
  TransitStopVertex getStopVertex(FeedScopedId id);

  /** Adapter, look at implementing service for documentation.  */
  Collection<PathTransfer> getTransfersFromStop(StopLocation stop);

  /** Adapter, look at implementing service for documentation.  */
  Collection<PathTransfer> getTransfersToStop(StopLocation stop);

  /** Adapter, look at implementing service for documentation.  */
  Collection<FlexTrip<?, ?>> getFlexTripsByStop(StopLocation stopLocation);

  /**
   * Return true if date is an active service date for the given trip, and can be used for
   * the given boarding stop position. The implementation should check that the trip is in
   * service for the given date. It should check other restrictions as well, like booking
   * arrangement constraints.
   */
  boolean isDateActive(FlexServiceDate date, FlexTrip<?, ?> trip);
}
