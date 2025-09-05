package org.opentripplanner.ext.empiricaldelay.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.internal.graphbuilder.EmpiricalDelayGraphBuilder;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.TimetableRepository;

@Module
public class EmpiricalDelayGraphBuilderModule {

  @Provides
  @Singleton
  @Nullable
  static EmpiricalDelayGraphBuilder provideEmpiricalDelayGraphBuilder(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    @Nullable EmpiricalDelayRepository empiricalDelayRepository,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore,
    Deduplicator deduplicator
  ) {
    if (OTPFeature.EmpiricalDelay.isOff() || empiricalDelayRepository == null) {
      return null;
    }

    return new EmpiricalDelayGraphBuilder(
      dataSources.getEmpiricalDelayConfiguredDataSource(),
      deduplicator,
      issueStore,
      config.empiricalDelay,
      empiricalDelayRepository,
      timetableRepository
    );
  }
}
