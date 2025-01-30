package org.opentripplanner.service.osminfo.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildService;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildService;

@Module
public interface OsmInfoGraphBuildServiceModule {
  @Binds
  OsmInfoGraphBuildService bind(DefaultOsmInfoGraphBuildService service);
}
