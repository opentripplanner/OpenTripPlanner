package org.opentripplanner.routing.core.vehicle_sharing;

public class Provider {
    private final int providerId;

    private final String providerName;

    public Provider(int providerId, String providerName) {
        this.providerId = providerId;
        this.providerName = providerName;
    }

    public int getProviderId() {
        return providerId;
    }

    public String getProviderName() {
        return providerName;
    }
}
