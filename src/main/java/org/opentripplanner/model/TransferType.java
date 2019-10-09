package org.opentripplanner.model;

// TODO add codes
public enum TransferType {
    POSSIBLE(0),
    GUARANTEED(1),
    GUARANTEED_WITH_MIN_TIME(2),
    FORBIDDEN(3);

    TransferType(int gtfsCode) {
                this.gtfsCode = gtfsCode;
        }

    public final int gtfsCode;

    public static TransferType valueOfGtfsCode(int gtfsCode) {
       for (TransferType value : values()) {
           if (value.gtfsCode == gtfsCode) {
               return value;
           }
       }
       throw new IllegalArgumentException("Unknown GTFS TransferType: " + gtfsCode);
    }
}
