/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.trippattern;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DrtTravelTime implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Pattern PATTERN = Pattern.compile(
            "(?<onlyConstant>^[0-9.]+$)|(^(?<coefficient>[0-9.]+)t(\\+(?<constant>[0-9.]+))?$)");

    static final String ERROR_MSG = "Invalid DRT formula. Valid forms are: 3, 4t, 2.5t+5";

    private String spec;
    private double coefficient = 0;
    private double constant = 0;

    public static DrtTravelTime fromSpec(String spec) {
        Matcher matcher = PATTERN.matcher(spec);
        if (matcher.find()) {
            // process and convert to seconds
            DrtTravelTime tt = new DrtTravelTime();
            String constantStr = matcher.group("onlyConstant");
            if (constantStr != null) {
                tt.constant = Double.parseDouble(constantStr) * 60;
            }
            constantStr = matcher.group("constant");
            if (constantStr != null) {
                tt.constant = Double.parseDouble(constantStr) * 60;
            }
            String coeffStr = matcher.group("coefficient");
            if (coeffStr != null) {
                tt.coefficient = Double.parseDouble(coeffStr);
            }
            tt.spec = spec;
            return tt;
        }
        throw new IllegalArgumentException(ERROR_MSG);
    }

    /**
     * Given a direct time in seconds, return the time processed by the arithmetic function
     * represented by this class.
     */
    public double process(double time) {
        return ((time * coefficient) + constant);
    }

    public double getCoefficient() {
        return coefficient;
    }

    public double getConstant() {
        return constant;
    }

    public String getSpec() {
        return spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DrtTravelTime that = (DrtTravelTime) o;

        if (Double.compare(that.coefficient, coefficient) != 0) return false;
        if (Double.compare(that.constant, constant) != 0) return false;
        return spec.equals(that.spec);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = spec.hashCode();
        temp = Double.doubleToLongBits(coefficient);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(constant);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
