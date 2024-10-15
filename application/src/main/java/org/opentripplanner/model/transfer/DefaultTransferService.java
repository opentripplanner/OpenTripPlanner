package org.opentripplanner.model.transfer;

import static java.util.Comparator.comparingInt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This class represents all transfer information in the graph. Transfers are grouped by
 * stop-to-stop pairs.
 * <p>
 * THIS CLASS IS NOT THREAD_SAFE. This class is loaded with plan data only, and read-only during
 * routing. No real-time update should touch this class; Hence it do not need to be thread-safe.
 */
public class DefaultTransferService implements Serializable, TransferService {

  private final List<ConstrainedTransfer> transfersList;

  /**
   * A map of map may seem a bit odd, but the first map have the FROM-transfer-point as its key,
   * while the second map have the TO-transfer-point as its key. This allows us to support all
   * combination of (Trip, Route, Stop and Station) in total 16 possible combination of keys to the
   * ConstrainedTransfer.
   */
  private final TransferPointMap<TransferPointMap<ConstrainedTransfer>> transfersMap;

  public DefaultTransferService() {
    this.transfersList = new ArrayList<>();
    this.transfersMap = new TransferPointMap<>();
  }

  public void addAll(Collection<ConstrainedTransfer> transfers) {
    Set<ConstrainedTransfer> set = new HashSet<>(transfersList);

    for (ConstrainedTransfer transfer : transfers) {
      if (!set.contains(transfer)) {
        add(transfer);
        set.add(transfer);
      }
    }
  }

  @Override
  public List<ConstrainedTransfer> listAll() {
    return transfersList;
  }

  @Override
  @Nullable
  public ConstrainedTransfer findTransfer(
    Trip fromTrip,
    int fromStopPosition,
    StopLocation fromStop,
    Trip toTrip,
    int toStopPosition,
    StopLocation toStop
  ) {
    return transfersMap
      .get(fromTrip, fromStop, fromStopPosition)
      .stream()
      .map(map2 -> map2.get(toTrip, toStop, toStopPosition))
      .flatMap(Collection::stream)
      .max(comparingInt(ConstrainedTransfer::getSpecificityRanking))
      .orElse(null);
  }

  private void add(ConstrainedTransfer transfer) {
    var from = transfer.getFrom();
    var to = transfer.getTo();

    transfersMap.computeIfAbsent(from, TransferPointMap::new).put(to, transfer);
    transfersList.add(transfer);
  }
}
