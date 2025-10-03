package org.opentripplanner.service.streetdecorator.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.streetdecorator.OsmStreetDecoratorRepository;
import org.opentripplanner.service.streetdecorator.internal.DefaultOsmStreetDecoratorRepository;

@Module
public interface OsmStreetDecoratorRepositoryModule {
  @Binds
  OsmStreetDecoratorRepository bind(DefaultOsmStreetDecoratorRepository repository);
}
