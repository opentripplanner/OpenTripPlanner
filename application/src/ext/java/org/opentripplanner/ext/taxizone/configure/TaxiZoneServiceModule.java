package org.opentripplanner.ext.taxizone.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.ext.taxizone.TaxiZoneService;
import org.opentripplanner.ext.taxizone.internal.DefaultTaxiZoneService;
import org.opentripplanner.framework.application.OTPFeature;

@Module
public class TaxiZoneServiceModule {

  @Provides
  @Nullable
  public TaxiZoneService provideTaxiZoneService(@Nullable TaxiZoneRepository taxiZoneRepository) {
    if (OTPFeature.TaxiZone.isOff() || taxiZoneRepository == null) {
      return null;
    }
    return new DefaultTaxiZoneService(taxiZoneRepository.getZones());
  }
}
