package org.opentripplanner.raptor.util;

import java.util.Comparator;
import java.util.function.IntFunction;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * The responsibility of this class is to compare two int arrays and list all elements that differ.
 * You may provide a list of indexes to compare or compare all elements.
 * <p/>
 * The result is returned as a multi-line string.
 * <p/>
 * If the header line exceeds 2000 characters and you compare all elements the comparison is
 * aborted.
 * <p/>
 * Both regular numbers and time is supported. The time uses the {@link
 * TimeUtils#timeToStrCompact(int)} to print all times.
 */
public class CompareIntArrays {

  private final String label;
  private final String aName;
  private final String bName;
  private final int unreached;
  private final IntFunction<String> mapValue;
  private final Comparator<Integer> comparator;

  private String hh = "";
  private String aValues = "";
  private String bValues = "";
  private int aLess = 0;
  private int bLess = 0;
  private int aNotReached = 0;
  private int bNotReached = 0;

  private CompareIntArrays(
    String label,
    String aName,
    String bName,
    int unreached,
    IntFunction<String> mapValue,
    Comparator<Integer> comparator
  ) {
    this.label = label;
    this.aName = aName;
    this.bName = bName;
    this.unreached = unreached;
    this.mapValue = mapValue;
    this.comparator = comparator;
  }

  public static String compareTime(
    String label,
    String aName,
    int[] a,
    String bName,
    int[] b,
    int unreached,
    int[] stops,
    Comparator<Integer> comparator
  ) {
    return compare(
      label,
      aName,
      a,
      bName,
      b,
      TimeUtils::timeToStrCompact,
      comparator,
      unreached,
      stops
    );
  }

  public static String compare(
    String label,
    String aName,
    int[] a,
    String bName,
    int[] b,
    int unreached,
    int[] stops,
    Comparator<Integer> comparator
  ) {
    return compare(label, aName, a, bName, b, Integer::toString, comparator, unreached, stops);
  }

  private static String compare(
    String label,
    String aName,
    int[] a,
    String bName,
    int[] b,
    IntFunction<String> mapValue,
    Comparator<Integer> comparator,
    int unreached,
    int[] stops
  ) {
    CompareIntArrays s = new CompareIntArrays(label, aName, bName, unreached, mapValue, comparator);
    int size;

    if (stops != null && stops.length > 0) {
      size = stops.length;
      s.compare(a, b, stops);
    } else {
      size = a.length;
      s.compareAll(a, b);
    }
    return s.result(size);
  }

  private void compare(int[] a, int[] b, int[] index) {
    for (int i : index) {
      addResult(i, a[i], b[i]);
      countDiff(a[i], b[i]);
    }
  }

  private void compareAll(int[] a, int[] b) {
    for (int i = 0; i < a.length; ++i) {
      int u = a[i];
      int v = b[i];

      if (u != v) {
        if (hh.length() < 2000) {
          addResult(i, u, v);
        }
        countDiff(a[i], b[i]);
      }
    }
    if (hh.length() == 0) {
      hh += "ALL STOPS";
      aValues += "  ARE";
      bValues += " EQUAL";
    }
  }

  private void countDiff(int a, int b) {
    if (a == unreached) aNotReached++;
    else if (b == unreached) bNotReached++;
    else {
      int c = comparator.compare(a, b);
      if (c < 0) aLess++;
      else if (c > 0) bLess++;
    }
  }

  private void addResult(int i, int u, int v) {
    hh += String.format("%8d ", i);
    aValues += String.format("%8s ", toStr(u));
    bValues += String.format("%8s ", toStr(v));
  }

  private String toStr(int v) {
    return v == unreached ? "" : mapValue.apply(v);
  }

  private String result(int size) {
    int w = Math.max(4, Math.max(aName.length(), bName.length()));
    String f = "%-" + w + "s  %s%n";
    String result = label + '\n';
    result += String.format(f, "Stop", hh);
    result += String.format(f, aName, aValues);
    result += String.format(f, bName, bValues);
    if (diffTot() != 0) {
      result += String.format(
        "Number of diffs: %d of %d, %s better: %d and not reached: %d, %s better: %d and not reached: %d.%n",
        diffTot(),
        size,
        aName,
        aLess,
        aNotReached,
        bName,
        bLess,
        bNotReached
      );
    }
    return result;
  }

  private int diffTot() {
    return aNotReached + aLess + bNotReached + bLess;
  }
}
