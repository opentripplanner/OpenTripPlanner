package org.opentripplanner.common.model;

import java.io.Serializable;

/**
 * An ordered objects of three (the same type)
 *
 * @param <E>
 */
public record P3<E>(E first, E second, E third) implements Serializable {
  private static final long serialVersionUID = 1L;
}
