package org.opentripplanner.service.shape.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.service.shape.dataimport.ShapeDataImport;

/** Provides an empty {@link ShapeDataImport}. */
@Module
public class ShapeDataImportModule {

  @Provides
  @Singleton
  public ShapeDataImport provideShapeDataImport() {
    return new ShapeDataImport();
  }
}
