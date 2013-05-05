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

otp.namespace("otp.modules.fieldtrip");


otp.modules.fieldtrip.FieldTripModule = 
    otp.Class(otp.modules.planner.PlannerModule, {

    moduleName  : "Field Trip Planner",
    moduleId    : "fieldtrip",
    
    itinWidget  : null,
    
    groupSize   : 100,
    bannedSegments : null,
    //itineraries : null,
    groupPlan : null,
    
    datastoreUrl : otp.config.loggerURL,
    
    userName : "admin",
    password : "secret",
    
    showIntermediateStops : true,
    
    stopsWidget: false,
    
    initialize : function(webapp) {
        otp.modules.planner.PlannerModule.prototype.initialize.apply(this, arguments);
        
        this.planTripFunction = this.ftPlanTrip;
    },

    activate : function() {
        if(this.activated) return;
        console.log("activate "+this.id);
        otp.modules.planner.PlannerModule.prototype.activate.apply(this);

        this.optionsWidget = new otp.widgets.TripWidget('otp-'+this.moduleId+'-optionsWidget', this);
        this.optionsWidget.$().resizable();
        this.addWidget(this.optionsWidget);
        
        this.optionsWidget.minimizable = true;
        this.optionsWidget.addHeader("Trip Options");
        
        if(this.webapp.geocoders && this.webapp.geocoders.length > 0) {
            this.optionsWidget.addControl("locations", new otp.widgets.TW_LocationsSelector(this.optionsWidget, this.webapp.geocoders), true);
            this.optionsWidget.addVerticalSpace(12, true);
        }

        this.optionsWidget.addControl("time", new otp.widgets.TW_TimeSelector(this.optionsWidget), true);
        this.optionsWidget.addVerticalSpace(12, true);
        
        
        var modeSelector = new otp.widgets.TW_ModeSelector(this.optionsWidget);
        this.optionsWidget.addControl("mode", modeSelector, true);

        modeSelector.addModeControl(new otp.widgets.TW_MaxWalkSelector(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.TW_GroupTripOptions(this.optionsWidget, "Number of Students: "));
        //modeSelector.addModeControl(new otp.widgets.TW_BikeTriangle(this.optionsWidget));
        //modeSelector.addModeControl(new otp.widgets.TW_PreferredRoutes(this.optionsWidget));

        modeSelector.refreshModeControls();

        this.optionsWidget.addSeparator();
        //this.optionsWidget.addControl("submit", new otp.widgets.TW_GroupTripSubmit(this.optionsWidget));
        this.optionsWidget.addControl("submit", new otp.widgets.TW_Submit(this.optionsWidget));

        this.fieldTripManager = new otp.modules.fieldtrip.FieldTripManagerWidget('otp-'+this.moduleId+'-fieldTripWidget', this);
        this.widgets.push(this.fieldTripManager);


        this.refreshTrips();
    },
    
    applyParameters : function() {
        if(_.has(this.webapp.urlParams, 'groupSize')) {
            this.groupSize = parseInt(this.webapp.urlParams['groupSize']);
        }        
        if("fromPlace" in this.webapp.urlParams && "toPlace" in this.webapp.urlParams) {
            this.optionsWidget.restorePlan({queryParams : this.webapp.urlParams});
        }
        otp.modules.planner.PlannerModule.prototype.applyParameters.apply(this);
    },    
    
    getExtendedQueryParams : function() {
        return { showIntermediateStops : this.showIntermediateStops };
    },

    getAdditionalUrlParams : function() {
        return { groupSize : this.groupSize };
    },
        
    ftPlanTrip : function() {
        var planDate = moment(this.optionsWidget.controls['time'].epoch).format("YYYY-MM-DD");
        this.currentGroupSize = this.groupSize;
        this.bannedSegments = [];
        console.log("RESET SEGMENTS");
        //this.itineraries = [];
        this.groupPlan = null;

        var this_ = this;
        $.ajax(this.datastoreUrl+'/fieldTrip/getTrips', {
            data: {
                userName : this.userName,
                password : this.password,                
                date : planDate,
                limit : 100,
            },
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                for(var t = 0; t < data.length; t++) {
                    var fieldTrip = data[t];
                    for(var i = 0; i < fieldTrip.groupItineraries.length; i++) {
                        var grpItin = fieldTrip.groupItineraries[i];
                        for(var gt =0 ; gt < grpItin.trips.length; gt++) {
                            //this_.bannedSegments.push(grpItin.trips[gt].tripString);
                        }
                    }
                }
                this_.planTrip();
            },
            
            error: function(data) {
                console.log("error retrieving trips");
                console.log(data);
            }
        });
        this.setBannedTrips();
    },
    
    processPlan : function(tripPlan, restoring) {
        if(this.groupPlan == null)
            this.groupPlan = new otp.modules.planner.TripPlan(null, _.extend(tripPlan.queryParams, { groupSize : this.groupSize }));

        if(this.itinWidget == null) this.createItinerariesWidget();
        /*if(restoring && this.restoredItinIndex) {
            this.itinWidget.updateItineraries(tripPlan, this.restoredItinIndex);
            this.restoredItinIndex = null;
        } else  {
            this.itinWidget.updateItineraries(tripPlan);
        }*/
        //this.itinWidget.updateItineraries(tripPlan);
        //this.itinWidget.show();
        
        /*if(restoring) {
            this.optionsWidget.restorePlan(tripPlan);
        }*/
        //this.drawItinerary(tripPlan.itineraries[0]);
        
        //var itin = new otp.modules.planner.Itinerary(tripPlan.itineraries[0], tripPlan);
        var itin = tripPlan.itineraries[0];
        var capacity = itin.getGroupTripCapacity();
        //console.log("cur grp size:"+this.currentGroupSize+", cap="+capacity);
        
        console.log("FT returned trip:");
        console.log(itin);        
        //this.itineraries.push(itin);
        this.groupPlan.addItinerary(itin);
        
        var segments = itin.getTransitSegments();
        for(var s = 0; s < segments.length; s++) {
            this.bannedSegments.push(segments[s].tripString);
        }

        this.setBannedTrips();// = this.bannedSegments.join(',');     
        console.log("added "+segments.length+" banned segments, total="+this.bannedSegments.length);
        
        if(this.currentGroupSize > capacity) { // group members remain; plan another trip
            this.currentGroupSize -= capacity;
            itin.groupSize = capacity;
            //console.log("remaining: "+this.currentGroupSize);
            this.planTrip();
        }
        else { // we're done; show the results
            itin.groupSize = this.currentGroupSize;
            this.showResults();
        }
        
    },

    showResults : function() {
        this.drawItinerary(this.groupPlan.itineraries[0]);
        //this.itinWidget.updateItineraries(this.itineraries, tripPlan.queryParams);
        this.itinWidget.updatePlan(this.groupPlan);
        this.itinWidget.show();
        this.itinWidget.bringToFront();
    },
    
        
    createItinerariesWidget : function() {
        this.itinWidget = new otp.widgets.ItinerariesWidget(this.moduleId+"-itinWidget", this);
        this.itinWidget.showButtonRow = false;
        this.itinWidget.showItineraryLink = false;
        this.itinWidget.showSearchLink = true;
        this.widgets.push(this.itinWidget);
    },
    
    setBannedTrips : function() {
        var tripIdsOnly = [];
        for(var i=0; i<this.bannedSegments.length; i++) {
            tripIdsOnly.push(this.bannedSegments[i].split(":")[0]);
        }
    
        this.bannedTrips = tripIdsOnly.length > 0 ? tripIdsOnly.join(',') : null;     
        //this.bannedTrips = this.bannedSegments.length > 0 ? this.bannedSegments.join(',') : null;     
        console.log("set bannedTrips: "+this.bannedTrips);
    },

    refreshTrips : function(date) {
        var this_ = this;
        $.ajax(this.datastoreUrl+'/fieldTrip/getTrips', {
            data: {
                userName : this.userName,
                password : this.password,                
                date : this.fieldTripManager.selectedDate,
                limit : 100,
            },
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                this_.fieldTripManager.updateTrips(data);
            },
            
            error: function(data) {
                console.log("error retrieving trips");
                console.log(data);
            }
        });
    },
    
    showSaveWidget : function() {
        new otp.modules.fieldtrip.SaveFieldTripWidget('otp-'+this.moduleId+'-saveFieldTripWidget', this);
    },
        
    saveTrip : function(desc) {
        var this_ = this;
        console.log("saving trip: "+desc);
        console.log(moment(this.optionsWidget.controls['time'].epoch).format("YYYY-MM-DDTHH:mm:ss"));
        
        var data = {
            userName : this.userName,
            password : this.password,
            'trip.origin' : this.getStartOTPString(),
            'trip.destination' : this.getEndOTPString(),
            'trip.description' : desc,
            'trip.createdBy' : this.userName,
            'trip.passengers' : this.groupSize,            
            'trip.departure' : moment(this.optionsWidget.controls['time'].epoch).format("YYYY-MM-DDTHH:mm:ss"),
            'trip.queryParams' : JSON.stringify(this.groupPlan.queryParams),
        };
        
        console.log(data);
        $.ajax(this.datastoreUrl+'/fieldTrip/newTrip', {
            type: 'POST',
            
            data: data,
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                this_.saveItineraries(data);
            },
            
            error: function(data) {
                console.log("error saving trip");
                console.log(data);
            }
        });
    },
    
    
    saveItineraries : function(tripId) {
        var this_ = this;
        this.itinsSaved = 0;
        for(var i = 0; i < this.groupPlan.itineraries.length; i++) {
            var itin = this.groupPlan.itineraries[i];
            //console.log("saving itin for trip "+tripId);
            //console.log(itin);

            var data = {
                userName : this.userName,
                password : this.password,
                fieldTripId : tripId,
                'itinerary.passengers' : itin.groupSize,
                'itinerary.itinData' : otp.util.Text.lzwEncode(JSON.stringify(itin.itinData))
            };
            
            var segments = itin.getTransitSegments()
            for(var s = 0; s < segments.length; s++) {
                var seg = segments[s];
                data['trips['+s+'].depart'] = moment(seg.leg.startTime).format("HH:mm:ss"); 
                data['trips['+s+'].arrive'] = moment(seg.leg.endTime).format("HH:mm:ss"); 
                data['trips['+s+'].tripString'] = seg.tripString; 
                /*for(var si = 0; si < seg.stopIndices.length; si++) {
                    data['itinerary.trips['+s+'].stops['+si+'].stopIndex'] = seg.stopIndices[si];                     
                }*/
            }
            
            $.ajax(this.datastoreUrl+'/fieldTrip/addItinerary', {
                type: 'POST',
                
                data: data,
                    
                success: function(data) {
                    if((typeof data) == "string") data = jQuery.parseJSON(data);
                    //console.log("success saving itinerary");
                    //console.log(data);       
                    this_.itinsSaved++;
                    if(this_.itinsSaved == this_.groupPlan.itineraries.length) {
                        //console.log("all itins saved");
                        this_.refreshTrips();
                    }
                                 
                },
                
                error: function(data) {
                    console.log("error saving itinerary");
                    console.log(data);
                }
            });
                    
        }
    },
    
    deleteTrip : function(trip) {
        var this_ = this;
        
        var data = {
                userName : this.userName,
                password : this.password,
                id : trip.id
        };
        
        console.log("delete");
        console.log(data);
        $.ajax(this.datastoreUrl+'/fieldTrip/deleteTrip', {
            type: 'POST',
            
            data: data,
                  
            success: function(data) {
                this_.refreshTrips();
            },
            
            error: function(data) {
                console.log("error deleting trip:");
                console.log(data);
            }
        });
    },
    
    renderTrip : function(trip) {
        var this_ = this;
        $.ajax(this.datastoreUrl+'/fieldTrip', {
            data: {
                userName : this.userName,
                password : this.password,                
                id : trip.id
            },
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                this_.groupPlan = new otp.modules.planner.TripPlan(null, JSON.parse(data.queryParams));
                for(var i = 0; i < data.groupItineraries.length; i++) {
                    var itinData = JSON.parse(otp.util.Text.lzwDecode(data.groupItineraries[i].itinData));
                    this_.groupPlan.addItinerary(new otp.modules.planner.Itinerary(itinData, this_.groupPlan));
                }
                if(this_.itinWidget == null) this_.createItinerariesWidget();
                this_.showResults();
                var queryParams = JSON.parse(data.queryParams);
                this_.restoreMarkers(queryParams);
                this_.optionsWidget.restorePlan({ queryParams : queryParams });
            },
            
            error: function(data) {
                console.log("error retrieving trip "+trip.id);
                console.log(data);
            }
        });

    },
    
});
