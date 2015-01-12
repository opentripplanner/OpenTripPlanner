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
     * @param options
     *            Object containing the various options.
     * 
     */
    initialize : function(node, options) {
        this.options = $.extend({
            defaultRouterId : "",
            selectDateTime : true,
            defaultArriveBy : false,
            defaultDateTime : new Date(),
            dateFormat : "yy/mm/dd",
            selectModes : true,
            defaultModes : "TRANSIT,WALK",
            selectWalkParams : true,
            defaultMaxWalk : 1000,
            defaultWalkSpeed : 1.389,
            defaultBikeSpeed : 4.167,
            coordinateOrigin : null,
            selectMaxTime : false,
            defaultMaxTime : 3600,
            defaultPrecision : 100,
            selectDataType : true,
            defaultDataType : "TIME",
            refreshButton : false
        }, options);
        this.locale = otp.locale.analyst;
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
                locationDiv.text(this.locale.differentOrigin);
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
            this.arriveByInput = this._createSelect(this.locale.arriveDepart, this.options.extend,
                    this.options.defaultArriveBy);
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
        // Mode selector + combo box
        if (this.options.selectModes) {
            var modesDiv = $("<div/>");
            node.append(modesDiv);
            modesDiv.text(this.locale.modesLabel);
            this.modesInput = this._createSelect(this.locale.modes, this.options.extend, this.options.defaultModes,
                    this._modeChanged);
            modesDiv.append(this.modesInput);
            this.transitModesDiv = $("<div/>");
            this.transitModes = [];
            $.each(this.locale.transitModes, function(i, mode) {
                thisRw.transitModes[mode[0]] = true;
                var label = $('<label />', {
                    text : mode[1]
                }).appendTo(thisRw.transitModesDiv);
                $('<input />', {
                    type : 'checkbox',
                    value : mode[0],
                    checked : true
                }).change(function() {
                    thisRw.transitModes[mode[0]] = this.checked;
                }).appendTo(label);
            });
            node.append(this.transitModesDiv);
        }
        // Max walk/bike distance / speed
        if (this.options.selectWalkParams) {
            // Walk
            this.maxWalkDiv = $("<div/>");
            this.maxWalkDiv.text(this.locale.walkLabel);
            node.append(this.maxWalkDiv);
            this.maxWalkInput = this._createSelect(this.locale.maxWalkDistance, this.options.extend,
                    this.options.defaultMaxWalk);
            this.maxWalkDiv.append(this.maxWalkInput);
            this.walkSpeedInput = this._createSelect(this.locale.walkSpeed, this.options.extend,
                    this.options.defaultWalkSpeed);
            this.maxWalkDiv.append(this.walkSpeedInput);
            // Bike
            this.maxBikeDiv = $("<div/>");
            this.maxBikeDiv.text(this.locale.bikeLabel);
            node.append(this.maxBikeDiv);
            this.bikeSpeedInput = this._createSelect(this.locale.bikeSpeed, this.options.extend,
                    this.options.defaultBikeSpeed);
            this.maxBikeDiv.append(this.bikeSpeedInput);
        }
        // Max time
        if (this.options.selectMaxTime) {
            var maxTimeDiv = $("<div/>");
            node.append(maxTimeDiv);
            maxTimeDiv.text(this.locale.maxTimeLabel);
            this.maxTimeInput = this._createSelect(this.locale.maxTime, this.options.extend,
                    this.options.defaultMaxTime);
            maxTimeDiv.append(this.maxTimeInput);
        }
        // Data type (time, boardings, max walk)
        if (this.options.selectDataType && !this.options.extend) {
            this.dataTypeDiv = $("<div/>");
            node.append(this.dataTypeDiv);
            this.dataTypeDiv.text(this.locale.dataTypeLabel);
            this.dataTypeInput = this._createSelect(this.locale.dataType, this.options.extend,
                    this.options.defaultDataType);
            this.dataTypeDiv.append(this.dataTypeInput);
        }
        // Refresh button
        if (this.options.refreshButton) {
            this.refreshButton = $("<button/>").text(this.locale.refresh).click(function() {
                thisRw.refreshCallbacks.fire(thisRw);
            });
            node.append(this.refreshButton);
        }
        this._modeChanged(this);
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
        var hour = parseInt(this.timeInput.val().split(":")[0]);
        var min = parseInt(this.timeInput.val().split(":")[1]);
        if (base) {
            this.dateTime = new Date(this.options.extend.dateTime.getTime() + hour * 3600000 + min * 60000);
            hour = this.dateTime.getHours();
            min = this.dateTime.getMinutes();
        } else {
            this.dateTime = this.options.selectDateTime ? this.dateInput.datepicker("getDate")
                    : this.options.defaultDateTime;
            if (!this.options.selectDateTime) {
                hour = this.options.defaultDateTime.getHour();
                min = this.options.defaultDateTime.getMinute();
            }
            this.dateTime = new Date(this.dateTime.getTime());
            this.dateTime.setHours(hour);
            this.dateTime.setMinutes(min);
        }
        retval.date = this.dateTime.getFullYear() + "/" + (this.dateTime.getMonth() + 1) + "/"
                + this.dateTime.getDate();
        retval.time = hour + ":" + (min < 10 ? "0" : "") + min + ":00";
        // Modes
        retval.mode = this.options.defaultModes;
        if (this.options.selectModes) {
            retval.mode = this.modesInput.val();
            if (retval.mode.indexOf("TRANSIT") > -1) {
                var strModes = "";
                for ( var mode in this.transitModes) {
                    if (this.transitModes[mode])
                        strModes = strModes + mode + ","
                }
                retval.mode = retval.mode.replace("TRANSIT,", strModes);
            }
        }
        if (base && retval.mode == "inherit") {
            retval.mode = base.mode;
        }
        // Max walk
        retval.maxWalkDistance = this.options.selectWalkParams ? this.maxWalkInput.val() : this.options.defaultMaxWalk;
        if (base && retval.maxWalkDistance == "inherit") {
            retval.maxWalkDistance = base.maxWalkDistance;
        }
        retval.maxWalkDistance = parseInt(retval.maxWalkDistance);
        retval.walkSpeed = this.options.selectWalkParams ? this.walkSpeedInput.val() : this.options.defaultWalkSpeed;
        if (base && retval.walkSpeed == "inherit") {
            retval.walkSpeed = base.walkSpeed;
        }
        retval.walkSpeed = parseFloat(retval.walkSpeed);
        // Bike speed
        retval.bikeSpeed = this.options.selectWalkParams ? this.bikeSpeedInput.val() : this.options.defaultBikeSpeed;
        if (base && retval.bikeSpeed == "inherit") {
            retval.bikeSpeed = base.bikeSpeed;
        }
        retval.bikeSpeed = parseFloat(retval.bikeSpeed);
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
    },

    /**
     * Callback when the mode selector changes.
     */
    _modeChanged : function(widget) {
        var modes = widget.modesInput.val();
        var hasWalk = modes.indexOf("WALK") > -1;
        var hasBike = modes.indexOf("BICYCLE") > -1;
        var hasTransit = modes.indexOf("TRANSIT") > -1;
        if (hasWalk) {
            widget.maxWalkDiv.show();
            widget.dataTypeDiv.show();
        } else {
            widget.maxWalkDiv.hide();
            widget.dataTypeDiv.hide();
            widget.dataTypeInput.val("TIME");
        }
        if (hasBike) {
            widget.maxBikeDiv.show();
        } else {
            widget.maxBikeDiv.hide();
        }
        if (hasTransit) {
            widget.transitModesDiv.show();
        } else {
            widget.transitModesDiv.hide();
        }
    },

    /**
     * Create a new <select>, filling it with provided values.
     */
    _createSelect : function(optionsList, inherit, defaultValue, onChangeCallback) {
        var thisWg = this;
        var retval = $("<select/>");
        if (inherit)
            retval.append($("<option />").text(this.locale.inheritValue).val("inherit"));
        for (var i = 0; i < optionsList.length; i++) {
            retval.append($("<option />").text(optionsList[i][1]).val([ optionsList[i][0] ]));
        }
        if (!inherit)
            retval.val(defaultValue);
        if (onChangeCallback)
            retval.change(function() {
                onChangeCallback(thisWg);
            });
        return retval;
    }
});
