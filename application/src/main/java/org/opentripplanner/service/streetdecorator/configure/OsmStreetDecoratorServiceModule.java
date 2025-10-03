package org.opentripplanner.service.streetdecorator.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.streetdecorator.OsmStreetDecoratorService;
import org.opentripplanner.service.streetdecorator.internal.DefaultOsmStreetDecoratorService;

@Module
public interface OsmStreetDecoratorServiceModule {
  @Binds
  OsmStreetDecoratorService bind(DefaultOsmStreetDecoratorService service);
}
