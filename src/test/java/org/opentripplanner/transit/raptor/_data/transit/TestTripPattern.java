package org.opentripplanner.transit.raptor._data.transit;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.opentripplanner.transit.raptor.api.transit.GuaranteedTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

public class TestTripPattern implements RaptorTripPattern<TestTripSchedule> {
  public static final byte BOARDING_MASK   = 0b0001;
  public static final byte ALIGHTING_MASK  = 0b0010;
  public static final byte WHEELCHAIR_MASK = 0b0100;

  private final String name;
  private final int[] stopIndexes;

  /**
   * 0 - 000 : No restriction
   * 1 - 001 : No Boarding.
   * 2 - 010 : No Alighting.
   * 4 - 100 : No wheelchair.
   */
  private final int[] restrictions;

  private final Multimap<Integer, GuaranteedTransfer<TestTripSchedule>> transfersFrom;

  private final Multimap<Integer, GuaranteedTransfer<TestTripSchedule>> transfersTo;


  private TestTripPattern(String name, int[] stopIndexes, int[] restrictions) {
    this.name = name;
    this.stopIndexes = stopIndexes;
    this.restrictions = restrictions;
    this.transfersFrom = ArrayListMultimap.create();
    this.transfersTo = ArrayListMultimap.create();
  }

  public static TestTripPattern pattern(String name, int ... stopIndexes) {
    return new TestTripPattern(name, stopIndexes, null);
  }

  /** Create a pattern with name 'R1' and given stop indexes */
  public static TestTripPattern pattern(int ... stopIndexes) {
    return new TestTripPattern("R1", stopIndexes, new int[stopIndexes.length]);
  }

  public void addGuaranteedTransfersFrom(GuaranteedTransfer<TestTripSchedule> tx) {
    transfersFrom.put(tx.getFromStopPos(), tx);
  }

  public void addGuaranteedTransfersTo(GuaranteedTransfer<TestTripSchedule> tx) {
    transfersTo.put(tx.getToStopPos(), tx);
  }

  @Override public int stopIndex(int stopPositionInPattern) {
    return stopIndexes[stopPositionInPattern];
  }

  @Override
  public boolean boardingPossibleAt(int stopPositionInPattern) {
    return isNotRestricted(stopPositionInPattern, BOARDING_MASK);
  }

  @Override
  public boolean alightingPossibleAt(int stopPositionInPattern) {
    return isNotRestricted(stopPositionInPattern, ALIGHTING_MASK);
  }

  @Override
  public int numberOfStopsInPattern() { return stopIndexes.length; }

  @Override
  public Collection<GuaranteedTransfer<TestTripSchedule>> listGuaranteedTransfersFromPattern(
          int stopPos
  ) {
    return transfersFrom.get(stopPos);
  }

  @Override
  public Collection<GuaranteedTransfer<TestTripSchedule>> listGuaranteedTransfersToPattern(
          int stopPos
  ) {
    return transfersTo.get(stopPos);
  }

  @Override
  public String debugInfo() { return "BUS " + name; }

  private boolean isNotRestricted(int index, int mask) {
    return restrictions == null || (restrictions[index] & mask) == 0;
  }
}
