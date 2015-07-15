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

otp.namespace("otp.widgets.tripoptions");

otp.widgets.tripoptions.TripOptionsWidget =
    otp.Class(otp.widgets.Widget, {

    //planTripCallback : null,
    controls : null,
    module : null,

    scrollPanel : null,

    autoPlan : false,

    initialize : function(id, module, options) {

        options = options || {};
        //TRANSLATORS: Widget title
        if(!_.has(options, 'title')) options['title'] = _tr("Travel Options");
        if(!_.has(options, 'cssClass')) options['cssClass'] = 'otp-defaultTripWidget';
        otp.widgets.Widget.prototype.initialize.call(this, id, module, options);

        this.mainDiv.addClass('otp-tripOptionsWidget');

        //this.planTripCallback = planTripCallback;
        this.module = module;

        this.controls = {};
    },

    addControl : function(id, control, scrollable) {

        if(scrollable) {
            if(this.scrollPanel == null) this.initScrollPanel();
            control.$().appendTo(this.scrollPanel);
        }
        else {
            control.$().appendTo(this.$());
        }
        //$("<hr />").appendTo(this.$());
        control.doAfterLayout();
        this.controls[id] = control;
    },

    initScrollPanel : function() {
        this.scrollPanel = $('<div id="'+this.id+'-scollPanel" class="notDraggable" style="overflow: auto;"></div>').appendTo(this.$());
        this.$().resizable({
            minHeight: 80,
            alsoResize: this.scrollPanel
        });
    },

    addSeparator : function(scrollable) {
        var hr = $("<hr />")
        if(scrollable) {
            if(this.scrollPanel == null) this.initScrollPanel();
            hr.appendTo(this.scrollPanel);
        }
        else {
            hr.appendTo(this.$());
        }
    },

    addVerticalSpace : function(pixels, scrollable) {
        var vSpace = $('<div style="height: '+pixels+'px;"></div>');
        if(scrollable) {
            if(this.scrollPanel == null) this.initScrollPanel();
            vSpace.appendTo(this.scrollPanel);
        }
        else {
            vSpace.appendTo(this.$());
        }
    },

    restorePlan : function(data) {
	    if(data == null) return;

	    for(var id in this.controls) {
            this.controls[id].restorePlan(data);
        }
    },

    applyQueryParams : function(queryParams) {
        this.restorePlan({ queryParams : queryParams });
    },

    restoreDefaults : function(useCurrentTime) {
        var params = _.clone(this.module.defaultQueryParams);
        if(useCurrentTime) {
            params['date'] = moment().format(otp.config.locale.time.date_format);
            params['time'] = moment().format(otp.config.locale.time.time_format);
        }
        this.applyQueryParams(params);
    },

    newItinerary : function(itin) {
        for(var id in this.controls) {
            this.controls[id].newItinerary(itin);
        }
    },

    inputChanged : function(params) {
        if(params) _.extend(this.module, params);
        if(this.autoPlan) {
            this.module.planTrip();
        }
    },


    CLASS_NAME : "otp.widgets.TripWidget"
});


//** CONTROL CLASSES **//

otp.widgets.tripoptions.TripOptionsWidgetControl = otp.Class({

    div :   null,
    tripWidget : null,

    initialize : function(tripWidget) {
        this.tripWidget = tripWidget;
        this.div = document.createElement('div');
        //this.div.className()
    },

    setContent : function(content) {
        this.div.innerHTML = content;
    },

    doAfterLayout : function() {
    },

    restorePlan : function(data) {
    },

    newItinerary : function(itin) {
    },

    isApplicableForMode : function(mode) {
        return false;
    },

    $ : function() {
        return $(this.div);
    }
});

//** LocationsSelector **//

otp.widgets.tripoptions.LocationsSelector =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,
    geocoders    :  null,

    activeIndex  :  0,

    initialize : function(tripWidget, geocoders) {
        console.log("init loc");
        this.geocoders = geocoders;

        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-locSelector";

        ich['otp-tripOptions-locations']({
            widgetId : this.id,
            showGeocoders : (this.geocoders && this.geocoders.length > 1),
            geocoders : this.geocoders,
            //TODO: Maybe change to Start and Destination
            start: pgettext('template', "Start"),
            end: _tr("End"),
            geocoder: _tr("Geocoder")
        }).appendTo(this.$());

        this.tripWidget.module.on("startChanged", $.proxy(function(latlng, name) {
            $("#"+this.id+"-start").val(name || '(' + latlng.lat.toFixed(5) + ', ' + latlng.lng.toFixed(5) + ')');
        }, this));

        this.tripWidget.module.on("endChanged", $.proxy(function(latlng, name) {
            $("#"+this.id+"-end").val(name || '(' + latlng.lat.toFixed(5) + ', ' + latlng.lng.toFixed(5) + ')');
        }, this));

    },

    doAfterLayout : function() {
        var this_ = this;

        this.startInput = this.initInput($("#"+this.id+"-start"), this.tripWidget.module.setStartPoint);
        this.endInput = this.initInput($("#"+this.id+"-end"), this.tripWidget.module.setEndPoint);


        $("#"+this.id+"-startDropdown").click($.proxy(function() {
            $("#"+this.id+"-start").autocomplete("widget").show();
        }, this));

        $("#"+this.id+"-endDropdown").click($.proxy(function() {
            $("#"+this.id+"-end").autocomplete("widget").show();
        }, this));


        $("#"+this.id+"-reverseButton").click($.proxy(function() {
            var module = this.tripWidget.module;
            var startLatLng = module.startLatLng, startName = module.startName;
            var endLatLng = module.endLatLng, endName = module.endName;
            module.clearTrip();
            module.setStartPoint(endLatLng, false, endName);
            module.setEndPoint(startLatLng, false, startName);
            this_.tripWidget.inputChanged();

        }, this));

        if(this.geocoders.length > 1) {
            var selector = $("#"+this.id+"-selector");
            selector.change(function() {
                this_.activeIndex = this.selectedIndex;
            });
        }
    },

    initInput : function(input, setterFunction) {
        var this_ = this;
        input.autocomplete({
            delay: 500, // 500ms between requests.
            source: function(request, response) {
                this_.geocoders[this_.activeIndex].geocode(request.term, function(results) {
                    console.log("got results "+results.length);
                    response.call(this, _.pluck(results, 'description'));
                    input.data("results", this_.getResultLookup(results));
                });
            },
            select: function(event, ui) {
                var result = input.data("results")[ui.item.value];
                var latlng = new L.LatLng(result.lat, result.lng);
                this_.tripWidget.module.webapp.map.lmap.panTo(latlng);
                setterFunction.call(this_.tripWidget.module, latlng, false, result.description);
                this_.tripWidget.inputChanged();
            },
        })
        .dblclick(function() {
            $(this).select();
        });
        return input;
    },

    getResultLookup : function(results) {
        var resultLookup = {};
        for(var i=0; i<results.length; i++) {
            resultLookup[results[i].description] = results[i];
        }
        return resultLookup;
    },

    restorePlan : function(data) {
        if(data.queryParams.fromPlace) {
            console.log("rP: "+data.queryParams.fromPlace);
            var fromName = otp.util.Itin.getLocationName(data.queryParams.fromPlace);
            if(fromName) {
                $("#"+this.id+"-start").val(fromName);
                this.tripWidget.module.startName = fromName;
            }
        }
        else {
            $("#"+this.id+"-start").val('');
            this.tripWidget.module.startName = null;
        }

        if(data.queryParams.toPlace) {
            var toName = otp.util.Itin.getLocationName(data.queryParams.toPlace);
            if(toName) {
                $("#"+this.id+"-end").val(toName);
                this.tripWidget.module.endName = toName;
            }
        }
        else {
            $("#"+this.id+"-end").val('');
            this.tripWidget.module.endName = null;
        }
    }

});


//** TimeSelector **//

otp.widgets.tripoptions.TimeSelector =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id          :  null,
    epoch       : null,

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-timeSelector";

        ich['otp-tripOptions-timeSelector']({
            widgetId : this.id,
            //TRANSLATORS: Depart [time dropdown] [date dropdown]. Used in
            //dropdown as a label to choose wanted time/date of departure
            depart   : pgettext("tripoptions", "Depart"),
            //TRANSLATORS: Arrive [time dropdown] [date dropdown]. Used in
            //dropdown as a label to choose wanted time/date of arrival.
            arrive   : _tr("Arrive"),
            //TRANSLATORS: on button that sets time and date of arrival/departure to now
            now      : _tr("Now")
        }).appendTo(this.$());

        this.epoch = moment().unix();
    },

    doAfterLayout : function() {
        var this_ = this;

        $("#"+this.id+'-depArr').change(function() {
            this_.tripWidget.module.arriveBy = (this.selectedIndex == 1);
        });

        $('#'+this.id+'-date').datepicker({
            timeFormat: otp.config.locale.time.time_format_picker,
            onSelect: function(date) {
                this_.tripWidget.inputChanged({
                    date : date,
                });
            }
        });
        $('#'+this.id+'-date').datepicker("setDate", new Date());

        $('#'+this.id+'-time').val(moment().format(otp.config.locale.time.time_format))
        .keyup(function() {
            if(otp.config.locale.time.time_format.toLowerCase().charAt(otp.config.locale.time.time_format.length-1) === 'a') {
                var val = $(this).val().toLowerCase();
                if(val.charAt(val.length-1) === 'm') {
                    val = val.substring(0, val.length-1);
                }
                if(val.charAt(val.length-1) === 'a' || val.charAt(val.length-1) === 'p') {
                    if(otp.util.Text.isNumber(val.substring(0, val.length-1))) {
                        var num = parseInt(val.substring(0, val.length-1));
                        if(num >= 1 && num <= 12) $(this).val(num + ":00" + val.charAt(val.length-1) + "m");
                        else if(num >= 100) {
                            var hour = Math.floor(num/100), min = num % 100;
                            if(hour >= 1 && hour <= 12 && min >= 0 && min < 60) {
                                $(this).val(hour + ":" + (min < 10 ? "0" : "") + min + val.charAt(val.length-1) + "m");
                            }
                        }
                    }
                }
            }
            this_.tripWidget.inputChanged({
                time : $(this).val(),
            });

        });

        $("#"+this.id+'-nowButton').click(function() {
            $('#'+this_.id+'-date').datepicker("setDate", new Date());
            $('#'+this_.id+'-time').val(moment().format(otp.config.locale.time.time_format))
            this_.tripWidget.inputChanged({
                time : $('#'+this_.id+'-time').val(),
                date : $('#'+this_.id+'-date').val()
            });
        });

    },

    getDate : function() {
        return $('#'+this.id+'-date').val();
    },

    getTime : function() {
        return $('#'+this.id+'-time').val();
    },

    restorePlan : function(data) {
        //var m = moment(data.queryParams.date+" "+data.queryParams.time, "MM-DD-YYYY h:mma");
        //$('#'+this.id+'-picker').datepicker("setDate", new Date(m));
        if(data.queryParams.date) {
            $('#'+this.id+'-date').datepicker("setDate", new Date(moment(data.queryParams.date, otp.config.locale.time.date_format)));
            this.tripWidget.module.date = data.queryParams.date;
        }
        if(data.queryParams.time) {
            $('#'+this.id+'-time').val(moment(data.queryParams.time, otp.config.locale.time.time_format).format(otp.config.locale.time.time_format));
            this.tripWidget.module.time = data.queryParams.time;
        }
        if(data.queryParams.arriveBy === true || data.queryParams.arriveBy === "true") {
            this.tripWidget.module.arriveBy = true;
            $('#'+this.id+'-depArr option:eq(1)').prop('selected', true);
        }
        else {
            this.tripWidget.module.arriveBy = false;
            $('#'+this.id+'-depArr option:eq(0)').prop('selected', true);
        }
    }

});


//** WheelChairSelector **//

otp.widgets.tripoptions.WheelChairSelector =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,
    //TRANSLATORS: label for checkbox
    label        : _tr("Wheelchair accesible trip:"),

    initialize : function(tripWidget) {

        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);

        this.id = tripWidget.id;


        ich['otp-tripOptions-wheelchair']({
            widgetId : this.id,
            label : this.label,
        }).appendTo(this.$());

    },

    doAfterLayout : function() {
        var this_ = this;

        $("#"+this.id+"-wheelchair-input").change(function() {
            this_.tripWidget.module.wheelchair = this.checked;
        });
    },

    restorePlan : function(data) {
        if(data.queryParams.wheelchair) {
            $("#"+this.id+"-wheelchair-input").prop("checked", data.queryParams.wheelchair);
        }
    },

    isApplicableForMode : function(mode) {
        //wheelchair mode is shown on transit and walk trips that
        //doesn't include a bicycle
        return (otp.util.Itin.includesTransit(mode)  || mode == "WALK") && !otp.util.Itin.includesBicycle(mode);
    }
});


//** ModeSelector **//

otp.widgets.tripoptions.ModeSelector =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,

    modes        : otp.config.modes,

    optionLookup : null,
    modeControls : null,

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-modeSelector";
        this.modeControls = [];
        this.optionLookup = {};

        //TRANSLATORS: Label for dropdown Travel by: [mode of transport]
        var html = "<div class='notDraggable'>" + _tr("Travel by") + ": ";
        html += '<select id="'+this.id+'">';
        _.each(this.modes, function(text, key) {
            html += '<option>'+text+'</option>';
        });
        html += '</select>';
        html += '<div id="'+this.id+'-widgets" style="overflow: hidden;"></div>';
        html += "</div>";

        $(html).appendTo(this.$());
        //this.setContent(content);
    },

    doAfterLayout : function() {
        var this_ = this;
        $("#"+this.id).change(function() {
            this_.tripWidget.inputChanged({
                mode : _.keys(this_.modes)[this.selectedIndex],
            });
            this_.refreshModeControls();
        });
    },

    restorePlan : function(data) {
        var i = 0;
        for(mode in this.modes) {
            if(mode === data.queryParams.mode) {
                this.tripWidget.module.mode = data.queryParams.mode;
                $('#'+this.id+' option:eq('+i+')').prop('selected', true);
            }
            i++;
        }

        for(i = 0; i < this.modeControls.length; i++) {
            this.modeControls[i].restorePlan(data);
        }
    },

    controlPadding : "8px",

    refreshModeControls : function() {
        var container = $("#"+this.id+'-widgets');
        container.empty();
        var mode = _.keys(this.modes)[document.getElementById(this.id).selectedIndex];
        for(var i = 0; i < this.modeControls.length; i++) {
            var control = this.modeControls[i];
            if(control.isApplicableForMode(mode)) {
                container.append($('<div style="height: '+this.controlPadding+';"></div>'));
                container.append(control.$());
                control.doAfterLayout();
            }
        }
    },

    addModeControl : function(widget) {
        this.modeControls.push(widget);
    }

});


//** MaxWalkSelector **//

otp.widgets.tripoptions.MaxDistanceSelector =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,
    presets      : null,
    distSuffix   : null,

    /**
    * As we want nice presets in both metric and imperial scale, we can't just do a transformation here, we just declare both
    */

    imperialDistanceSuffix: 'mi.',
    metricDistanceSuffix: 'm.',

    initialize : function(tripWidget) {
        var presets;

        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);

        // Set it up the system correctly ones, so we don't need to later on
        if (otp.config.metric) {
            this.presets = presets = this.metricPresets;
            this.distSuffix = this.metricDistanceSuffix;
        } else {
            this.presets = this.imperialPresets;
            this.distSuffix = this.imperialDistanceSuffix;
            presets = [];
            // Transform the presets to miles/meters depending on the metric setting
            for (var i = 0; i < this.presets.length; i++) {
                presets.push((otp.util.Imperial.metersToMiles(this.presets[i])).toFixed(2));
            }
        }
        this.id = tripWidget.id+"-maxWalkSelector";

        // currentMaxDistance is used to compare against the title string of the option element, to select the correct one
        var currentMaxDistance = otp.util.Geo.distanceString(this.tripWidget.module.maxWalkDistance);

        ich['otp-tripOptions-maxDistance']({
            widgetId : this.id,
            presets : presets,
            label : this.label,
            //TRANSLATORS: default value for preset values of maximum walk
            //distances in Trip Options
            presets_label : _tr("Presets"),
            distSuffix: this.distSuffix,
            currentMaxDistance: parseFloat(currentMaxDistance)
        }).appendTo(this.$());

    },

    doAfterLayout : function() {
        var this_ = this;

        $('#'+this.id+'-value').change(function() {
            var meters = parseFloat($(this).val());

            // If inputed in miles transform to meters to change the value
            if (!otp.config.metric) { meters = otp.util.Imperial.milesToMeters(meters); } // input field was in miles

            this_.setDistance(meters);
        });

        $('#'+this.id+'-presets').change(function() {
            var presetVal = this_.presets[this.selectedIndex-1];

            // Save the distance in meters
            this_.setDistance(presetVal);

            if (!otp.config.metric) { presetVal = otp.util.Imperial.metersToMiles(presetVal); } // Output in miles

            // Show the value in miles/meters
            $('#'+this_.id+'-value').val(presetVal.toFixed(2));
            $('#'+this_.id+'-presets option:eq(0)').prop('selected', true);
        });
    },

    restorePlan : function(data) {
        if(!data.queryParams.maxWalkDistance) return;

        var meters = parseFloat(data.queryParams.maxWalkDistance);
        if (isNaN(meters)) { return; }

        if (!otp.config.metric) { meters = otp.util.Imperial.metersToMiles(meters); }

        $('#'+this.id+'-value').val(meters.toFixed(2));
        this.tripWidget.module.maxWalkDistance = parseFloat(data.queryParams.maxWalkDistance);
    },

    setDistance : function(distance) {
        this.tripWidget.inputChanged({
            maxWalkDistance : distance,
        });
    },

});

otp.widgets.tripoptions.MaxWalkSelector =
    otp.Class(otp.widgets.tripoptions.MaxDistanceSelector, {

    // miles (0.1, 0.2, 0.25, 0.3, 0.4, 0.5, 0.75, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5)
    imperialPresets: [160.9344, 321.8688, 402.336, 482.8032, 643.7376, 804.672, 1207.008, 1609.344, 2414.016, 3218.688, 4023.36, 4828.032, 5632.704, 6437.376, 7242.048000000001, 8046.72],

    // meters
    metricPresets      : [100, 200, 300, 400, 500, 750, 1000, 1500, 2000, 2500, 5000, 7500, 10000],

    //TRANSLATORS: label for choosing how much should person's trip on foot be
    label       : _tr("Maximum walk")+":",

    initialize : function(tripWidget) {
        this.id = tripWidget.id+"-maxWalkSelector";
        otp.widgets.tripoptions.MaxDistanceSelector.prototype.initialize.apply(this, arguments);
    },

    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode) && otp.util.Itin.includesWalk(mode);
    },

});

otp.widgets.tripoptions.MaxBikeSelector =
    otp.Class(otp.widgets.tripoptions.MaxDistanceSelector, {

    // miles (0.1, 0.25, 0.5, 0.75, 1, 2, 3, 4, 5, 10, 15, 20, 30, 40, 100)
    imperialPresets: [160.934, 402.335, 804.67, 1207.0049999999999, 1609.34, 3218.68, 4828.0199999999995, 6437.36, 8046.7, 16093.4, 24140.1, 32186.8, 48280.2, 64373.6, 160934],

    // meters
    metricPresets      : [100, 300, 750, 1000, 1500, 2500, 5000, 7500, 10000],

    //TRANSLATORS: label for choosing how much should person's trip on bicycle be
    label       : _tr("Maximum bike")+":",

    initialize : function(tripWidget) {
        this.id = tripWidget.id+"-maxBikeSelector";
        otp.widgets.tripoptions.MaxDistanceSelector.prototype.initialize.apply(this, arguments);
    },

    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode) && otp.util.Itin.includesBicycle(mode);
    },

});

//** PreferredRoutes **//

otp.widgets.tripoptions.PreferredRoutes =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,

    selectorWidget : null,

    lastSliderValue : null,

    initialize : function(tripWidget) {
        var this_ = this;
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-preferredRoutes";

        ich['otp-tripOptions-preferredRoutes']({
            widgetId : this.id,
            //TRANSLATORS: label Preferred Routes: (routes/None)
            preferredRoutes_label: _tr("Preferred Routes"),
            //TRANSLATORS: button to edit Preffered public transport Routes
            edit: _tr("Edit"),
            //TRANSLATORS: Words in brackets when no Preffered public transport route is set
            none : _tr("None"),
            //TRANSLATORS: Label for Weight slider  to set to preffered public
            //transport routes
            weight: _tr("Weight")
        }).appendTo(this.$());

        //TRANSLATORS: widget title
        this.selectorWidget = new otp.widgets.RoutesSelectorWidget(this.id+"-selectorWidget", this, _tr("Preferred Routes"));
    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-button').button().click(function() {
            this_.selectorWidget.updateRouteList();

            this_.selectorWidget.show();
            if(this_.selectorWidget.isMinimized) this_.selectorWidget.unminimize();
            this_.selectorWidget.bringToFront();
        });

        $('#'+this.id+'-weightSlider').slider({
            min : 0,
            max : 120000,
            value : this_.lastSliderValue || 300,
        })
        .on('slidechange', function(evt) {
            this_.lastSliderValue = $(this).slider('value');
            this_.tripWidget.inputChanged({
                otherThanPreferredRoutesPenalty : this_.lastSliderValue,
            });
        });

    },

    setRoutes : function(paramStr, displayStr) {
        this.tripWidget.inputChanged({
            preferredRoutes : paramStr,
        });
        $('#'+this.id+'-list').html(displayStr);
    },

    restorePlan : function(planData) {
        if(planData.queryParams.preferredRoutes) {
            var this_ = this;

            var restoredIds = [];
            var preferredRoutesArr = planData.queryParams.preferredRoutes.split(',');

            // convert the API's agency_name_id format to standard agency_id
            for(var i=0; i < preferredRoutesArr.length; i++) {
                var apiIdArr = preferredRoutesArr[i].split("_");
                var agencyAndId = apiIdArr[0] + "_" + apiIdArr.pop();
                restoredIds.push(agencyAndId);
            }

            this.selectorWidget.restoredRouteIds = restoredIds;
            if(this.selectorWidget.initializedRoutes) this.selectorWidget.restoreSelected();

            this.tripWidget.module.preferredRoutes = planData.queryParams.preferredRoutes;

            // resolve the IDs to user-friendly names
            var indexApi = this.tripWidget.module.webapp.indexApi;
            indexApi.loadRoutes(this, function() {
                var routeNames = [];
                for(var i = 0; i < restoredIds.length; i++) {
                    var route = indexApi.routes[restoredIds[i]].routeData;
                    routeNames.push(route.shortName || route.longName);
                }
                $('#'+this_.id+'-list').html(routeNames.join(', '));
            });

        }
        else { // none specified
            this.selectorWidget.clearSelected();
            this.selectorWidget.restoredRouteIds = [];
            $('#'+this.id+'-list').html('('+_tr("None")+')');
            this.tripWidget.module.preferredRoutes = null;
        }
        if(planData.queryParams.otherThanPreferredRoutesPenalty) {
            this.lastSliderValue = planData.queryParams.otherThanPreferredRoutesPenalty;
            $('#'+this.id+'-weightSlider').slider('value', this.lastSliderValue);
        }
    },

    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode);
    }

});


//** BannedRoutes **//

otp.widgets.tripoptions.BannedRoutes =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,

    selectorWidget : null,

    initialize : function(tripWidget) {
        var this_ = this;
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-bannedRoutes";

        var html = '<div class="notDraggable">';
        //TRANSLATORS: buton edit Banned public transport routes
        var html = '<div style="float:right; font-size: 12px;"><button id="'+this.id+'-button">' + _tr("Edit") + '…</button></div>';
        //TRANSLATORS: label Banned public transport Routes: (routes/None)
        //(Routes you don't want to take)
        html += _tr("Banned routes") + ': <span id="'+this.id+'-list">('+_tr("None")+')</span>';
        html += '<div style="clear:both;"></div></div>';

        $(html).appendTo(this.$());

        //TRANSLATORS: Widget title
        this.selectorWidget = new otp.widgets.RoutesSelectorWidget(this.id+"-selectorWidget", this, _tr("Banned routes"));
    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-button').button().click(function() {
            this_.selectorWidget.updateRouteList();
            this_.selectorWidget.show();
            if(this_.selectorWidget.isMinimized) this_.selectorWidget.unminimize();
            this_.selectorWidget.bringToFront();
        });
    },

    setRoutes : function(paramStr, displayStr) {
        this.tripWidget.inputChanged({
            bannedRoutes : paramStr,
        });
        $('#'+this.id+'-list').html(displayStr);
    },

    restorePlan : function(planData) {
        if(planData.queryParams.bannedRoutes) {
            var this_ = this;

            var restoredIds = [];
            var bannedRoutesArr = planData.queryParams.bannedRoutes.split(',');

            // convert the API's agency_name_id format to standard agency_id
            for(var i=0; i < bannedRoutesArr.length; i++) {
                var apiIdArr = bannedRoutesArr[i].split("_");
                var agencyAndId = apiIdArr[0] + "_" + apiIdArr.pop();
                restoredIds.push(agencyAndId);
            }

            this.selectorWidget.restoredRouteIds = restoredIds;
            if(this.selectorWidget.initializedRoutes) this.selectorWidget.restoreSelected();

            this.tripWidget.module.bannedRoutes = planData.queryParams.bannedRoutes;

            // resolve the IDs to user-friendly names
            var indexApi = this.tripWidget.module.webapp.indexApi;
            indexApi.loadRoutes(this, function() {
                var routeNames = [];
                for(var i = 0; i < restoredIds.length; i++) {
                    var route = indexApi.routes[restoredIds[i]].routeData;
                    routeNames.push(route.shortName || route.longName);
                }
                $('#'+this_.id+'-list').html(routeNames.join(', '));
            });

        }
        else { // none specified
            this.selectorWidget.clearSelected();
            this.selectorWidget.restoredRouteIds = [];
            $('#'+this.id+'-list').html('('+_tr("None")+')');
            this.tripWidget.module.bannedRoutes = null;
        }
    },

    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode);
    }

});


//** BikeTriangle **//

otp.widgets.tripoptions.BikeTriangle =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,
    bikeTriangle :  null,

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-bikeTriangle";

        var content = '';
        //content += '<h6 class="drag-to-change">Drag to Change Trip:</h6>';
        content += '<div id="'+this.id+'" class="otp-bikeTriangle notDraggable"></div>';

        this.setContent(content);
    },

    doAfterLayout : function() {
        if(!this.bikeTriangle) this.bikeTriangle = new otp.widgets.BikeTrianglePanel(this.id);
        var this_ = this;
        this.bikeTriangle.onChanged = function() {
            var formData = this_.bikeTriangle.getFormData();
            this_.tripWidget.inputChanged({
                optimize : "TRIANGLE",
                triangleTimeFactor : formData.triangleTimeFactor,
                triangleSlopeFactor : formData.triangleSlopeFactor,
                triangleSafetyFactor : formData.triangleSafetyFactor,
            });

        };
    },

    restorePlan : function(planData) {
        if(planData.queryParams.optimize === 'TRIANGLE') {
            this.bikeTriangle.setValues(planData.queryParams.triangleTimeFactor,
                                        planData.queryParams.triangleSlopeFactor,
                                        planData.queryParams.triangleSafetyFactor);
        }
    },

    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesAnyBicycle(mode);
    }

});


//** BikeType **//

otp.widgets.tripoptions.BikeType =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id           :  null,

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-bikeType";
        this.$().addClass('notDraggable');

        var content = '';
        //TRANSLATORS: In Bike share planner radio button: <Use>: My Own Bike A shared bike
        content += _tr('Use') + ': ';
        //TRANSLATORS: In Bike share planner radio button: Use: <My Own Bike> A shared bike
        content += '<input id="'+this.id+'-myOwnBikeRBtn" type="radio" name="bikeType" value="my_bike" checked> ' + _tr("My Own Bike") + '&nbsp;&nbsp;';
        //TRANSLATORS: In Bike share planner radio button: Use: My Own Bike <A Shared bike>
        content += '<input id="'+this.id+'-sharedBikeRBtn" type="radio" name="bikeType" value="shared_bike"> ' + _tr("A Shared Bike");

        this.setContent(content);
    },

    doAfterLayout : function() {
        //var module = this.tripWidget.module;
        var this_ = this;
        $('#'+this.id+'-myOwnBikeRBtn').click(function() {
            //module.mode = "BICYCLE";
            //module.planTrip();
            this_.tripWidget.inputChanged({
                mode : "BICYCLE",
            });

        });
        $('#'+this.id+'-sharedBikeRBtn').click(function() {
            //module.mode = "WALK,BICYCLE";
            //module.planTrip();
            this_.tripWidget.inputChanged({
                mode : "WALK,BICYCLE_RENT",
            });
        });
    },

    restorePlan : function(planData) {
        if(planData.queryParams.mode === "BICYCLE") {
            $('#'+this.id+'-myOwnBikeRBtn').attr('checked', 'checked');
        }
        if(planData.queryParams.mode === "WALK,BICYCLE_RENT") {
            $('#'+this.id+'-sharedBikeRBtn').attr('checked', 'checked');
        }
    },

    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesBicycle(mode) && otp.util.Itin.includesWalk(mode);
    }

});


//** TripSummary **//

otp.widgets.tripoptions.TripSummary =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    id  : null,

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-tripSummary";


        var content = '';
        content += '<div id="'+this.id+'-distance" class="otp-tripSummary-distance"></div>';
        content += '<div id="'+this.id+'-duration" class="otp-tripSummary-duration"></div>';
        content += '<div id="'+this.id+'-timeSummary" class="otp-tripSummary-timeSummary"></div>';
        this.setContent(content);
    },

    newItinerary : function(itin) {
    	var dist = 0;

    	for(var i=0; i < itin.legs.length; i++) {
    		dist += itin.legs[i].distance;
        }

        $("#"+this.id+"-distance").html(otp.util.Geo.distanceString(dist));
        $("#"+this.id+"-duration").html(otp.util.Time.secsToHrMin(itin.duration));

        var timeByMode = { };
        for(var i=0; i < itin.legs.length; i++) {
            if(itin.legs[i].mode in timeByMode) {
                timeByMode[itin.legs[i].mode] = timeByMode[itin.legs[i].mode] + itin.legs[i].duration;
            }
            else {
                timeByMode[itin.legs[i].mode] = itin.legs[i].duration;
            }
        }

        var summaryStr = "";
        for(mode in timeByMode) {
            summaryStr += otp.util.Time.secsToHrMin(timeByMode[mode]) + " " + this.getModeName(mode) + " / ";
        }
        summaryStr = summaryStr.slice(0, -3);
        $("#"+this.id+"-timeSummary").html(summaryStr);
    },

    getModeName : function(mode) {
        switch(mode) {
            case 'WALK':
                return "walking";
            case 'BICYCLE':
                return "biking";
        }
        return "n/a";
    }
});


//** AddThis **//

otp.widgets.tripoptions.AddThis =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);

        var content = '';
        content += '<h6 id="share-route-header">Share this Trip:</h6>';
        content += '<div id="share-route"></div>';

        this.setContent(content);
    },

    doAfterLayout : function() {
        // Copy our existing share widget from the header and customize it for route sharing.
        // The url to share is set in PlannerModule.js in the newTrip() callback that is called
        // once a new route is loaded from the server.
        var addthisElement = $(".addthis_toolbox").clone();
        addthisElement.find(".addthis_counter").remove();

        // give this addthis toolbox a unique class so we can activate it alone in Webapp.js
        addthisElement.addClass("addthis_toolbox_route");
        addthisElement.appendTo("#share-route");
        addthisElement.attr("addthis:title", "Check out my trip planned on "+otp.config.siteName);
        addthisElement.attr("addthis:description", otp.config.siteDescription);
    }
});


//** Submit **//

otp.widgets.tripoptions.Submit =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-submit";

        //TRANSLATORS: button to send query for trip planning
        $('<div class="notDraggable" style="text-align:center;"><button id="'+this.id+'-button">' + _tr("Plan Your Trip") + '</button></div>').appendTo(this.$());
        //console.log(this.id+'-button')

    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-button').button().click(function() {
            //this_.tripWidget.pushSettingsToModule();
            if(typeof this_.tripWidget.module.userPlanTripStart == 'function') this_.tripWidget.module.userPlanTripStart();
            this_.tripWidget.module.planTripFunction.apply(this_.tripWidget.module);
        });
    }
});

//** Group Trip **//

otp.widgets.tripoptions.GroupTripOptions =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {


    initialize : function(tripWidget, label) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-groupTripOptions";

        label = label || "Group size: ";
        var html = '<div class="notDraggable">'+label+'<input id="'+this.id+'-value" type="text" style="width:30px;" value="100" />';
        html += "</div>";

        $(html).appendTo(this.$());
    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-value').change(function() {
            //this_.tripWidget.module.groupSize = parseInt($('#'+this_.id+'-value').val());
            this_.tripWidget.inputChanged({
                groupSize : parseInt($('#'+this_.id+'-value').val()),
            });

        });
    },

    restorePlan : function(data) {
        if(_.has(data.queryParams, 'groupSize')) {
            $('#'+this.id+'-value').val(data.queryParams['groupSize']);
            this.tripWidget.module.groupSize = parseInt(data.queryParams['groupSize']);
        }
    },

    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode);
    }
});

/*otp.widgets.TW_GroupTripSubmit =
    otp.Class(otp.widgets.tripoptions.TripOptionsWidgetControl, {

    initialize : function(tripWidget) {
        otp.widgets.tripoptions.TripOptionsWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-gtSubmit";

        $('<div class="notDraggable" style="text-align:center;"><button id="'+this.id+'-button">Plan Trip</button></div>').appendTo(this.$());
        //console.log(this.id+'-button')

    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-button').button().click(function() {
            this_.tripWidget.module.groupTripSubmit();
        });
    }
});*/
