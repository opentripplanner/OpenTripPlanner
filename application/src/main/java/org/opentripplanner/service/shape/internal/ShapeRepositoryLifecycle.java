package org.opentripplanner.service.shape.internal;

import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.ShapeRepositorySnapshot;

/**
 * Copy-on-write / freeze lifecycle for the shape repository. The repository is a single long-lived
 * buffer shared by all transactions: {@link #copyOnWrite} hands out the same buffer instance, and
 * {@link #freeze} publishes an immutable snapshot of it.
 */
public class ShapeRepositoryLifecycle
  implements RepositoryLifecycle<ShapeRepositorySnapshot, ShapeRepository>
{

  private final ShapeRepository buffer;

  public ShapeRepositoryLifecycle(ShapeRepository buffer) {
    this.buffer = buffer;
  }

  @Override
  public ShapeRepository copyOnWrite(ShapeRepositorySnapshot snapshot) {
    return buffer;
  }

  @Override
  public ShapeRepositorySnapshot freeze(ShapeRepository repository) {
    return buffer.createSnapshot();
  }
}
