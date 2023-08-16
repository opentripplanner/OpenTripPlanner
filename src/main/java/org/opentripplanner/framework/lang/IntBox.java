package org.opentripplanner.framework.lang;

/**
 * An IntBox is a writable container for an int. The most common use-case for this class is to
 * be able to set an integer value inside a lambda callback where local variables is not
 * accessible.
 */
public final class IntBox {

  private int value;

  public IntBox(int value) {
    this.value = value;
  }

  public int get() {
    return value;
  }

  public void set(int value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || o.getClass() != IntBox.class) {
      return false;
    }
    return value == ((IntBox) o).value;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(value);
  }

  @Override
  public String toString() {
    return Integer.toString(value);
  }

  public void inc() {
    ++value;
  }
}
