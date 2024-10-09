package org.opentripplanner.transit.model.network.grouppriority;

/**
 * Used to concatenate matches with either the logical "AND" or "OR" operator.
 */
enum BinarySetOperator {
  AND("&"),
  OR("|");

  private final String token;

  BinarySetOperator(String token) {
    this.token = token;
  }

  @Override
  public String toString() {
    return token;
  }
}
