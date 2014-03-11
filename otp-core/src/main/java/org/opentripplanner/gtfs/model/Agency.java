package org.opentripplanner.gtfs.model;


public class Agency extends GtfsEntity {

    private static final long serialVersionUID = 1L;

    @Required public String agency_name;        
    @Required public String agency_url;        
    @Required public String agency_timezone;        
    public String agency_id;        
    public String agency_lang ;        
    public String agency_phone;        
    public String agency_fare_url;
    
    @Override
    public String getKey() {
        return agency_id == null ? agency_name : agency_id;
    }

}