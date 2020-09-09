package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Objects;

public class Provider {

    private int providerId;

    private String providerName;

    public Provider() {
    }

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

    public void setProviderId(int providerId) {
        this.providerId = providerId;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
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
