package org.opentripplanner.raptor.util.paretoset;

import java.util.Objects;

class Vector {

  private static final int NOT_SET = -999;
  final String name;
  final int v1, v2, v3, v4;

  Vector(Vector o) {
    this(o.name, o.v1, o.v2, o.v3, o.v4);
  }

  Vector(String name, int v1) {
    this(name, v1, NOT_SET, NOT_SET, NOT_SET);
  }

  Vector(String name, int v1, int v2) {
    this(name, v1, v2, NOT_SET, NOT_SET);
  }

  Vector(String name, int v1, int v2, int v3) {
    this(name, v1, v2, v3, NOT_SET);
  }

  Vector(String name, int v1, int v2, int v3, int v4) {
    this.name = name;
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    this.v4 = v4;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, v1, v2, v3, v4);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Vector v = (Vector) o;
    return v1 == v.v1 && v2 == v.v2 && v3 == v.v3 && v4 == v.v4 && name.equals(v.name);
  }

  @Override
  public String toString() {
    if (v2 == NOT_SET) {
      return "%s[%d]".formatted(name, v1);
    }
    if (v3 == NOT_SET) {
      return "%s[%d, %d]".formatted(name, v1, v2);
    }
    if (v4 == NOT_SET) {
      return "%s[%d, %d, %d]".formatted(name, v1, v2, v3);
    }
    return "%s[%d, %d, %d, %d]".formatted(name, v1, v2, v3, v4);
  }
}
