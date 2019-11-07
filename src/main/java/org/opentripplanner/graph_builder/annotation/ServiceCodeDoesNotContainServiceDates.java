package org.opentripplanner.graph_builder.annotation;

public class ServiceCodeDoesNotContainServiceDates extends GraphBuilderAnnotation {
        private static final long serialVersionUID = 1L;

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
