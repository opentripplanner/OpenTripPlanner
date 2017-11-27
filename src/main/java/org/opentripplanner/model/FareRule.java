/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.model;

import java.io.Serializable;

public final class FareRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private FareAttribute fare;

    private Route route;

    private String originId;

    private String destinationId;

    private String containsId;

    public FareAttribute getFare() {
        return fare;
    }

    public void setFare(FareAttribute fare) {
        this.fare = fare;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public String getContainsId() {
        return containsId;
    }

    public void setContainsId(String containsId) {
        this.containsId = containsId;
    }

    public String toString() {
        return "<FareRule "
                + toStrOpt(" route=", route)
                + toStrOpt(" origin=", originId)
                + toStrOpt(" contains=", containsId)
                + toStrOpt(" destination=", destinationId)
                + ">";
    }


    private static String toStrOpt(String lbl, IdentityBean arg) {
        return (arg == null ? "" : lbl + arg.getId());
    }

    private static String toStrOpt(String lbl, String arg) {
        return (arg == null ? "" : lbl + '\'' + arg + '\'');
    }
}
