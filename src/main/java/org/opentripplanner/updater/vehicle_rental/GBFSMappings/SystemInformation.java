package org.opentripplanner.updater.vehicle_rental.GBFSMappings;

/**
 * Response class for the system_information.json file.
 * See https://github.com/NABSA/gbfs/blob/v1.0/gbfs.md#system_informationjson
 */
public class SystemInformation extends BaseGtfsResponse {
    public SystemInformationData data;

    /**
     * See https://github.com/NABSA/gbfs/blob/v1.0/gbfs.md#system_informationjson
     */
    public class SystemInformationData {
        public String system_id;
        public String language;
        public String name;
        public String short_name;
        public String operator;
        public String url;
        public String purchase_url;
        public String start_date;
        public String phone_number;
        public String email;
        public String timezone;
        public String license_url;
    }
}
