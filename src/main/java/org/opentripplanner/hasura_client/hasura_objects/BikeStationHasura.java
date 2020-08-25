package org.opentripplanner.hasura_client.hasura_objects;

import org.opentripplanner.routing.core.vehicle_sharing.Provider;

public class BikeStationHasura extends HasuraObject {
    private int bikesAvaiable;
    private int spacesAvaiable;
    private Provider provider;
    private double longitude;
    private double latitude;
    private long id;
    private String name;

    public int getBikesAvaiable() {
        return bikesAvaiable;
    }

    public void setBikesAvaiable(int bikesAvaiable) {
        this.bikesAvaiable = bikesAvaiable;
    }

    public int getSpacesAvaiable() {
        return spacesAvaiable;
    }

    public void setSpacesAvaiable(int spacesAvaiable) {
        this.spacesAvaiable = spacesAvaiable;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
