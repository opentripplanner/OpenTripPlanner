package org.opentripplanner.routing.util;

public record DiffEntry<T>(T left, T right) {
  /**
   * Return the left instance if it exist, if not return the right instance. If both exist(left
   * equals right) then left is returned.
   */
  public T element() {
    return rightOnly() ? right : left;
  }

  /* The element exist in the left collection, not in the right. */
  public boolean leftOnly() {
    return right == null;
  }

  /* The element exist in the right collection, not in the left. */
  public boolean rightOnly() {
    return left == null;
  }

  /* The element exist in both collections. Element left equals the right instance. */
  public boolean isEqual() {
    return left != null && right != null;
  }

  /**
   * Return a status message based on the element existence: - exist in left and right collection -
   * exist left only - or, exist right only
   */
  public String status(String equal, String left, String right) {
    return leftOnly() ? left : (rightOnly() ? right : equal);
  }

  @Override
  public String toString() {
    if (isEqual()) return "(eq: " + element() + ")";
    if (leftOnly()) return "(left: " + left + ")";
    return "(right: " + right + ")";
  }

  static <T> DiffEntry<T> ofLeft(T left) {
    return new DiffEntry<T>(left, null);
  }

  static <T> DiffEntry<T> ofRight(T right) {
    return new DiffEntry<T>(null, right);
  }

  static <T> DiffEntry<T> ofEqual(T left, T right) {
    return new DiffEntry<T>(left, right);
  }
}
