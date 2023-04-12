package org.opentripplanner.raptor.api.path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;

class RaptorPathTest {

  private static final int VALUE = 150;
  private static final int SMALL = 100;

  private final APath subject = new APath(VALUE, VALUE, VALUE, VALUE, VALUE, VALUE);
  private final APath same = new APath(VALUE, VALUE, VALUE, VALUE, VALUE, VALUE);
  private final APath smallIterationDepartureTime = new APath(
    SMALL,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE
  );
  private final APath smallDepartureTime = new APath(VALUE, SMALL, VALUE, VALUE, VALUE, VALUE);
  private final APath smallArrivalTime = new APath(VALUE, VALUE, SMALL, VALUE, VALUE, VALUE);
  private final APath smallDuration = new APath(VALUE, VALUE, VALUE, SMALL, VALUE, VALUE);
  private final APath smallNumberOfTransfers = new APath(VALUE, VALUE, VALUE, VALUE, SMALL, VALUE);
  private final APath smallC1 = new APath(VALUE, VALUE, VALUE, VALUE, VALUE, SMALL);

  @Test
  void compareIterationDepartureTime() {
    assertFalse(RaptorPath.compareIterationDepartureTime(subject, subject));
    assertFalse(RaptorPath.compareIterationDepartureTime(subject, same));
    assertTrue(RaptorPath.compareIterationDepartureTime(subject, smallIterationDepartureTime));
    assertFalse(RaptorPath.compareIterationDepartureTime(smallIterationDepartureTime, subject));
  }

  @Test
  void compareDepartureTime() {
    assertFalse(RaptorPath.compareDepartureTime(subject, subject));
    assertFalse(RaptorPath.compareDepartureTime(subject, same));
    assertTrue(RaptorPath.compareDepartureTime(subject, smallDepartureTime));
    assertFalse(RaptorPath.compareDepartureTime(smallDepartureTime, subject));
  }

  @Test
  void compareArrivalTime() {
    assertFalse(RaptorPath.compareArrivalTime(subject, subject));
    assertFalse(RaptorPath.compareArrivalTime(subject, same));
    assertFalse(RaptorPath.compareArrivalTime(subject, smallArrivalTime));
    assertTrue(RaptorPath.compareArrivalTime(smallArrivalTime, subject));
  }

  @Test
  void compareDuration() {
    assertFalse(RaptorPath.compareDuration(subject, subject));
    assertFalse(RaptorPath.compareDuration(subject, same));
    assertFalse(RaptorPath.compareDuration(subject, smallDuration));
    assertTrue(RaptorPath.compareDuration(smallDuration, subject));
  }

  @Test
  void compareNumberOfTransfers() {
    assertFalse(RaptorPath.compareNumberOfTransfers(subject, subject));
    assertFalse(RaptorPath.compareNumberOfTransfers(subject, same));
    assertFalse(RaptorPath.compareNumberOfTransfers(subject, smallNumberOfTransfers));
    assertTrue(RaptorPath.compareNumberOfTransfers(smallNumberOfTransfers, subject));
  }

  @Test
  void compareC1() {
    assertFalse(RaptorPath.compareC1(subject, subject));
    assertFalse(RaptorPath.compareC1(subject, same));
    assertFalse(RaptorPath.compareC1(subject, smallC1));
    assertTrue(RaptorPath.compareC1(smallC1, subject));
    assertTrue(
      RaptorPath.compareC1(GeneralizedCostRelaxFunction.of(1.25, 26), subject, smallArrivalTime)
    );
  }

  private record APath(
    int rangeRaptorIterationDepartureTime,
    int startTime,
    int endTime,
    int durationInSeconds,
    int numberOfTransfers,
    int c1
  )
    implements RaptorPath<TestTripSchedule> {
    @Override
    public int numberOfTransfersExAccessEgress() {
      return -1;
    }

    @Nullable
    @Override
    public AccessPathLeg<TestTripSchedule> accessLeg() {
      return null;
    }

    @Nullable
    @Override
    public EgressPathLeg<TestTripSchedule> egressLeg() {
      return null;
    }

    @Override
    public List<Integer> listStops() {
      return null;
    }

    @Override
    public int waitTime() {
      return 0;
    }

    @Override
    public Stream<PathLeg<TestTripSchedule>> legStream() {
      return null;
    }

    @Override
    public Stream<TransitPathLeg<TestTripSchedule>> transitLegs() {
      return null;
    }

    @Override
    public String toStringDetailed(RaptorStopNameResolver stopNameResolver) {
      return null;
    }

    @Override
    public String toString(RaptorStopNameResolver stopNameTranslator) {
      return null;
    }
  }
}
