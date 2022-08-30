package org.opentripplanner.common.model;

import java.io.Serializable;

/**
 * An ordered object of three (potentially different types)
 */
public record T3<E1, E2, E3>(E1 first, E2 second, E3 third) implements Serializable {
  private static final long serialVersionUID = 1L;
}
