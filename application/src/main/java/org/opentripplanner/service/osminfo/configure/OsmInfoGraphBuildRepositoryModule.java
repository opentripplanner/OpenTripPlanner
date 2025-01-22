package org.opentripplanner.service.osminfo.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;

@Module
public interface OsmInfoGraphBuildRepositoryModule {
  @Binds
  OsmInfoGraphBuildRepository bind(DefaultOsmInfoGraphBuildRepository repository);
}
