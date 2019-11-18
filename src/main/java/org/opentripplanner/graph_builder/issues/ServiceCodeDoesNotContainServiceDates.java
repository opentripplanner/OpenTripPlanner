package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;

public class ServiceCodeDoesNotContainServiceDates implements DataImportIssue {

        public static final String FMT = "ServiceCode %s does not contain any serviceDates";

        public final String serviceId;

        public ServiceCodeDoesNotContainServiceDates(String serviceId){
                this.serviceId = serviceId;
        }

        @Override
        public String getMessage() {
                return String.format(FMT, serviceId);
        }
}
