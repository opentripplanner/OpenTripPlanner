package org.opentripplanner.routing.core;

import java.util.Currency;
import java.util.Locale;

public class WrappedCurrency {
    public Currency value;
    
    public WrappedCurrency() {
        value = null;
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
    
    public WrappedCurrency(Currency value) {
        this.value = value;
    }
    
    public String toString() {
        return value.toString();
    }
    
}
