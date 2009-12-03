package org.opentripplanner.api.model;

import java.util.Currency;

public class Money {

    Currency currency;
    int cents; 

    /*
     * cents: the currency in decimal fixed-point, with
     * currency.getDefaultFractionDigits() digits after the decimal point.
     */
    public Money() {}
    
    public Money(Currency currency, int cents) {
        this.currency = currency;
        this.cents = cents;
    }
    

}
