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

otp.namespace("otp.planner");

/**
 * otp/planner/FormsOptionsManager.js 
 * 
 * Purpose is to manage the options for the trip planner input forms.
 * 
 * The options are dependent on each other, and this class will manage
 * how the changes are propagated.
 * 
 */
otp.planner.FormsOptionsManager = {
        
    // form option vars
    // these are ext combo boxes
    mode:          null,
    optimize:      null,
    maxWalk:       null,
    wheelchair:    null,
    locale:        null,
    bikeTriangle:  null,
    bikeDefault:   "TRIANGLE",

    // the optimize store is used to control the optimize options
    optimizeStore: null,
        
    initialize : function(config) {
        otp.configure(this, config);

        // we're also interested in the optimize options store
        // to filter some of the options
        this.optimizeStore = this.optimize.getStore();

        // add event handlers to the options that can affect others
        this.mode.on({scope: this, select: this.modeSelected});

        this.optimize.on({scope: this, select: this.optSelected});

        // initially, we are set to transit mode (we assume here anyway)
        // we'll set up the initial options/state accordingly
        this.optimizeStore.filterBy(function(record) {
            return record.get("opt") !== "SAFE";
        });
    },

    modeSelected : function(comboBox, record, idx) {
        var mode = record.get("opt");
        
        this.optimizeStore.clearFilter();
        var showTransitOptions = false;
        var showBikeOptions = false;
        
        // if we're neither a bike nor a transit mode
        // then we don't show the optimize options at all
        // because it would just be quickest trip
        // we show it by default here if it's not visible
        // and it gets hidden below if necessary
        if (this.optimize.hidden) {
            this.showComboBox(this.optimize);
        }

        // and we want to reset the optimize option too
        // because it's possible that it's no longer valid
        this.optimize.reset();

        if (this.isTransitOrBus(mode)) {
            if (this.isBike(mode)) {
                this.maxWalk.label.update(this.locale.tripPlanner.labels.maxBikeDistance);
            } else {
                this.maxWalk.label.update(this.locale.tripPlanner.labels.maxWalkDistance);
            }
            this.showComboBox(this.maxWalk);
            this.showComboBox(this.wheelchair);
            showTransitOptions = true;
        } else {
            this.hideComboBox(this.maxWalk);
            this.hideComboBox(this.wheelchair);
        }
        if (this.isBike(mode)) {
            showBikeOptions = true;
        }

        // we don't display the combo box at all in this case
        if(!showTransitOptions && !showBikeOptions) {
            this.hideComboBox(this.optimize);
        }
  
        this.optimizeStore.filterBy(this.getOptimizeFilter(showTransitOptions, showBikeOptions));


        this.bikeTriangle.disable();

        // change bike options
        if(showBikeOptions) {
            this.optimize.setValue(this.bikeDefault);
            if(this.bikeDefault == "TRIANGLE") {
                this.bikeTriangle.enable();
            }
        }
    },

    getOptimizeFilter : function(record) {
        var mode = record.get("opt");
        return this.getOptimizeFilter(this.isTransitOrBus(mode), this.isBike(mode));  
    },
    
    getOptimizeFilter : function(showTransitOptions, showBikeOptions) {
        var optimizeFilter;        

        // we don't have many permutations of filters currently
        // so a naive approach seems sufficient
        if (showTransitOptions && showBikeOptions) {
            optimizeFilter = function(record) {
            	return true;
            };
        } else if (showTransitOptions) {
            optimizeFilter = function(record) {
                return record.get("opt") !== "SAFE" && record.get("opt") !== "TRIANGLE";
            };
        } else if (showBikeOptions) {
            optimizeFilter = function(record) {
                return record.get("opt") !== "TRANSFERS";
            };
        } else {
            optimizeFilter = function(record) {
                var opt = record.get("opt");
                return !(opt === "TRANSFERS" ||
                         opt === "SAFE");
            };
        }
        return optimizeFilter;
    },
    
    showComboBox : function(cb) {
        cb.show();
        cb.label.show();
    },
    
    hideComboBox : function(cb) {
        cb.reset();
        cb.hide();
        cb.label.hide();
    },
    
    isTransitOrBus : function(mode) {
        return mode.indexOf("TRANSIT") !== -1 ||
               mode.indexOf("TRAINISH") !== -1 ||
               mode.indexOf("BUSISH") !== -1;
    },

    isBike : function(mode) {
        return mode.indexOf("BICYCLE") !== -1;
    },
    
    optSelected : function(comboBox, record, idx) {
    	  var optimize = record.get("opt");
        if(optimize.indexOf("TRIANGLE") !== -1) this.bikeTriangle.enable();
        else this.bikeTriangle.disable();
    },
    	   
    CLASS_NAME: "otp.planner.FormsOptionsManager"

};

otp.planner.FormsOptionsManager = new otp.Class(otp.planner.FormsOptionsManager);
