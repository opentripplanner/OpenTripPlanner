package org.opentripplanner.raptor.rangeraptor;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.internalapi.RangeRaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.util.composite.CompositeUtil;

/**
 * Iterate over two RR workers. The head should process the access and the tail should produce the
 * result. Paths from the head needs to propagate to the tail - this is NOT part of the
 * responsibilities for this class.
 */
public class RangeRaptorWorkerComposite<T extends RaptorTripSchedule>
  implements RangeRaptorWorker<T> {

  private final List<RangeRaptorWorker<T>> children;

  private RangeRaptorWorkerComposite(Collection<RangeRaptorWorker<T>> children) {
    this.children = List.copyOf(children);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends RaptorTripSchedule> RangeRaptorWorker<T> of(
    @Nullable RangeRaptorWorker<T> head,
    @Nullable RangeRaptorWorker<T> tail
  ) {
    return CompositeUtil.of(
      RangeRaptorWorkerComposite::new,
      it -> it instanceof RangeRaptorWorkerComposite<T>,
      it -> ((RangeRaptorWorkerComposite) it).children,
      head,
      tail
    );
  }

  @Override
  public RaptorWorkerResult<T> results() {
    return tail().results();
  }

  @Override
  public boolean hasMoreRounds() {
    return children.stream().anyMatch(RangeRaptorWorker::hasMoreRounds);
  }

  @Override
  public void findTransitForRound() {
    for (RangeRaptorWorker<T> child : children) {
      child.findTransitForRound();
    }
  }

  @Override
  public void findTransfersForRound() {
    for (RangeRaptorWorker<T> child : children) {
      child.findTransfersForRound();
    }
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    return tail().isDestinationReachedInCurrentRound();
  }

  @Override
  public void findAccessOnStreetForRound() {
    head().findAccessOnStreetForRound();
  }

  @Override
  public void findAccessOnBoardForRound() {
    head().findAccessOnBoardForRound();
  }

  private RangeRaptorWorker<T> head() {
    return children.getFirst();
  }

  private RangeRaptorWorker<T> tail() {
    return children.getLast();
  }
}
