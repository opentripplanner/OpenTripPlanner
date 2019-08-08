package org.opentripplanner.model;

public final class IgnoredAlert extends IdentityBean<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String description;

    public IgnoredAlert() {
    }

    public IgnoredAlert(IgnoredAlert a) {
        this.id = a.id;
        this.description = a.description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "<IgnoredAlert " + this.id + ">";
    }
}
