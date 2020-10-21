package org.opentripplanner.model;

/**
 * A company which is responsible for operating public transport services.
 * The operator will often operate under contract with an Authority (Agency).
 * <p/>
 * Netex ONLY. Operators are available only if the data source is Netex, not GTFS.
 *
 * @see Agency
 */
public class Operator extends TransitEntity {

    private static final long serialVersionUID = 1L;

    private String name;

    private String url;

    private String phone;


    public Operator(FeedScopedId id) {
        super(id);
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String toString() {
        return "<Operator " + getId() + ">";
    }
}
