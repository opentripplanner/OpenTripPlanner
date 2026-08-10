package org.opentripplanner.updater.spi;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Routes each write domain to the callback of its writer thread.
 */
public class WriteToGraphCallbacks {

  private final Map<WriteDomain<?>, WriteToGraphCallback<?>> byDomain = new HashMap<>();

  public <C> WriteToGraphCallbacks with(WriteDomain<C> domain, WriteToGraphCallback<C> callback) {
    byDomain.put(domain, callback);
    return this;
  }

  /**
   * Return the callback registered for the given domain, or {@code null} if the domain has no
   * callback.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <C> WriteToGraphCallback<C> forDomain(WriteDomain<C> domain) {
    // This is a typesafe heterogeneous container: an entry can only be added by a with(..) call
    // that pairs a domain key with a callback of the matching context type, so this cast is safe.
    return (WriteToGraphCallback<C>) byDomain.get(domain);
  }
}
