package org.opentripplanner.routing.core;

import java.util.Currency;
import java.util.Locale;

/**
 * A Bean wrapper class for java.util.Currency 
 * @author novalis
 *
 */
public class WrappedCurrency {
    private Currency value;
    
    public WrappedCurrency() {
        value = null;
    }

    public WrappedCurrency(Currency value) {
        this.value = value;
    }
    
    public WrappedCurrency(String name) {
        value = Currency.getInstance(name);
    }

    public int getDefaultFractionDigits() {
        return value.getDefaultFractionDigits();
    }
    
    public String getCurrencyCode() {
        return value.getCurrencyCode();
    }
    
    public String getSymbol() {
        return value.getSymbol();
    }
    
    public String getSymbol(Locale l) {
        return value.getSymbol(l);
    }

    public String toString() {
        return value.toString();
    }
    
    public boolean equals(Object o) {
        if (o instanceof WrappedCurrency) {
            WrappedCurrency c = (WrappedCurrency) o;
            return value.equals(c.value);
        }
        return false;
    }
    
    public Currency getCurrency() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
