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

otp.namespace("otp.widgets");

otp.widgets.TripWidget = 
    otp.Class(otp.widgets.Widget, {
    
    //planTripCallback : null,
    controls : null,
    module : null,

    scrollPanel : null,
            
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        this.$().addClass('otp-defaultTripWidget');
        
        //this.planTripCallback = planTripCallback;
        this.module = module;
        
        this.controls = { };
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
    
    newItinerary : function(itin) {
        for(var id in this.controls) {
            this.controls[id].newItinerary(itin);
        }
    },
    
    
    CLASS_NAME : "otp.widgets.TripWidget"
});


//** CONTROL CLASSES **//

otp.widgets.TripWidgetControl = otp.Class({
    
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

otp.widgets.TW_LocationsSelector = 
    otp.Class(otp.widgets.TripWidgetControl, {
    
    id           :  null,
    geocoders    :  null,
    
    resultLookup :  null,
    activeIndex  :  0,

    initialize : function(tripWidget, geocoders) {
        console.log("init loc");
        this.geocoders = geocoders;
        
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-locSelector";
        
        var html = '<div style="width: 2.5em; float:left;">';
        html += '<div style="height: 2em;">Start:</div>';
        html += '<div>End:</div>';
        html += "</div>";
        
        html += '<div class="notDraggable" style="margin-left: 2.5em; text-align:right;">';
        html += '<div style="height: 2em;"><input id="'+this.id+'-start" style="width:95%;"></div>';
        html += '<div><input id="'+this.id+'-end" style="width:95%;"></div>';
        html += "</div>";

        html += '<div style="clear:both;"></div>';

        if(geocoders.length > 1) {
            html += '<div style="margin-top:5px;">Geocoder: <select id="'+this.id+'-selector">';
            for(var i=0; i<geocoders.length; i++) {
                html += '<option>'+geocoders[i].name+'</option>';
            
            }
            html += '</select></div>';
        }
         
        $(html).appendTo(this.$());
    },

    doAfterLayout : function() {
        var this_ = this;
        
        var startInput = $("#"+this.id+"-start");
        console.log("startInput "+startInput);
        this.initInput(startInput, this.tripWidget.module.setStartPoint);
        this.initInput($("#"+this.id+"-end"), this.tripWidget.module.setEndPoint);

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
            source: function(request, response) {
                this_.geocoders[this_.activeIndex].geocode(request.term, function(results) {
                    response.call(this, _.pluck(results, 'description'));
                    this_.updateResultLookup(results);
                });
            },
            select: function(event, ui) {
                var result = this_.resultLookup[ui.item.value];
                var latlng = new L.LatLng(result.lat, result.lng);
                this_.tripWidget.module.webapp.map.lmap.panTo(latlng);
                setterFunction.call(this_.tripWidget.module, latlng, true, result.description);
            }
        })
        .click(function() {
            $(this).select();
        })
        .change(function() {
        });
    },
    
    updateResultLookup : function(results) {
        this.resultLookup = {};
        for(var i=0; i<results.length; i++) {
            this.resultLookup[results[i].description] = results[i];
        }    
    },
    
    restorePlan : function(data) {
        var fromName = otp.util.Itin.getLocationName(data.queryParams.fromPlace);
        if(fromName) {
            $("#"+this.id+"-start").val(fromName);
            this.tripWidget.module.startName = fromName;
        }

        var toName = otp.util.Itin.getLocationName(data.queryParams.toPlace);
        if(toName) {
            $("#"+this.id+"-end").val(toName);
            this.tripWidget.module.endName = toName;
        }
    }    
        
});


//** TimeSelector **//

otp.widgets.TW_TimeSelector = 
    otp.Class(otp.widgets.TripWidgetControl, {
    
    id          :  null,
    epoch       : null,   
    
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-timeSelector";

        var html = '<div id="'+this.id+'" class="notDraggable">';

        var depArrId = this.id+'-depArr';
        html += '<select id="'+depArrId+'">';
        html += '<option>Depart</option>';
        html += '<option>Arrive</option>';
        html += '</select>';
        
        var inputId = this.id+'-picker';
        html += '&nbsp;<input type="text" name="'+inputId+'" id="'+inputId+'" class="otp-datepicker-input" />';
        
        html += '</div>';
        $(html).appendTo(this.$());
    
        this.epoch = moment().unix()*1000;    
    },

    doAfterLayout : function() {
        var this_ = this;

        $("#"+this.id+'-depArr').change(function() {
            this_.tripWidget.module.arriveBy = (this.selectedIndex == 1);
        });


        $('#'+this.id+'-picker').datetimepicker({
            timeFormat: "hh:mmtt", 
            onSelect: function(dateTime) {
                var dateTimeArr = dateTime.split(' ');
                //vare date = 
                this_.tripWidget.module.date = dateTimeArr[0];
                this_.tripWidget.module.time = dateTimeArr[1];
                console.log(dateTime);
                this_.epoch = 1000*moment(dateTime, "MM/DD/YYYY hh:mma").unix();
                console.log(this_.epoch);
            }
        });
        $('#'+this.id+'-picker').datepicker("setDate", new Date());
    },

    restorePlan : function(data) {
        var m = moment(data.queryParams.date+" "+data.queryParams.time, "MM-DD-YYYY h:mma");
        $('#'+this.id+'-picker').datepicker("setDate", new Date(m));
        this.tripWidget.module.date = data.queryParams.date;
        this.tripWidget.module.time = data.queryParams.time;
        if(data.queryParams.arriveBy === true || data.queryParams.arriveBy === "true") {
            this.tripWidget.module.arriveBy = true;
            $('#'+this.id+'-depArr option:eq(1)').prop('selected', true);  
        }
    }
        
});


//** ModeSelector **//

otp.widgets.TW_ModeSelector = 
    otp.Class(otp.widgets.TripWidgetControl, {
    
    id           :  null,

    modes        : { "TRANSIT,WALK" : "Transit", 
                     "BUSISH,WALK" : "Bus Only", 
                     "TRAINISH,WALK" : "Rail Only", 
                     "BICYCLE" : 'Bicycle Only',
                     "WALK" : 'Walk Only',
                     "TRANSIT,BICYCLE" : "Bicycle &amp; Transit" 
                   },
    
    optionLookup : null,
    modeControls : null,
           
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-modeSelector";
        this.modeControls = [];
        this.optionLookup = {};
        
        var html = "<div class='notDraggable'>Travel by: ";
        html += '<select id="'+this.id+'">';
        _.each(this.modes, function(text, key) {
            html += '<option>'+text+'</option>';            
        });
        html += '</select>';
        html += '<div id="'+this.id+'-widgets"></div>';
        html += "</div>";
        
        $(html).appendTo(this.$());
        //this.setContent(content);
    },

    doAfterLayout : function() {
        var this_ = this;
        $("#"+this.id).change(function() {
            this_.tripWidget.module.mode = _.keys(this_.modes)[this.selectedIndex];
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
        console.log("refreshing widgets for mode: "+mode);
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

otp.widgets.TW_MaxWalkSelector = 
    otp.Class(otp.widgets.TripWidgetControl, {
    
    id           :  null,

    presets      : [0.1, 0.2, 0.25, 0.3, 0.4, 0.5, 0.75, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5],
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-maxWalkSelector";
        
        var html = '<div class="notDraggable">Maximum walk: <input id="'+this.id+'-value" type="text" style="width:30px;" value="0.5" /> mi.&nbsp;';
        html += '<select id="'+this.id+'-presets"><option>Presets:</option>';
        for(var i=0; i<this.presets.length; i++) {
            //html += '<option'+(this.values[i] == .5 ? ' selected' : '')+'>'+this.values[i]+'</option>';            
            html += '<option>'+this.presets[i]+' mi.</option>';            
        }
        html += '</select>';
        html += "</div>";
              
        $(html).appendTo(this.$());
        //this.setContent(content);
    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-value').change(function() {
            this_.tripWidget.module.maxWalkDistance = parseFloat($('#'+this_.id+'-value').val())*1609.34;
        });
        
        $('#'+this.id+'-presets').change(function() {
            var presetVal = this_.presets[this.selectedIndex-1];
            $('#'+this_.id+'-value').val(presetVal);    

            var m = presetVal*1609.34;
            this_.tripWidget.module.maxWalkDistance = m;

            $('#'+this_.id+'-presets option:eq(0)').prop('selected', true);    
        });
    },

    restorePlan : function(data) {
        $('#'+this.id+'-value').val(data.queryParams.maxWalkDistance/1609.34);  
        this.tripWidget.module.maxWalkDistance = data.queryParams.maxWalkDistance;
    },
 
    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode) && otp.util.Itin.includesWalk(mode);
    }       
});


//** PreferredRoutes **//

otp.widgets.TW_PreferredRoutes = 
    otp.Class(otp.widgets.TripWidgetControl, {
    
    id           :  null,
    
    selectorWidget : null,
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-preferredRoutes";
        
        var html = '<div class="notDraggable">';
        var html = '<div style="float:right; font-size: 12px;"><button id="'+this.id+'-button">Edit..</button></div>';
        html += 'Preferred Routes: <span id="'+this.id+'-list">(None)</span>';
        html += '<div style="clear:both;"></div></div>';
        
        $(html).appendTo(this.$());
        
        this.selectorWidget = new otp.widgets.PreferredRoutesSelectorWidget(this.id+"-selectorWidget", this);
    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-button').button().click(function() {
            console.log("edit pref rtes");
            if(this.selectorWidget == null) {
                
            }
            this_.selectorWidget.updateRouteList();

            this_.selectorWidget.show();
            this_.selectorWidget.bringToFront();
        });
    },

    setRoutes : function(paramStr, displayStr) {
        this.tripWidget.module.preferredRoutes = paramStr;
        $('#'+this.id+'-list').html(displayStr);
    },
    
    restorePlan : function(planData) {
        if(planData.queryParams.preferredRoutes) {
            var this_ = this;
            this.selectorWidget.restoredRouteIds = planData.queryParams.preferredRoutes;
            this.tripWidget.module.preferredRoutes = planData.queryParams.preferredRoutes;
            
            // resolve the IDs to user-friendly names
            var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/routes';
            this.currentRequest = $.ajax(url, {
                dataType:   'jsonp',
                    
                success: function(ajaxData) {
                    var displayStr = '', count = 0;
                    var routeIdArr = planData.queryParams.preferredRoutes.split(',');
                    for(var i = 0; i < ajaxData.routes.length; i++) {
                        var route = ajaxData.routes[i];
                        var combinedId = route.id.agencyId+"_"+route.id.id;
                        if(_.contains(routeIdArr, combinedId)) {
                            displayStr += (route.routeShortName || route.routeLongName);
                            if(count < routeIdArr.length-1) displayStr += ", ";
                            count++;
                        }
                    }
                    $('#'+this_.id+'-list').html(displayStr);
                }
            });            
        }
    },
    
    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode);
    }      
        
});


//** BikeTriangle **//

otp.widgets.TW_BikeTriangle = 
    otp.Class(otp.widgets.TripWidgetControl, {
    
    id           :  null,
    bikeTriangle :  null,
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
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
            this_.tripWidget.module.triangleTimeFactor = formData.triangleTimeFactor;
            this_.tripWidget.module.triangleSlopeFactor = formData.triangleSlopeFactor;
            this_.tripWidget.module.triangleSafetyFactor = formData.triangleSafetyFactor;
            this_.tripWidget.module.planTrip();
        };
    },

    restorePlan : function(data) {
        if(data.optimize === 'TRIANGLE') {
            this.bikeTriangle.setValues(data.triangleTimeFactor, data.triangleSlopeFactor, data.triangleSafetyFactor);
        }
    },
    
    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesBicycle(mode);
    }      
        
});


//** BikeType **//

otp.widgets.TW_BikeType = 
    otp.Class(otp.widgets.TripWidgetControl, {

    id           :  null,
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-bikeType";

        var content = '';        
        content += 'Use: ';
        content += '<input id="'+this.id+'-myOwnBikeRBtn" type="radio" name="bikeType" value="my_bike" checked> My Own Bike&nbsp;&nbsp;';
        content += '<input id="'+this.id+'-sharedBikeRBtn" type="radio" name="bikeType" value="shared_bike"> A Shared Bike';
        
        this.setContent(content);
    },

    doAfterLayout : function() {
        var module = this.tripWidget.module;
        $('#'+this.id+'-myOwnBikeRBtn').click(function() {
            module.mode = "BICYCLE";
            module.planTrip();
        });
        $('#'+this.id+'-sharedBikeRBtn').click(function() {
            module.mode = "WALK,BICYCLE";
            module.planTrip();
        });
    },
    
    restorePlan : function(data) {
        if(data.mode === "BICYCLE") {
            $('#'+this.id+'-myOwnBikeRBtn').attr('checked', 'checked');
        }
        if(data.mode === "WALK,BICYCLE") {
            $('#'+this.id+'-sharedBikeRBtn').attr('checked', 'checked');
        }
    },
    
    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesBicycle(mode) && otp.util.Itin.includesWalk(mode);
    }    
        
});


//** TripSummary **//

otp.widgets.TW_TripSummary = 
    otp.Class(otp.widgets.TripWidgetControl, {
       
    id  : null,
    
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
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
    	
        $("#"+this.id+"-distance").html(Math.round(100*(dist/1609.344))/100+" mi.");
        $("#"+this.id+"-duration").html(otp.util.Time.msToHrMin(itin.duration));	
        
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
            summaryStr += otp.util.Time.msToHrMin(timeByMode[mode]) + " " + this.getModeName(mode) + " / ";
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

otp.widgets.TW_AddThis = 
    otp.Class(otp.widgets.TripWidgetControl, {
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        
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

otp.widgets.TW_Submit = 
    otp.Class(otp.widgets.TripWidgetControl, {
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-submit";

        $('<div class="notDraggable" style="text-align:center;"><button id="'+this.id+'-button">Plan Trip</button></div>').appendTo(this.$());
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

otp.widgets.TW_GroupTripOptions = 
    otp.Class(otp.widgets.TripWidgetControl, {

       
    initialize : function(tripWidget, label) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-groupTripOptions";
        
        label = label || "Group size: ";
        var html = '<div class="notDraggable">'+label+'<input id="'+this.id+'-value" type="text" style="width:30px;" value="100" />';
        html += "</div>";
              
        $(html).appendTo(this.$());
    },

    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-value').change(function() {
            console.log("new groupSize");
            this_.tripWidget.module.groupSize = parseInt($('#'+this_.id+'-value').val());
        });
    },

    restorePlan : function(data) {
        if(_.has(data.queryParams, 'groupSize')) {
            $('#'+this.id+'-value').val(data.queryParams['groupSize']);
        }
    },
 
    isApplicableForMode : function(mode) {
        return otp.util.Itin.includesTransit(mode);
    }       
});

/*otp.widgets.TW_GroupTripSubmit = 
    otp.Class(otp.widgets.TripWidgetControl, {
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetControl.prototype.initialize.apply(this, arguments);
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
