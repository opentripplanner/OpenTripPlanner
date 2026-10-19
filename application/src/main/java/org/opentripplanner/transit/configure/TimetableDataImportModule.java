package org.opentripplanner.transit.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transit.dataimport.TimetableDataImport;

/**
 * Provides the empty {@link TimetableDataImport} populated by the graph build. When a graph
 * is loaded, the deserialized instance is used instead.
 */
@Module
public class TimetableDataImportModule {

  @Provides
  @Singleton
  public TimetableDataImport provideTimetableDataImport() {
    return new TimetableDataImport();
  }
}
