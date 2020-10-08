package org.opentripplanner.model;

/**
 * This is an element that originates from the NeTEx specification and is described as "Text-based notification
 * describing circumstances which cannot be modelled as structured data." Any NeTEx element can have a notice attached,
 * although not all are supported in OTP.
 */
public class Notice extends TransitEntity {

    private static final long serialVersionUID = 1L;

    private String text;

    private String publicCode;


    public Notice(FeedScopedId id) {
        super(id);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String publicCode) {
        this.publicCode = publicCode;
    }

    @Override
    public String toString() { return "<Notice " + getId() + ">"; }
}