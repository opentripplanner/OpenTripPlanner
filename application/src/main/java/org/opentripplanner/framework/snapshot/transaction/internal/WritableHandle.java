package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.function.Supplier;

/**
 * Package-private companion to {@link org.opentripplanner.framework.snapshot.transaction.RepositoryHandle}
 * that exposes the mutable snapshot supplier.
 *
 * <p>This interface is implemented by the anonymous {@code RepositoryHandle} instances created in
 * {@link DefaultRepositoryRegistry}. It is intentionally not part of the public
 * {@code RepositoryHandle} API — only {@link DefaultWriteContext} casts to it internally to obtain
 * write access on behalf of a submitted update task.
 */
public interface WritableHandle<M> {
  Supplier<M> mutableSnapshot();
}
