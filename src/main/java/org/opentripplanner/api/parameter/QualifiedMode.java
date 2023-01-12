package org.opentripplanner.api.parameter;

import jakarta.ws.rs.BadRequestException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class QualifiedMode implements Serializable {

  public final ApiRequestMode mode;
  public final Set<Qualifier> qualifiers;

  public QualifiedMode(String qMode) {
    try {
      Collection<Qualifier> qualifiers = new ArrayList<>();
      StringBuilder mode = new StringBuilder();

      String[] elements = qMode.split("_");
      mode.append(elements[0].trim());
      for (int i = 1; i < elements.length; i++) {
        String element = elements[i].trim();
        if (element.isBlank()) {
          continue;
        }
        try {
          Qualifier q = Qualifier.valueOf(element);
          qualifiers.add(q);
        } catch (IllegalArgumentException e) {
          mode.append("_");
          mode.append(element);
        }
      }

      this.mode = ApiRequestMode.valueOf(mode.toString());
      this.qualifiers = Set.copyOf(qualifiers);
    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
      throw new BadRequestException(
        "Qualified mode is not valid: '" + qMode + "', details: " + e.getMessage()
      );
    }
  }

  public QualifiedMode(ApiRequestMode mode, Qualifier... qualifiers) {
    this.mode = mode;
    this.qualifiers = Set.of(qualifiers);
  }

  @Override
  public int hashCode() {
    return mode.hashCode() * qualifiers.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof QualifiedMode) {
      QualifiedMode qmOther = (QualifiedMode) other;
      return qmOther.mode.equals(this.mode) && qmOther.qualifiers.equals(this.qualifiers);
    }
    return false;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(mode);
    for (Qualifier qualifier : qualifiers) {
      sb.append("_");
      sb.append(qualifier);
    }
    return sb.toString();
  }
}
