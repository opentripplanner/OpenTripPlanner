/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.opentripplanner.common.diff;

/**
 * This represents a single difference found between two objects by the ObjectDiffer.
 */
public class Difference {

    Object a;
    Object b;
    String message;

    public Difference(Object a, Object b) {
        this.a = a;
        this.b = b;
    }

    public Difference withMessage (String formatString, Object... args) {
        message = String.format(formatString, args);
        return this;
    }

}
