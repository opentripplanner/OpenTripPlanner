package org.opentripplanner.raptor.api.model;

import java.util.Objects;

/// This class is a utility class which can hold a value an the unit. It can be used to parse
/// a value as printed by the {@link #toString()} method. This is most of all useful in tests.
public class RaptorValue {

  private final int value;
  private final RaptorValueType type;

  public RaptorValue(int value, RaptorValueType type) {
    this.value = value;
    this.type = type;
  }

  /// Parse values like `C₁1_880.87`, `C₂8_000`, `Wₜ1.34`, `Tₙ5`, `Tₚ3`, `Rₙ1` and `Vₙ1` into an
  /// instance of this class.
  public static RaptorValue of(String text) {
    for (var type : RaptorValueType.values()) {
      if (text.startsWith(type.prefix())) {
        return new RaptorValue(type.parseValue(text), type);
      }
    }
    throw new IllegalArgumentException("Not a valid RaptorValue: " + text);
  }

  public int value() {
    return value;
  }

  public RaptorValueType type() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RaptorValue that = (RaptorValue) o;
    return value == that.value && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, type);
  }

  @Override
  public String toString() {
    return type.format(value);
  }
}
