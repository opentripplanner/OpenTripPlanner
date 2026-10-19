package org.opentripplanner.transit.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transit.dataimport.TimetableDataImport;

/**
 * Provides an empty {@link TimetableDataImport}.
 */
@Module
public class TimetableDataImportModule {

  @Provides
  @Singleton
  public TimetableDataImport provideTimetableDataImport() {
    return new TimetableDataImport();
  }
}
