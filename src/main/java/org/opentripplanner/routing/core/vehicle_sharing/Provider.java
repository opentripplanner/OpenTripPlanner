package org.opentripplanner.routing.core.vehicle_sharing;

public class Provider {
    private final int id;

    private final String name;

    public Provider(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
