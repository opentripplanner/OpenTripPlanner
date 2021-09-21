package org.opentripplanner.transit.raptor.api.path;

import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.BoardAndAlightTime;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;


/**
 * The path builder is a utility to build paths. The path builder is responsible for reconstructing
 * information that was used in decision making inside Raptor, but not kept due to performance
 * reasons. For example information about the transfer like transfer constraints.
 * <p>
 * The path builder enforce the same logic as Raptor and generate information like the
 * generalized-cost instead of getting it from the stop-arrivals. This is convenient if a path is
 * created OUTSIDE Raptor, witch is the case in the {@link org.opentripplanner.routing.algorithm.transferoptimization.OptimizeTransferService}.
 * <p>
 * The path builder comes in two versions. One witch add new legs to the tail of the path
 * allowing us to add legs starting with access leg and ending with the egress leg. The other
 * add legs in the opposite order, from egress to access. Hence the forward and reverse mapper
 * is simplified using the head and tail builder respectively.
 * See {@link #headPathBuilder(RaptorPathConstrainedTransferSearch, RaptorSlackProvider, CostCalculator)}
 * and {@link #tailPathBuilder(RaptorPathConstrainedTransferSearch, RaptorSlackProvider, CostCalculator)}
 * <p>
 * The builder is also used for creating test data in unit test.
 * <p>
 * The {@code PathBuilder} can be extended to override specific things. The {@link org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail} does this to be able to create
 * {@link org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath} instead of regular {@link Path} objects.
 */
public abstract class PathBuilder<T extends RaptorTripSchedule> {

  @Nullable
  private final RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch;
  protected final RaptorSlackProvider slackProvider;
  @Nullable
  protected final CostCalculator costCalculator;


  // Path leg elements as a double linked list, this make it easy to look at
  // legs before and after in the logic and easy to fork, building alternative
  // paths with the same path tail.
  private PathBuilderLeg<T> head = null;
  private PathBuilderLeg<T> tail = null;

  protected PathBuilder(PathBuilder<T> other) {
    this.transferConstraintsSearch = other.transferConstraintsSearch;
    this.slackProvider = other.slackProvider;
    this.costCalculator = other.costCalculator;
    this.head = other.head == null ? null : other.head.mutate();
    this.tail = this.head == null ? null : last(this.head);
  }

  protected PathBuilder(
          @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
          RaptorSlackProvider slackProvider,
          @Nullable CostCalculator costCalculator
  ) {
    this.transferConstraintsSearch = transferConstraintsSearch;
    this.slackProvider = slackProvider;
    this.costCalculator = costCalculator;
  }

  /**
   * Create a new path builder to build path starting from the access and add elements
   * in forward order until the last egress leg is added.
   * <p>
   * This builder insert transferConstraints, time-shift access/transfers/egress and
   * calculate generalized-cost in the build phase. (Insert new tail)
   */
  public static <T extends RaptorTripSchedule> PathBuilder<T> headPathBuilder(
          @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
          RaptorSlackProvider slackProvider,
          @Nullable CostCalculator costCalculator
  ) {
    return new HeadPathBuilder<>(transferConstraintsSearch, slackProvider, costCalculator);
  }

  /**
   * Create a new path builder to build path starting from the egress and add elements
   * in reverse order until the access leg is added last. (Insert new head)
   * <p>
   * This builder insert transferConstraints, time-shift access/transfers/egress and
   * calculate generalized-cost in the build phase.
   */
  public static <T extends RaptorTripSchedule> PathBuilder<T> tailPathBuilder(
          RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
          RaptorSlackProvider slackProvider,
          @Nullable CostCalculator costCalculator
  ) {
    return new TailPathBuilder<>(transferConstraintsSearch, slackProvider, costCalculator);
  }

  public void access(RaptorTransfer access) {
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

  public void egress(RaptorTransfer egress) {
    add(PathBuilderLeg.egress(egress));
  }

  public Path<T> build(int iterationDepartureTime) {
    updateAggregatedFields();
    return new Path<>(iterationDepartureTime, createPathLegs(costCalculator, slackProvider));
  }

  @Override
  public String toString() {
    final PathStringBuilder builder = new PathStringBuilder(null);
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
    if(head == null) {
      head = tail = newLeg;
    }
    else {
      tail.setNext(newLeg);
      newLeg.setPrev(tail);
      tail = newLeg;
    }
  }

  protected void addHead(PathBuilderLeg<T> newLeg) {
    if(head == null) {
      head = tail = newLeg;
    }
    else {
      newLeg.setNext(head);
      head.setPrev(newLeg);
      head = newLeg;
    }
  }


  /* private methods */

  private void timeShiftAllStreetLegs() {
    legsAsStream().forEach(leg -> leg.timeShiftThisAndNextLeg(slackProvider));
  }

  private void insertConstrainedTransfers() {
    // If constrained transfer is not in use
    if(transferConstraintsSearch == null) { return; }

    var prev = head.nextTransitLeg();
    if(prev == null) { return; }
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
            from.trip(), from.toStopPos(), to.trip(), to.fromStopPos()
    );
    if(tx != null) { from.setConstrainedTransferAfterLeg(tx); }
  }

  protected AccessPathLeg<T> createPathLegs(
          CostCalculator costCalculator,
          RaptorSlackProvider slackProvider
  ) {
    return head.createAccessPathLeg(costCalculator, slackProvider);
  }

  private PathBuilderLeg<T> last(PathBuilderLeg<T> head) {
    return head.next() == null ? head : last(head.next());
  }

  protected boolean skipCostCalc() {
    return costCalculator == null;
  }

  private static class HeadPathBuilder<T extends RaptorTripSchedule> extends PathBuilder<T> {

    private HeadPathBuilder(
            @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
            RaptorSlackProvider slackProvider,
            CostCalculator costCalculator
    ) {
      super(transferConstraintsSearch, slackProvider, costCalculator);
    }

    @Override protected void add(PathBuilderLeg<T> newLeg) { addHead(newLeg); }
  }


  private static class TailPathBuilder<T extends RaptorTripSchedule> extends PathBuilder<T> {

    private TailPathBuilder(
            @Nullable RaptorPathConstrainedTransferSearch<T> transferConstraintsSearch,
            RaptorSlackProvider slackProvider,
            CostCalculator costCalculator
    ) {
      super(transferConstraintsSearch, slackProvider, costCalculator);
    }

    @Override protected void add(PathBuilderLeg<T> newLeg) { addTail(newLeg); }
  }
}
