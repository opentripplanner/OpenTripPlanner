package org.opentripplanner.model.transfer;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public interface TransferService {
  List<ConstrainedTransfer> listAll();

  @Nullable
  ConstrainedTransfer findTransfer(
    Trip fromTrip,
    int fromStopPosition,
    StopLocation fromStop,
    Trip toTrip,
    int toStopPosition,
    StopLocation toStop
  );
}
