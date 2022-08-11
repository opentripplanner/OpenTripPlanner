package org.opentripplanner.common.model;

import java.io.Serializable;

/**
 * An ordered object of three (potentially different types)
 */
public class T3<E1, E2, E3> implements Serializable {
  private static final long serialVersionUID = 1L;

  public final E1 first;

  public final E2 second;

  public final E3 third;

  public T3(E1 first, E2 second, E3 third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  @Override
  public int hashCode() {
    return (first != null ? first.hashCode() : 0) + (second != null ? second.hashCode() : 0) + (third != null ? third.hashCode() : 0);
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof T3)) return false;

    T3 other = (T3) object;

    if (first == null) {
      if (other.first != null) return false;
    } else {
      if (!first.equals(other.first)) return false;
    }

    if (second == null) {
      if (other.second != null) return false;
    } else {
      if (!second.equals(other.second)) return false;
    }

    if (third == null) {
      if (other.third != null) return false;
    } else {
      if (!third.equals(other.third)) return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "T3(" + first + ", " + second + ", " + third + ")";
  }
}