package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Provider provider = (Provider) o;
        return providerId == provider.providerId &&
                Objects.equals(providerName, provider.providerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, providerName);
    }
}
