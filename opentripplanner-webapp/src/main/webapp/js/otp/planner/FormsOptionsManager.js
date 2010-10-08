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
    mode:        null,
    optimize:    null,
    maxWalk:     null,
    wheelchair:  null,
    
    // the optimize store is used to control the optimize options
    optimizeStore: null,
        
    initialize : function(config) {
        otp.configure(this, config);
        
        // we're also interested in the optimize options store
        // to filter some of the options
        this.optimizeStore = this.optimize.getStore();

        // add event handlers to the options that can affect others
        this.mode.on({scope: this, select: this.modeSelected});

        // initially, we are set to transit mode (we assume here anyway)
        // we'll set up the initial options/state accordingly
        this.optimizeStore.filterBy(function(record) {
            return record.get("opt") !== "SAFE";
        });
    },

    modeSelected : function(comboBox, record, idx) {
        var mode = record.get("opt");
        
        var optimizeFilter = null;
        this.optimizeStore.clearFilter();
        var showFewestTransfersOption = false;
        var showSafeTripOption = false;
        
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
            this.showComboBox(this.maxWalk);
            showFewestTransfersOption = true;
            this.showComboBox(this.wheelchair);
        } else {
            this.hideComboBox(this.maxWalk);
            this.hideComboBox(this.wheelchair);
        }
        if (this.isBike(mode)) {
            showSafeTripOption = true;
        }

        // we don't have many permutations of filters currently
        // so a naive approach seems sufficient
        if (showFewestTransfersOption && showSafeTripOption) {
            return;
        }
        if (showFewestTransfersOption) {
            optimizeFilter = function(record) {
                return record.get("opt") !== "SAFE";
            };
        } else if (showSafeTripOption) {
            optimizeFilter = function(record) {
                return record.get("opt") !== "TRANSFERS";
            };
        } else {
            optimizeFilter = function(record) {
                var opt = record.get("opt");
                return !(opt === "TRANSFERS" ||
                         opt === "SAFE");
            };
            // we don't display the combo box at all in this case
            // leaving the filter here though just in case we
            this.hideComboBox(this.optimize);
        }
        this.optimizeStore.filterBy(optimizeFilter);
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
   
    CLASS_NAME: "otp.planner.FormsOptionsManager"

};

otp.planner.FormsOptionsManager = new otp.Class(otp.planner.FormsOptionsManager);
