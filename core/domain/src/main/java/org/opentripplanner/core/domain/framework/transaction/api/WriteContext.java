package org.opentripplanner.framework.transaction.api;

import org.opentripplanner.framework.event.DomainEvent;
import org.opentripplanner.framework.event.EventHandler;
import org.opentripplanner.framework.transaction.UpdateManager;

/**
 * Task-scoped coordination point for write access during a single update.
 *
 * <p>A {@code WriteContext} is created fresh for each task submitted to the {@link UpdateManager}
 * and lives only for the duration of that task. It is the sole entry point for:
 * <ul>
 *   <li>Getting a mutable repository within the context</li>
 *   <li>Publishing domain events</li>
 * </ul>
 *
 * <p>Code outside a submitted task cannot obtain a {@code WriteContext}, making the write path
 * unforgeable. The {@link UpdateManager} commits all changes automatically after the task returns.
 */
public interface WriteContext {
  /**
   * Return the mutable repository for the given handle within the context of a transaction.
   *
   * @param handle the repository handle
   * @param <M> the mutable repository type
   * @return the mutable repository for this task
   */
  <M> M repository(RepositoryHandle<?, M> handle);

  /**
   * Publish a domain event, dispatching synchronously to all registered {@link EventHandler}s.
   *
   * <p>Handlers that need to write to a repository receive the mutable repository via their
   * {@code handle} method parameter — injected by this context at dispatch time.
   *
   * @param event the domain event to publish
   */
  void publish(DomainEvent event);
}
