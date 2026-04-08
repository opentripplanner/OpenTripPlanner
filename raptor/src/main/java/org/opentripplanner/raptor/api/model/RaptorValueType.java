package org.opentripplanner.raptor.api.model;

/// Types for printing primitive values used with Raptor. The prefixes should be easy to
/// read and easy to remember. The suffix subscripts represent:
/// - ₁ & ₂ : Is used to index Cost 1 & 2
/// - ₜ : Time related
/// - ₙ : Total number of, count
/// - ₚ : Priority
///
/// `Wₜ` is **W**AIT_TIME_COST and `ₜ` because it is time related,
/// Tₙ is the number of **T**ransfers
public enum RaptorValueType {
  // Cost 1 or "generalized cost"
  C1("C₁"),
  // Cost 2, used for several diffrent purpouses
  C2("C₂"),
  // Wait time cost, used in optimized transfers
  WAIT_TIME_COST("Wₜ"),
  /// Time penalty
  TIME_PENALTY("Pₜ"),
  /// Number of transfers
  TRANSFERS("Tₙ"),
  /// Transfer priority
  TRANSFER_PRIORITY("Tₚ"),
  /// Number of rides
  RIDES("Rₙ"),
  /// Number of via locations visited
  VIAS("Vₙ");

  private final String prefix;

  RaptorValueType(String prefix) {
    this.prefix = prefix;
  }

  public String prefix() {
    return prefix;
  }

  public String format(int num) {
    return switch (this) {
      case C1, WAIT_TIME_COST -> prefix + RaptorValueFormatter.formatCenti(num);
      default -> prefix + RaptorValueFormatter.format(num);
    };
  }

  public int parseValue(String value) {
    String v = value.substring(prefix.length());
    return switch (this) {
      case C1, WAIT_TIME_COST -> RaptorValueFormatter.parseCenti(v);
      default -> RaptorValueFormatter.parse(v);
    };
  }
}
