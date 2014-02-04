/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

otp.namespace("otp.analyst");

/**
 * Widget class. Control the parameters of a request.
 * 
 * Require jQuery/jQueryUI.
 */
otp.analyst.ParamsWidget = otp.Class({

    /**
     * Constructor.
     * 
     * @param node
     *            The DOM container to contain the widget.
     * @options Object containing the
     */
    initialize : function(node, options) {
        this.options = $.extend({
            defaultRouterId : "",
            selectDateTime : true,
            defaultArriveBy : false,
            defaultDateTime : new Date(),
            dateFormat : "yy/mm/dd",
            selectModes : true,
            defaultModes : "WALK,TRANSIT",
            selectMaxWalk : true,
            defaultMaxWalk : 1000,
            coordinateOrigin : null,
            selectMaxTime : false,
            defaultMaxTime : 3600,
            defaultPrecision : 100,
            selectDataType : true,
            defaultDataType : "TIME",
            refreshButton : false
        }, options);
        this.parentNode = node;
        var thisRw = this;
        // Refresh callbacks list
        this.refreshCallbacks = $.Callbacks();
        // Origin marker
        if (this.options.map) {
            if (this.options.extend) {
                // Same origin or not
                var locationDiv = $("<div/>");
                node.append(locationDiv);
                this.locationCheckbox = $("<input/>").attr({
                    type : "checkbox"
                }).click(function() {
                    thisRw.enableLocationMarker(this.checked);
                });
                locationDiv.text("Different origin");
                locationDiv.append(this.locationCheckbox);
            } else {
                thisRw.enableLocationMarker(true);
            }
        }
        // ArriveBy/Date/time picker
        if (this.options.selectDateTime) {
            var dateTimeDiv = $("<div/>");
            node.append(dateTimeDiv);
            // Arrive By
            this.arriveByInput = $("<select/>");
            if (this.options.extend)
                this.arriveByInput.append($("<option />").text("(same)").val("inherit"));
            this.arriveByInput.append($("<option />").text("Depart at").val(false), $("<option />").text("Arrive by")
                    .val(true));
            if (!this.options.extend)
                this.arriveByInput.val(this.options.defaultArriveBy);
            dateTimeDiv.append(this.arriveByInput);
            // Date
            if (!this.options.extend) {
                this.dateInput = $("<input/>").attr({
                    size : 12,
                    maxlength : 12,
                }).datepicker({
                    dateFormat : this.options.dateFormat
                }).datepicker("setDate", this.options.defaultDateTime);
                dateTimeDiv.append(this.dateInput);
            }
            // Hour:minutes (ev. delta)
            var hour = this.options.defaultDateTime.getHours();
            var min = this.options.defaultDateTime.getMinutes();
            this.timeInput = $("<input/>").attr({
                size : 5,
                maxlength : 5,
                length : 5,
                value : this.options.extend ? "0:00" : (hour + ":" + (min < 10 ? "0" : "") + min)
            });
            dateTimeDiv.append(this.timeInput);
        }
        // Mode selector
        if (this.options.selectModes) {
            var modesDiv = $("<div/>");
            node.append(modesDiv);
            modesDiv.text("Modes");
            this.modesInput = $("<select/>");
            if (this.options.extend)
                this.modesInput.append($("<option />").text("(same)").val("inherit"));
            // TODO Add other modes, depending on options
            // WALK, BICYCLE, CAR, TRAM, SUBWAY, RAIL, BUS, FERRY,
            // CABLE_CAR, GONDOLA, FUNICULAR, TRANSIT, TRAINISH, BUSISH
            var modes = [ [ "Transit", "WALK,TRANSIT" ], [ "Tram only", "WALK,TRAM" ],
                    [ "Subway only", "WALK,SUBWAY" ], [ "Subway+Tram only", "WALK,TRAM,SUBWAY" ],
                    [ "Walk only", "WALK" ], [ "Bike only", "BICYCLE" ], [ "Car only", "CAR" ],
                    [ "Bike + Subway", "BICYCLE,SUBWAY" ] ];
            for (var i = 0; i < modes.length; i++) {
                this.modesInput.append($("<option />").text(modes[i][0]).val(modes[i][1]));
            }
            if (!this.options.extend)
                this.modesInput.val(this.options.defaultModes);
            modesDiv.append(this.modesInput);
        }
        // Max walk distance
        if (this.options.selectMaxWalk) {
            var maxWalkDiv = $("<div/>");
            node.append(maxWalkDiv);
            maxWalkDiv.text("Max walk");
            this.maxWalkInput = $("<select/>");
            if (this.options.extend)
                this.maxWalkInput.append($("<option />").text("(same)").val("inherit"));
            this.maxWalkInput.append($("<option />").text("500m").val("500"), $("<option />").text("750m").val("750"),
                    $("<option />").text("1km").val("1000"), $("<option />").text("1,5km").val("1500"), $("<option />")
                            .text("2km").val("2000"), $("<option />").text("5km").val("5000"));
            if (!this.options.extend)
                this.maxWalkInput.val(this.options.defaultMaxWalk);
            maxWalkDiv.append(this.maxWalkInput);
        }
        // Max time
        if (this.options.selectMaxTime) {
            var maxTimeDiv = $("<div/>");
            node.append(maxTimeDiv);
            maxTimeDiv.text("Max time");
            this.maxTimeInput = $("<select/>");
            if (this.options.extend)
                this.maxTimeInput.append($("<option />").text("(same)").val("inherit"));
            this.maxTimeInput.append($("<option />").text("0:30").val("1800"),
                    $("<option />").text("0:45").val("2700"), $("<option />").text("1:00").val("3600"), $("<option />")
                            .text("1:30").val("5400"), $("<option />").text("2:00").val("7200"), $("<option />").text(
                            "2:30").val("9000"), $("<option />").text("3:00").val("10800"));
            if (!this.options.extend)
                this.maxTimeInput.val(this.options.defaultMaxTime);
            maxTimeDiv.append(this.maxTimeInput);
        }
        // Data type (time, boardings, max walk)
        if (this.options.selectDataType && !this.options.extend) {
            var dataTypeDiv = $("<div/>");
            node.append(dataTypeDiv);
            dataTypeDiv.text("Output data");
            this.dataTypeInput = $("<select/>");
            this.dataTypeInput.append($("<option />").text("Time").val("TIME"), $("<option />").text("Boardings").val(
                    "BOARDINGS"), $("<option />").text("Walk distance").val("WALK_DISTANCE"));
            dataTypeDiv.append(this.dataTypeInput);
        }
        // Refresh button
        if (this.options.refreshButton) {
            this.refreshButton = $("<button/>").text("Refresh").click(function() {
                thisRw.refreshCallbacks.fire(thisRw);
            });
            node.append(this.refreshButton);
        }
    },

    /**
     * Show/hide the widget.
     */
    setVisible : function(visible) {
        if (visible)
            this.parentNode.show();
        else
            this.parentNode.hide();
    },

    /**
     * Add a refresh callback, called whenever the widget parameters or the
     * origin position have changed.
     * 
     * @param callback
     */
    onRefresh : function(callback) {
        this.refreshCallbacks.add(callback);
        return this;
    },

    /**
     * @param enable
     *            True to add a position marker, false otherwise.
     */
    enableLocationMarker : function(enable) {
        if (enable) {
            var thisPw = this;
            // TODO This depend on Leaflet, make it more flexible
            this.origMarker = L.marker(this.options.map.getCenter(), {
                draggable : true,
            }).on("dragend", function() {
                thisPw.refreshCallbacks.fire(thisPw);
            }).addTo(this.options.map);
        } else {
            if (this.origMarker) {
                this.options.map.removeLayer(this.origMarker);
                this.origMarker = null;
            }
        }
    },

    /**
     * Force a refresh.
     */
    refresh : function() {
        this.refreshCallbacks.fire(this);
    },

    /**
     * Update the origin position.
     */
    setOrigin : function(latLng) {
        if (this.origMarker)
            this.origMarker.setLatLng(latLng);
    },

    /**
     * @returns The request parameter, compatible to use with a TimeGrid.
     */
    getParameters : function() {
        var base = this.options.extend ? this.options.extend.getParameters() : null;
        var retval = {};
        retval.routerId = this.options.defaultRouterId;
        // Depart at / Arrive by
        retval.arriveBy = this.options.selectDateTime ? this.arriveByInput.val() == "true"
                : this.options.defaultArriveBy;
        if (base && retval.mode == "inherit") {
            retval.arriveBy = base.arriveBy;
        }
        // Position
        var place;
        if (this.origMarker) {
            place = this.origMarker.getLatLng().lat + ',' + this.origMarker.getLatLng().lng;
        } else if (base) {
            place = base.arriveBy ? base.toPlace : base.fromPlace;
        }
        if (retval.arriveBy) {
            retval.toPlace = place;
        } else {
            retval.fromPlace = place;
        }
        // Date/time
        var dateTime;
        var hour = parseInt(this.timeInput.val().split(":")[0]);
        var min = parseInt(this.timeInput.val().split(":")[1]);
        if (base) {
            dateTime = new Date(base.dateTime.getTime() + hour * 3600000 + min * 60000);
            hour = dateTime.getHours();
            min = dateTime.getMinutes();
        } else {
            dateTime = this.options.selectDateTime ? this.dateInput.datepicker("getDate")
                    : this.options.defaultDateTime;
            if (!this.options.selectDateTime) {
                hour = this.options.defaultDateTime.getHour();
                min = this.options.defaultDateTime.getMinute();
            }
            dateTime = new Date(dateTime.getTime());
            dateTime.setHours(hour);
            dateTime.setMinutes(min);
        }
        retval.date = dateTime.getFullYear() + "/" + (dateTime.getMonth() + 1) + "/" + dateTime.getDate();
        retval.time = hour + ":" + (min < 10 ? "0" : "") + min + ":00";
        retval.dateTime = dateTime; // For extending
        // Modes
        retval.mode = this.options.selectModes ? this.modesInput.val() : this.options.defaultModes;
        if (base && retval.mode == "inherit") {
            retval.mode = base.mode;
        }
        // Max walk
        retval.maxWalkDistance = this.options.selectMaxWalk ? this.maxWalkInput.val() : this.options.defaultMaxWalk;
        if (base && retval.maxWalkDistance == "inherit") {
            retval.maxWalkDistance = base.maxWalkDistance;
        }
        retval.maxWalkDistance = parseInt(retval.maxWalkDistance);
        // Max time
        retval.maxTimeSec = this.options.selectMaxTime ? this.maxTimeInput.val() : this.options.defaultMaxTime;
        if (base && retval.maxTimeSec == "inherit") {
            retval.maxTimeSec = base.maxTimeSec;
        }
        if (base) {
            // Force some overrides for comparisons
            retval.precisionMeters = base.precisionMeters;
            retval.zDataType = base.zDataType;
            retval.coordinateOrigin = base.coordinateOrigin;
        } else {
            retval.precisionMeters = this.options.defaultPrecision; // TODO
            // Data type
            retval.zDataType = this.options.selectDataType ? this.dataTypeInput.val() : this.options.defaultDataType;
            retval.coordinateOrigin = this.options.coordinateOrigin.lat + "," + this.options.coordinateOrigin.lng;
        }
        return retval;
    }
});
