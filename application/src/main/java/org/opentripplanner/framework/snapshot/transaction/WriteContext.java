package org.opentripplanner.framework.snapshot.transaction;

import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.event.DomainEvent;
import org.opentripplanner.framework.snapshot.event.EventHandler;

/**
 * Task-scoped coordination point for write access during a single update.
 *
 * <p>A {@code WriteContext} is created fresh for each task submitted to the {@link UpdateManager}
 * and lives only for the duration of that task. It is the sole entry point for:
 * <ul>
 *   <li>Obtaining a mutable snapshot via {@link #mutable(RepositoryHandle)}</li>
 *   <li>Publishing domain events that may trigger further writes via {@link #publish(DomainEvent)}</li>
 * </ul>
 *
 * <p>Code outside a submitted task cannot obtain a {@code WriteContext}, making the write path
 * unforgeable. The {@link UpdateManager} commits all changes automatically after the task returns.
 */
public interface WriteContext {
  /**
   * Return the mutable snapshot for the given repository handle.
   *
   * <p>Copy-on-write is performed lazily on first access within the task. Subsequent calls for
   * the same handle return the same mutable instance.
   *
   * @param handle the repository handle registered at startup
   * @param <M>    the mutable snapshot type
   * @return the mutable snapshot for this task
   */
  <M> Supplier<M> mutable(RepositoryHandle<?, M> handle);

  /**
   * Publish a domain event, dispatching synchronously to all registered
   * {@link EventHandler}s.
   *
   * <p>Handlers that need to write to a repository receive the mutable snapshot via their
   * {@code handle} method parameter — injected by this context at dispatch time.
   *
   * @param event the domain event to publish
   */
  void publish(DomainEvent event);
}
