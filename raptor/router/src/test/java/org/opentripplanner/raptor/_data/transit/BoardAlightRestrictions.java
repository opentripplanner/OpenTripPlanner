package org.opentripplanner.raptor._data.transit;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BoardAlightRestrictions {

  private static final EnumSet<BoardAlightRestriction> BOARDING_ONLY = EnumSet.of(
    BoardAlightRestriction.BOARDING
  );
  private static final EnumSet<BoardAlightRestriction> ALIGHTING_ONLY = EnumSet.of(
    BoardAlightRestriction.ALIGHTING
  );
  private static final EnumSet<BoardAlightRestriction> ALL_ALLOWED = EnumSet.allOf(
    BoardAlightRestriction.class
  );
  private static final EnumSet<BoardAlightRestriction> NONE_ALLOWED = EnumSet.noneOf(
    BoardAlightRestriction.class
  );

  private final List<EnumSet<BoardAlightRestriction>> restrictions;

  private BoardAlightRestrictions(List<EnumSet<BoardAlightRestriction>> restrictions) {
    if (restrictions.isEmpty()) {
      throw new IllegalArgumentException("At least one stop is required.");
    }
    this.restrictions = restrictions;
  }

  public static BoardAlightRestrictions noRestriction(int size) {
    var list = new ArrayList<EnumSet<BoardAlightRestriction>>(size);
    for (int i = 0; i < size; i++) {
      list.add(ALL_ALLOWED);
    }
    return new BoardAlightRestrictions(List.copyOf(list));
  }

  /**
   * Set alight and board restriction using a "coded" string, use space as a separator
   * between stops.
   * <pre>
   * Codes:
   *   b : Board
   *   a : Alight
   *   * : Board & Alight
   *   - : Boarding & Alighting is not allowed
   *
   * Example:   B BA * A
   * </pre>
   */
  public static BoardAlightRestrictions restrictions(String restrictions) {
    var codes = restrictions.toLowerCase().trim().split("\\s");
    var list = new ArrayList<EnumSet<BoardAlightRestriction>>();
    for (String code : codes) {
      if ("a".equals(code)) {
        list.add(ALIGHTING_ONLY);
      } else if ("b".equals(code)) {
        list.add(BOARDING_ONLY);
      } else if (code.matches("ab|ba|\\*")) {
        list.add(ALL_ALLOWED);
      } else if ("-".equals(code)) {
        list.add(NONE_ALLOWED);
      }
    }
    return new BoardAlightRestrictions(list);
  }

  public boolean isBoardingPossibleAt(int stopPositionInPattern) {
    return restrictions.get(stopPositionInPattern).contains(BoardAlightRestriction.BOARDING);
  }

  public boolean isAlightingPossibleAt(int stopPositionInPattern) {
    return restrictions.get(stopPositionInPattern).contains(BoardAlightRestriction.ALIGHTING);
  }

  @Override
  public String toString() {
    var buf = new StringBuilder();
    for (int i = 0; i < restrictions.size(); ++i) {
      if (isAlightingPossibleAt(i) && isBoardingPossibleAt(i)) {
        buf.append(" *");
      } else if (isAlightingPossibleAt(i)) {
        buf.append(" A");
      } else if (isBoardingPossibleAt(i)) {
        buf.append(" B");
      } else {
        buf.append(" Ø");
      }
    }
    return buf.substring(1);
  }

  private enum BoardAlightRestriction {
    BOARDING,
    ALIGHTING,
  }
}
