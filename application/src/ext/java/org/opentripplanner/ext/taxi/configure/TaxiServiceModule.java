package org.opentripplanner.ext.taxi.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.taxi.TaxiRepository;
import org.opentripplanner.ext.taxi.TaxiService;
import org.opentripplanner.ext.taxi.internal.DefaultTaxiService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;

@Module
public class TaxiServiceModule {

  @Provides
  @Nullable
  public TaxiService provideTaxiService(
    @Nullable TaxiRepository taxiRepository,
    Graph graph,
    StreetLimitationParametersService streetLimitationParametersService,
    VehicleRentalService vehicleRentalService,
    StreetDetailsService streetDetailsService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings
  ) {
    if (OTPFeature.TaxiRouting.isOff() || taxiRepository == null) {
      return null;
    }
    return new DefaultTaxiService(
      taxiRepository.getZones(),
      graph,
      streetLimitationParametersService,
      vehicleRentalService,
      streetDetailsService,
      dataOverlayParameterBindings
    );
  }
}
