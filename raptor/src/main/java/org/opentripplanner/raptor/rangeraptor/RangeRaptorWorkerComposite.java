package org.opentripplanner.raptor.rangeraptor;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.support.RouterResultPathAggregator;
import org.opentripplanner.raptor.util.composite.CompositeUtil;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * Iterates over a list of {@link RangeRaptorWorker} children. Each * {@code RangeRaptorWorker}
 * method is called in order for all children. The final results are collected from all children
 * and merged into a single {@link RaptorRouterResult}. Propagating state from one worker to
 * another is deliberately kept outside the responsibility of this class.
 * <p>
 * This is used in via searches where each segment in the via search has its own
 * worker. See
 * {@link org.opentripplanner.raptor.rangeraptor.context.SearchContextViaSegments}
 * for more information.
 */
public class RangeRaptorWorkerComposite<T extends RaptorTripSchedule>
  implements RangeRaptorWorker<T> {

  private final List<RangeRaptorWorker<T>> children;
  private final ParetoComparator<RaptorPath<T>> comparator;
  private RaptorRouterResult<T> result = null;

  public RangeRaptorWorkerComposite(
    List<RangeRaptorWorker<T>> children,
    ParetoComparator<RaptorPath<T>> comparator
  ) {
    this.children = children;
    this.comparator = comparator;
  }

  /**
   * Concatenate the two given workers, flattening any composite workers into a list.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends RaptorTripSchedule> RangeRaptorWorker<T> of(
    ParetoComparator<RaptorPath<T>> comparator,
    @Nullable RangeRaptorWorker<T> a,
    @Nullable RangeRaptorWorker<T> b
  ) {
    return CompositeUtil.of(
      children -> new RangeRaptorWorkerComposite(children, comparator),
      it -> it instanceof RangeRaptorWorkerComposite<T>,
      it -> ((RangeRaptorWorkerComposite) it).children,
      a,
      b
    );
  }

  @Override
  public boolean hasMoreRounds() {
    for (RangeRaptorWorker<T> child : children) {
      if (child.hasMoreRounds()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void applyStreetStopAccess() {
    for (RangeRaptorWorker<T> child : children) {
      child.applyStreetStopAccess();
    }
  }

  @Override
  public void applyOnBoardStopAccess() {
    for (RangeRaptorWorker<T> child : children) {
      child.applyOnBoardStopAccess();
    }
  }

  @Override
  public void applyOnBoardTripAccess(int iterationDepartureTime) {
    for (RangeRaptorWorker<T> child : children) {
      child.applyOnBoardTripAccess(iterationDepartureTime);
    }
  }

  @Override
  public void routeTransit() {
    for (RangeRaptorWorker<T> child : children) {
      child.routeTransit();
    }
  }

  @Override
  public void routeTransitUsingOnBoardTripAccess() {
    for (RangeRaptorWorker<T> child : children) {
      child.routeTransitUsingOnBoardTripAccess();
    }
  }

  @Override
  public void applyTransfers() {
    for (RangeRaptorWorker<T> child : children) {
      child.applyTransfers();
    }
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    for (RangeRaptorWorker<T> child : children) {
      if (child.isDestinationReachedInCurrentRound()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public RaptorRouterResult<T> result() {
    if (result == null) {
      this.result = new RouterResultPathAggregator(
        children.stream().map(RangeRaptorWorker::result).toList(),
        comparator
      );
    }
    return result;
  }
}
