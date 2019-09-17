package org.opentripplanner.model;

// TODO add codes
public enum TransferType {
        POSSIBLE(-2),
        GUARANTEED(-7),
        GUARANTEED_WITH_MIN_TIME(1),
        FORBIDDEN(3);

        TransferType(int gtfsCode) {
                this.gtfsCode = gtfsCode;
        }

        public final int gtfsCode;
}
