package org.opentripplanner.model;

/**
 * OTP model for branding. Common for both NeTEx and GTFS.
 */
public class Branding extends TransitEntity {
    private String shortName;
    private String name;
    private String url;
    private String description;
    private String image;

    public Branding(FeedScopedId id) {
        super(id);
    }

    public Branding() {
        super(null);
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
