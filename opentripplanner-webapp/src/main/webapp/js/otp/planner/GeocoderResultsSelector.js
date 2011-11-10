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
  * otp/planner/GeocoderResultsSelector.js 
  * 
  * Purpose is to prompt the user to select one of the geocoded results
  * from a list.
  * 
  */
otp.planner.GeocoderResultsSelector = {

    locale: null,
        
    // a callback function gets called after the user has made his selection
    // this function takes a lat, lng, description
    callback: null,
    
    // the format here should be an array of arrays
    // each element should be an array of the form: [lat, lng, description]
    geocoderResults: null,
    
    // ext store holding geocoder results
    // if an appropriate store is passed in, then the geocoder results don't
    // have to be
    store: null,
    
    // grid displaying the geocoder results
    grid: null,
    
    initialize : function(config) {
        otp.configure(this, config);
        var self = this;
        
        if (typeof config.callback === "function") {
            this.callback = config.callback;
        }

        if (!this.store) {
            this.store = new Ext.data.ArrayStore({
                fields : [
                    {name : "lat"},
                    {name : "lng"},
                    {name : "address"}
                ]
            });
            this.store.loadData(this.geocoderResults);
        }
        
        var selectionModel = new Ext.grid.RowSelectionModel({singleSelect: true});
        this.grid = new Ext.grid.GridPanel({
            store: this.store,
            columns: [{
                header: this.locale.tripPlanner.geocoder.address_header,
                id:        "address", 
                dataIndex: "address",
                width: 200}],
            stripeRows: true,
            autoExpandColumn: "address",
            height: 150,
            width: 300,
            selModel: selectionModel
        });
        this.grid.on({
            viewready   : function() {selectionModel.selectFirstRow();},
            rowdblclick : function(g, i, e) {self.resultSelected();},
            rowclick    : function(g, i, e) {self.previewSelected();}
        });
        this.win = new Ext.Window({
            title: this.locale.tripPlanner.geocoder.select_result_title,
            layout: "fit",
            width:  500,
            height: 300,
            x:      50,
            y:      170,
            items: this.grid,
            closable: true,
            buttons: [
                {text: this.locale.buttons.ok,     handler: this.resultSelected.createDelegate(this) },
                {text: this.locale.buttons.cancel, handler: this.hideDialog.createDelegate(this) }
            ]
        });
    },
    
    /**
     * actually display the window to the user prompting for selection
     */
    displayDialog : function() {
        this.win.show();
        this.win.focus();
    },
    
    /** */
    hideDialog : function() {
        this.win.close();
    },

    /** */
    previewSelected : function() {
        var record = this.grid.getSelectionModel().getSelected();

        var lat = record.get("lat");
        var lng = record.get("lng");
        var address = record.get("address");

        this.callback(lat, lng, address);
    },

    /**
     * when the user has selected the geocoded result, retrieve the lat/lng
     * and call the callback function with the values
     */
    resultSelected : function() {
        this.previewSelected();
        this.win.close();
    },
    
    CLASS_NAME: "otp.planner.GeocoderResultsSelector"
};

otp.planner.GeocoderResultsSelector = new otp.Class(otp.planner.GeocoderResultsSelector);
