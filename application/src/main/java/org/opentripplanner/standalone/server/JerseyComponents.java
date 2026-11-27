package org.opentripplanner.standalone.server;

import java.util.Set;

/**
 * Resources and providers to register with a JAX-RS application, in addition to the ones the
 * application always registers. This lets the server wiring decide which application serves an
 * optional API.
 *
 * @param classes    request-scoped components, see {@code Application.getClasses()}
 * @param singletons singleton components, see {@code Application.getSingletons()}
 */
public record JerseyComponents(Set<Class<?>> classes, Set<Object> singletons) {
  /** No additional components. */
  public static final JerseyComponents EMPTY = new JerseyComponents(Set.of(), Set.of());

  public JerseyComponents {
    classes = Set.copyOf(classes);
    singletons = Set.copyOf(singletons);
  }
}
