package org.opentripplanner.routing.core;

import java.text.NumberFormat;
import java.util.Currency;

/**
 * <strong>Fare support is very, very preliminary.</strong>
 *
 */
public class Money implements Comparable<Money> {

    /**
     * The currency of the money.
     */
    private WrappedCurrency currency = null;
    /**
     * The actual currency value in decimal fixed-point, with the default number of fraction digits
     * from currency after the decimal point.
     */
    private int cents; 

    public Money() {}
    
    public Money(WrappedCurrency currency, int cents) {
        this.currency = currency;
        this.cents = cents;
    }
    
    public String toString() {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        Currency cur = currency.getCurrency();
        if (cur == null) {
            return "Money()";
        }
        nf.setCurrency(cur);
        String c = nf.format(cents / (Math.pow(10, currency.getDefaultFractionDigits())));
        return "Money(" + c + ")";
    }
    
    public boolean equals(Object other) {
        if (other instanceof Money) {
            Money m = (Money) other;
            return m.currency.equals(currency) && m.cents == cents;
        }
        return false;
    }
    
    public int compareTo(Money m) {
        if (m.currency != currency) {
            throw new RuntimeException("Can't compare " + m.currency + " to " + currency);
        }
        return cents - m.cents;
    }

    public void setCurrency(Currency currency) {
        this.currency = new WrappedCurrency(currency);
    }
    
    public void setCurrency(WrappedCurrency currency) {
        this.currency = currency;
    }

    public WrappedCurrency getCurrency() {
        return currency;
    }

    public void setCents(int cents) {
        this.cents = cents;
    }

    public int getCents() {
        return cents;
    }

    @Override
    public int hashCode() {
        return currency.hashCode() * 31 + cents;
    }
}
