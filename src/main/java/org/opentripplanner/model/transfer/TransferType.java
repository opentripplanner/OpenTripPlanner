package org.opentripplanner.model.transfer;

public enum TransferType {
    /**
     * This transfer is recommended over other transfers. The routing algorithm should prefer
     * this transfer compared to other transfers, for example by assigning a lower weight to it.
     */
    RECOMMENDED(0),
    /**
     * This means the departing vehicle will wait for the arriving one and leave sufficient time
     * for a rider to transfer between routes.
     */
    GUARANTEED(1),
    /**
     * This is a regular transfer that is defined in the transit data (as opposed to
     * OpenStreetMap data). In the case that both are present, this should take precedence.
     * Because the the duration of the transfer is given and not the distance, walk speed will
     * have no effect on this.
     */
    MIN_TIME(2),
    /**
     * Transfers between these stops (and route/trip) is not possible (or not allowed), even if
     * a transfer is already defined via OpenStreetMap data or in transit data.
     */
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
