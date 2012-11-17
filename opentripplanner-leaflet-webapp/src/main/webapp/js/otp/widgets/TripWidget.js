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
    panels : null,
    module : null,
        
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.apply(this, arguments);
        this.$().addClass('otp-defaultTripWidget');
        
        //this.planTripCallback = planTripCallback;
        this.module = module;
        
        this.panels = { };       
    },

    addPanel : function(id, panel) {
        panel.$().appendTo(this.$());
        //$("<hr />").appendTo(this.$());
        panel.doAfterLayout();
        this.panels[id] = panel;
    },
    
    addSeparator : function() {
        $("<hr />").appendTo(this.$());
    },
    
    addVerticalSpace : function(pixels) {
        $('<div style="height: '+pixels+'px;"></div>').appendTo(this.$());
    },

    restorePlan : function(data) {
	    if(data == null) return;

        for(var id in this.panels) {
            this.panels[id].restorePlan(data);
        }
    },
    
    newItinerary : function(itin) {
        for(var id in this.panels) {
            this.panels[id].newItinerary(itin);
        }
    },
    
    
    CLASS_NAME : "otp.widgets.TripWidget"
});


/** PANEL CLASSES */

otp.widgets.TripWidgetPanel = otp.Class({
    
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

    $ : function() {
        return $(this.div);
    }
});


otp.widgets.TW_TripSummary = 
    otp.Class(otp.widgets.TripWidgetPanel, {
       
    id  : null,
    
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
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



otp.widgets.TW_ModeSelector = 
    otp.Class(otp.widgets.TripWidgetPanel, {
    
    id           :  null,

    modes        : { "TRANSIT,WALK" : "Walk to Transit", 
                     "TRANSIT,BICYCLE" : "Bike to Transit", 
                     "WALK" : 'Walk Only',
                     "BICYCLE" : 'Bike Only' },
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-modeSelector";
        
        var html = "<div class='notDraggable'>Travel by: ";
        html += '<select id="'+this.id+'">';
        _.each(this.modes, function(text, key) {
            html += '<option>'+text+'</option>';            
        });
        html += '</select>';
        html += "</div>";
              
        $(html).appendTo(this.$());
        //this.setContent(content);
    },

    doAfterLayout : function() {
        var this_ = this;
        $("#"+this.id).change(function() {
            this_.tripWidget.module.mode = _.keys(this_.modes)[this.selectedIndex];
        });
    },

    restorePlan : function(data) {
    }
        
});

otp.widgets.TW_TimeSelector = 
    otp.Class(otp.widgets.TripWidgetPanel, {
    
    id           :  null,
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
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
                this_.tripWidget.module.date = dateTimeArr[0];
                this_.tripWidget.module.time = dateTimeArr[1];
            }
        });
        $('#'+this.id+'-picker').datepicker("setDate", new Date());
    },

    restorePlan : function(data) {
    }
        
});

otp.widgets.TW_BikeTriangle = 
    otp.Class(otp.widgets.TripWidgetPanel, {
    
    id           :  null,
    bikeTriangle :  null,
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-bikeTriangle";
        
        var content = '';
        content += '<h6 class="drag-to-change">Drag to Change Trip:</h6>';
        content += '<div id="'+this.id+'" class="otp-bikeTriangle notDraggable"></div>';
        
        this.setContent(content);
    },

    doAfterLayout : function() {
        this.bikeTriangle = new otp.widgets.BikeTrianglePanel(this.id);
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
    }
        
});


otp.widgets.TW_BikeType = 
    otp.Class(otp.widgets.TripWidgetPanel, {

    id           :  null,
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
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
    }
        
});



otp.widgets.TW_AddThis = 
    otp.Class(otp.widgets.TripWidgetPanel, {
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
        
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

otp.widgets.TW_Submit = 
    otp.Class(otp.widgets.TripWidgetPanel, {
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
        this.id = tripWidget.id+"-submit";

        $('<div class="notDraggable"><button id="'+this.id+'-button">Plan Trip</button></div>').appendTo(this.$());
        //console.log(this.id+'-button')
        
    },
    
    doAfterLayout : function() {
        var this_ = this;
        $('#'+this.id+'-button').button().click(function() {
            //this_.tripWidget.pushSettingsToModule();
            this_.tripWidget.module.planTrip();
        });
    }
});
