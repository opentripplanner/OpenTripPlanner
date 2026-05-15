package org.opentripplanner.raptor.util.paretoset;

/// Pareto Dominance can have 4 values when comparing two values `x`, `y`. The 4 values are
/// mutually exclusive. Remember x and y can be vectors with one or more criteria.
public enum ParetoDominance {
  /// Left-Dominance (`x` ≺ `y`), when `x` is strictly better than`y`. `[1, 1, 3]` ≺ `[1, 7, 3]`.
  LEFT('≺'),
  /// Right-Dominance (`x` ≻ `y`), when `y` is strictly better than`x`. `[1, 7, 3]` ≻ `[1, 1, 3]`.
  RIGHT('≻'),
  /// Mutual-Dominance (Incomparable/Indifferent)  (`x` ∥ `y`). Neither solution is superior to the
  /// other. Both solutions are part of the Pareto optimal set (Pareto front). This happens when
  /// the `x` is better in one criteria, and `y` is better in another: `[1, 7, 3]` ∥ `[7, 1, 3]`.
  MUTUAL('∥'),
  /// No Dominate (Strictly equal)  (`x` ≡ `y`). Neither `x` dominates `y` nor `y` dominates
  /// `x`, AND `x` and `y` are equal in all objective values: `[1, 7, 3]` ≡ `[1, 7, 3]`.
  NONE('≡');

  private final char symbol;

  ParetoDominance(char symbol) {
    this.symbol = symbol;
  }

  /// Create a dominance value from two directional dominance flags. For `x`(left) and `y`(right):
  /// @param leftDominanceExist `x` has at least one criteria that is better than `y`.
  /// @param rightDominanceExist `y` has at least one criteria that is better than `x`.
  public static ParetoDominance of(boolean leftDominanceExist, boolean rightDominanceExist) {
    if (leftDominanceExist) {
      return rightDominanceExist ? MUTUAL : LEFT;
    } else {
      return rightDominanceExist ? RIGHT : NONE;
    }
  }

  /**
   * Parse a dominance value from either its symbol or enum name.
   *
   * <p>Accepted symbols are `≺`, `≻`, `∥`, and `≡`. If the input is not a single-character symbol,
   * the value is parsed as an enum name (case-insensitive).
   */
  public static ParetoDominance of(String value) {
    if (value.length() == 1) {
      char ch = value.charAt(0);
      for (var it : values()) {
        if (it.symbol == ch) {
          return it;
        }
      }
    }
    return valueOf(value.toUpperCase());
  }

  public char symbol() {
    return symbol;
  }

  /**
   * Return the symbolic representation of this dominance value.
   */
  @Override
  public String toString() {
    return name() + " " + symbol;
  }
}
