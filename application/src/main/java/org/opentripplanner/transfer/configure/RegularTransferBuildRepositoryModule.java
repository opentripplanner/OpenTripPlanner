package org.opentripplanner.raptor.data.transfers.regular.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

@Module
public class RegularTransferBuildRepositoryModule {

  @Provides
  @Singleton
  public RegularTransferBuildRepository<NearbyStop> provideRegularTransferBuildRepository() {
    return RegularTransferFactory.createBuildRepository();
  }
}
