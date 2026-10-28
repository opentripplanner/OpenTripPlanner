package org.opentripplanner.service.shape.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.framework.transaction.configure.TransitDomain;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.ShapeRepositorySnapshot;
import org.opentripplanner.service.shape.internal.DefaultShapeRepository;
import org.opentripplanner.service.shape.internal.ShapeRepositoryLifecycle;

/**
 * Registers the {@link ShapeRepository} in the transit domain's transaction registry. The
 * repository owns the {@link ScheduledShapeData}; real-time shapes are not part of the graph, so
 * they start empty at every startup.
 */
@Module
public class ShapeModule {

  @Provides
  @Singleton
  public static RepositoryHandle<ShapeRepositorySnapshot, ShapeRepository> shapeRepositoryHandle(
    ScheduledShapeData scheduledShapeData,
    @TransitDomain RepositoryRegistry repositoryRegistry
  ) {
    var buffer = new DefaultShapeRepository(scheduledShapeData);
    var lifecycle = new ShapeRepositoryLifecycle(buffer);
    return repositoryRegistry.registerRepository(buffer, lifecycle);
  }
}
