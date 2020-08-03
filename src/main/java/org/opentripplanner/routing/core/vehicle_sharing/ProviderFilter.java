package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Locale;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class ProviderFilter implements VehicleFilter {

    private final Set<String> providers;

    private final boolean allowed;

    private ProviderFilter(Set<String> providers, boolean allowed) {
        this.providers = providers.stream().map(p -> p.toLowerCase(Locale.US)).collect(toSet());
        this.allowed = allowed;
    }

    public static ProviderFilter providersAllowedFilter(Set<String> providersAllowed) {
        return new ProviderFilter(providersAllowed, true);
    }

    public static ProviderFilter providersDisallowedFilter(Set<String> providersDisallowed) {
        return new ProviderFilter(providersDisallowed, false);
    }

    @Override
    public boolean isValid(VehicleDescription vehicle) {
        if (allowed) {
            return providers.contains(vehicle.getProvider().getProviderName().toLowerCase(Locale.US));
        } else {
            return !providers.contains(vehicle.getProvider().getProviderName().toLowerCase(Locale.US));
        }
    }
}
