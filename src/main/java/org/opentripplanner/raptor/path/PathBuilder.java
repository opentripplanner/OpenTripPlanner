package org.opentripplanner.raptor.path;

import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;

/**
 * The path builder is a utility to build paths. The path builder is responsible for reconstructing
 * information that was used in decision-making inside Raptor, but not kept due to performance
 * reasons. For example information about the transfer like transfer constraints.
 * <p>
 * The path builder enforces the same logic as Raptor and generates information like the
 * generalized-cost instead of getting it from the stop-arrivals. This is convenient if a path is
 * created OUTSIDE Raptor, which is the case in the {@link
 * org.opentripplanner.routing.algorithm.transferoptimization.OptimizeTransferService}.
 * <p>
 * The path builder comes in two versions. One which adds new legs to the tail of the path, allowing
 * us to add legs starting with the access leg and ending with the egress leg. The other adds legs
 * in the opposite order, from egress to access. Hence, the forward and reverse mappers are
 * simplified using the head and tail builder respectively. See {@link #headPathBuilder(
 * RaptorSlackProvider, int, RaptorCostCalculator, RaptorStopNameResolver,
 * RaptorPathConstrainedTransferSearch)} and {@link #tailPathBuilder(RaptorSlackProvider, int,
 * RaptorCostCalculator, RaptorStopNameResolver, RaptorPathConstrainedTransferSearch)}.
 * <p>
 * The builder is also used for creating test data in unit test.
 * <p>
 * The {@code PathBuilder} can be extended to override specific things. The {@link
 * org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail} does this to
 * be able to create {@link org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath}
 * instead of regular {@link RaptorPath} objects.
 */
public abstract class PathBuilder<T extends RaptorTripSchedule> {

  private final RaptorSlackProvider slackProvider;

  @Nullable
  private final RaptorCostCalculator<T> costCalculator;

  @Nullable
  private final RaptorStopNameResolver stopNameResolver;

  @Nullable
  private final RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch;

  private int c2 = RaptorConstants.NOT_SET;

  // Path leg elements as a double linked list. This makes it easy to look at
  // legs before and after in the logic and easy to fork, building alternative
  // paths with the same path tail.
  private PathBuilderLeg<T> head = null;
  private PathBuilderLeg<T> tail = null;
  protected final int iterationDepartureTime;

  protected PathBuilder(PathBuilder<T> other) {
    this(
      other.slackProvider,
      other.iterationDepartureTime,
      other.costCalculator,
      other.stopNameResolver,
      other.transferConstraintsSearch
    );
    this.head = other.head == null ? null : other.head.mutate();
    this.tail = this.head == null ? null : last(this.head);
  }

  protected PathBuilder(
    RaptorSlackProvider slackProvider,
    int iterationDepartureTime,
    @Nullable RaptorCostCalculator<T> costCalculator,
    @Nullable RaptorStopNameResolver stopNameResolver,
    @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch
  ) {
    this.slackProvider = slackProvider;
    this.costCalculator = costCalculator;
    this.stopNameResolver = stopNameResolver;
    this.transferConstraintsSearch = transferConstraintsSearch;
    this.iterationDepartureTime = iterationDepartureTime;
  }

  /**
   * Create a new path builder to build path starting from the access and add elements in forward
   * order until the last egress leg is added.
   * <p>
   * This builder inserts transferConstraints, time-shifts access/transfers/egress and calculates
   * generalized-cost in the build phase. (Insert new tail)
   */
  public static <T extends RaptorTripSchedule> PathBuilder<T> headPathBuilder(
    RaptorSlackProvider slackProvider,
    int iterationDepartureTime,
    @Nullable RaptorCostCalculator<T> costCalculator,
    @Nullable RaptorStopNameResolver stopNameResolver,
    @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch
  ) {
    return new HeadPathBuilder<>(
      slackProvider,
      costCalculator,
      iterationDepartureTime,
      stopNameResolver,
      transferConstraintsSearch
    );
  }

  /**
   * Create a new path builder to build path starting from the egress and add elements in reverse
   * order until the access leg is added last. (Insert new head)
   * <p>
   * This builder inserts transferConstraints, time-shifts access/transfers/egress and calculates
   * generalized-cost in the build phase.
   */
  public static <T extends RaptorTripSchedule> PathBuilder<T> tailPathBuilder(
    RaptorSlackProvider slackProvider,
    int iterationDepartureTime,
    @Nullable RaptorCostCalculator<T> costCalculator,
    @Nullable RaptorStopNameResolver stopNameResolver,
    @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch
  ) {
    return new TailPathBuilder<>(
      slackProvider,
      costCalculator,
      iterationDepartureTime,
      stopNameResolver,
      transferConstraintsSearch
    );
  }

  protected RaptorSlackProvider slackProvider() {
    return slackProvider;
  }

  @Nullable
  protected RaptorCostCalculator<T> costCalculator() {
    return costCalculator;
  }

  @Nullable
  protected RaptorStopNameResolver stopNameResolver() {
    return stopNameResolver;
  }

  public void access(RaptorAccessEgress access) {
    add(PathBuilderLeg.accessLeg(access));
  }

  public void transit(T trip, BoardAndAlightTime times) {
    add(PathBuilderLeg.transitLeg(trip, times));
  }

  public void transit(
    T trip,
    BoardAndAlightTime times,
    RaptorConstrainedTransfer txConstrainedTransferAfter
  ) {
    add(PathBuilderLeg.transitLeg(trip, times, txConstrainedTransferAfter));
  }

  public void transfer(RaptorTransfer transfer, int toStop) {
    add(PathBuilderLeg.transferLeg(transfer, toStop));
  }

  public void egress(RaptorAccessEgress egress) {
    add(PathBuilderLeg.egress(egress));
  }

  public void c2(int c2) {
    this.c2 = c2;
  }

  public int c2() {
    return tail.isC2Set() ? tail.c2() : c2;
  }

  public boolean isC2Set() {
    return tail.isC2Set() || c2 != RaptorConstants.NOT_SET;
  }

  public RaptorPath<T> build() {
    updateAggregatedFields();
    var pathLegs = createPathLegs(costCalculator, slackProvider);
    return new Path<>(iterationDepartureTime, pathLegs, pathLegs.c1Total(), c2());
  }

  @Override
  public String toString() {
    final PathStringBuilder builder = new PathStringBuilder(stopNameResolver);
    legsAsStream().forEach(it -> it.toString(builder));
    return builder.toString();
  }

  public Stream<PathBuilderLeg<T>> legsAsStream() {
    return Stream.iterate(head, Objects::nonNull, PathBuilderLeg::next);
  }

  public PathBuilderLeg<T> head() {
    return head;
  }

  public PathBuilderLeg<T> tail() {
    return tail;
  }

  /* package local methods, accessible by private sub-classes */

  protected abstract void add(PathBuilderLeg<T> newLeg);

  protected void updateAggregatedFields() {
    timeShiftAllStreetLegs();
    insertConstrainedTransfers();
  }

  protected void addTail(PathBuilderLeg<T> newLeg) {
    if (head == null) {
      head = tail = newLeg;
    } else {
      tail.setNext(newLeg);
      newLeg.setPrev(tail);
      tail = newLeg;
    }
  }

  protected void addHead(PathBuilderLeg<T> newLeg) {
    if (head == null) {
      head = tail = newLeg;
    } else {
      newLeg.setNext(head);
      head.setPrev(newLeg);
      head = newLeg;
    }
  }

  /* private methods */

  protected AccessPathLeg<T> createPathLegs(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    return head.createAccessPathLeg(costCalculator, slackProvider);
  }

  protected boolean skipCostCalc() {
    return costCalculator == null;
  }

  private void timeShiftAllStreetLegs() {
    legsAsStream()
      .forEach(leg -> leg.timeShiftThisAndNextLeg(slackProvider, iterationDepartureTime));
  }

  private void insertConstrainedTransfers() {
    // If constrained transfer is not in use
    if (transferConstraintsSearch == null) {
      return;
    }

    var prev = head.nextTransitLeg();
    if (prev == null) {
      return;
    }
    var curr = prev.nextTransitLeg();

    while (curr != null) {
      addTransferConstraints(prev, curr);
      prev = curr;
      curr = curr.nextTransitLeg();
    }
  }

  private void addTransferConstraints(PathBuilderLeg<T> from, PathBuilderLeg<T> to) {
    @SuppressWarnings("ConstantConditions")
    var tx = transferConstraintsSearch.findConstrainedTransfer(
      from.trip(),
      from.toStopPos(),
      to.trip(),
      to.fromStopPos()
    );
    if (tx != null) {
      from.setConstrainedTransferAfterLeg(tx);
    }
  }

  private PathBuilderLeg<T> last(PathBuilderLeg<T> head) {
    return head.next() == null ? head : last(head.next());
  }

  private static class HeadPathBuilder<T extends RaptorTripSchedule> extends PathBuilder<T> {

    private HeadPathBuilder(
      RaptorSlackProvider slackProvider,
      RaptorCostCalculator<T> costCalculator,
      int iterationDepartureTime,
      @Nullable RaptorStopNameResolver stopNameResolver,
      @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch
    ) {
      super(
        slackProvider,
        iterationDepartureTime,
        costCalculator,
        stopNameResolver,
        transferConstraintsSearch
      );
    }

    @Override
    protected void add(PathBuilderLeg<T> newLeg) {
      addHead(newLeg);
    }
  }

  private static class TailPathBuilder<T extends RaptorTripSchedule> extends PathBuilder<T> {

    private TailPathBuilder(
      RaptorSlackProvider slackProvider,
      RaptorCostCalculator<T> costCalculator,
      int iterationDepartureTime,
      @Nullable RaptorStopNameResolver stopNameResolver,
      @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch
    ) {
      super(
        slackProvider,
        iterationDepartureTime,
        costCalculator,
        stopNameResolver,
        transferConstraintsSearch
      );
    }

    @Override
    protected void add(PathBuilderLeg<T> newLeg) {
      addTail(newLeg);
    }
  }
}
