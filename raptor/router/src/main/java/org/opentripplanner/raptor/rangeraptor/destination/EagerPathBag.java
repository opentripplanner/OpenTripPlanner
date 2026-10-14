package org.opentripplanner.raptor.rangeraptor.path;

import java.util.IdentityHashMap;
import java.util.List;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * A bag of paths, one for each {@link DestinationArrival} in the destination pareto-set. The bag
 * is kept in sync with the set by listening to the set events: A path is built when an arrival is
 * accepted, and removed when the arrival is dropped. Hence, the bag never holds a path for an
 * arrival which is not in the set.
 * <p>
 * This is used by StdRaptor, where the stop-arrival state is mutable and overwritten between
 * iterations, so the arrival chain of a destination arrival is not valid after the iteration. The
 * path must be built when the arrival is accepted. McRaptor does not need this, it builds the paths
 * after the search from the arrivals in the set.
 * <p>
 * The only way to access the paths is {@link #toList()}. The bag can be chained with other
 * listeners, like the debug listener, using the
 * {@link org.opentripplanner.raptor.util.paretoset.ParetoSetEventListenerComposite}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class EagerPathBag<T extends RaptorTripSchedule> implements
  ParetoSetEventListener<DestinationArrival<T>> {

  private final IdentityHashMap<DestinationArrival<T>, RaptorPath<T>> paths =
    new IdentityHashMap<>();
  private final PathMapper<T> pathMapper;
  private final RaptorStopNameResolver stopNameResolver;

  EagerPathBag(PathMapper<T> pathMapper, RaptorStopNameResolver stopNameResolver) {
    this.pathMapper = pathMapper;
    this.stopNameResolver = stopNameResolver;
  }

  /**
   * Return the paths of all arrivals in the set, sorted using the natural order of the paths. The
   * order is deterministic, which allows using it "as is" in tests.
   */
  List<RaptorPath<T>> toList() {
    return paths.values().stream().sorted().toList();
  }

  @Override
  public void notifyElementAccepted(DestinationArrival<T> newElement) {
    var path = pathMapper.mapToPath(newElement);
    DestinationArrivalPaths.assertGeneralizedCostIsCalculatedCorrectByMapper(
      newElement,
      path,
      stopNameResolver
    );
    paths.put(newElement, path);
  }

  @Override
  public void notifyElementDropped(
    DestinationArrival<T> element,
    DestinationArrival<T> droppedByElement
  ) {
    paths.remove(element);
  }
}
