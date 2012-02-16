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

otp.namespace("otp.core");

/**
 * Ext text form (SolrComboBox) for use with a SOLR geo searcher backend 
 * @class
 */
otp.core.SolrComboBoxStatic = {

    url   : '/solr/select',
    divID : 'search-item',

    /**
     * constructor of sorts
     */
    initialize : function(config)
    {
        console.log("enter SolrComboBox constructor");

        // test url if on local host
        if(otp.isLocalHost())
            this.url = '/js/otp/planner/test/solr-geo.json';

        otp.inherit(this, otp.core.ComboBoxStatic);
        // CALLS THE BASE CONSTRUCTOR this.creator(config);
        otp.configure(this, config);


        this.template  = this.template || this._makeTemplate(); 
        this.store     = this._makeStore(); 
        this.form      = this._makeForm();

        console.log("exit SolrComboBox constructor");
    },

    /** SolrComboBox.selectCB stub (when an item from a SOLR result list is selected)  */
    selectCB : function(record, index)
    {
        // IMPORTANT NOTE: you're inside of form callback (not Search)
        try
        {
            console.log("enter SolrComboBox.selectCB " + record + " " + index);

            var name = record.data['name'];
            var lat  = record.data['lat'];
            var lon  = record.data['lon'];
            this.PARENT.setGeocodeName(name, true);
            this.PARENT.setGeocodeCoord(lon + ',' + lat, record.data);
            this.collapse();

            console.log("exit SolrComboBox.selectCB " + record + " " + index);
        }
        catch(e)
        {
            console.log("EXCEPTION: SolrComboBox.selectCB " + e);
        }
    },

    /** */
    hoverCB : function(record, index)
    {
        try
        {
            var coord = otp.utils.ObjUtils.getNamedCoordRecord(record.data, this.poi.isMercator);
            this.poi.setIntermediate(coord['x'], coord['y'], coord['name']);
        }
        catch(e)
        {
            console.log("EXCEPTION: SolrComboBox.selectCB " + e);
        }
    },

    /** SolrComboBox.hoverCB stub (when hover on a SOLR result list item -- good for painting a POI arrow on the map) */
    hoverCB : function(record, index)
    {
    },

    /** SolrComboBox.focusCB stub (when input focus is given to the form) */
    focusCB : function(combo)
    {
    },

    /** SolrComboBox.expandCB stub */
    expandCB : function(combo)
    {
    },


    /** Make an Ext store for SOLR */
    _makeStore : function() 
    {
        var retVal = otp.utils.SolrUtils.makeSolrStore(this.url, {baseParams:{wt:'json', qt:'dismax', rows:this.maxNumValues}});
        return retVal;
    },

    /** Make an Ext search form */
    _makeForm : function() 
    {
        var sel    = 'div.' + this.divID;
        var parent = this;

        var retVal = new Ext.form.ComboBox({
            id:            this.id,
            hiddenName:    this.name,
            store:         this.store,
            queryParam:    'q',
            displayField:  'title',
            itemSelector:  sel,
            minChars:      1,
            fieldLabel:    this.label,
            loadingText:   this.locale.indicators.searching,
            emptyText:     this.locale.indicators.qEmptyText,
            onSelect:      this.selectCB,
            tpl:           this.template,
            PARENT:        parent,
            editable:      true,
            typeAhead:     false,
            hideTrigger:   false,
            selectOnFocus: true,
            anchor:        '100%',
            resizable:     true,
            shadow:        'frame',
            pageSize:      10,
            keys:          {key: [10, 13], handler: function(key) { try { this.expand(); } catch(Ex){}}}
        });

        /** callback: some added ways to get rid of the pesky drop down dialog */
        retVal.on('expand', function(combo) {
            if(combo && combo.view)
            {
                combo.view.on('dblclick',    combo.collapse, this);
                combo.view.on('contextmenu', combo.collapse, this);
                this.PARENT.expandCB(combo);
            }
        });
        retVal.on('focus', function(combo) { 
            try { 
                combo.expand(); 
                this.PARENT.focusCB(combo);
            } 
            catch(Ex){}
        });

        /** hack hover handler */
        retVal.origOnViewOver = retVal.onViewOver; 
        retVal.onViewOver = function(e, t){
            try
            {
                this.origOnViewOver(e, t);
                var item = this.view.findItemFromChild(t);
                if (item) {
                    var index = this.view.indexOf(item);
                    var rec   = this.store.getAt(index);
                    this.PARENT.hoverCB(rec, index);
                }
            }
            catch(e)
            {
            }
        }
        return retVal;
    },

    /** make a geocoder result template, using solr fields to identify things */
    _makeTemplate : function()
    {
        var placeTypeTpl = ''
            + '<tpl if="type_name != null && type_name.length &gt; 0">'
            + ' ({type_name}'
            +   '<tpl if="stop_id != null && stop_id.length &gt; 0">'
            +     ' {stop_id}'
            +   '</tpl>'
            + ')'
            + '</tpl>';
    
        var cityTpl = ''
            + '<tpl if="city != undefined && city.length &gt; 0">'
            + ' {city}'
            + '</tpl>';

        var nameCityType = '{name}' + cityTpl + placeTypeTpl;

        var retVal = new Ext.XTemplate(
            '<tpl for=".">',
            '<div class="' + this.divID + '" >',
              nameCityType,
            '</div>',
            '</tpl>'
        );

        return retVal;
    },

    CLASS_NAME : "otp.core.SolrComboBox"
};
otp.core.SolrComboBox = new otp.Class(otp.core.SolrComboBoxStatic);



/**
 * @class 
 */
otp.utils.SolrUtils = {

    id:    'id',
    total: 'response.numFound',
    root:  'response.docs',

    // SOLR elements
    fields : [
            {name: 'name'}, 
            {name: 'address'},
            {name: 'city'},
            {name: 'url'},

            {name: 'lat'},  {name: 'lon'},
            {name: 'x'},    {name: 'y'},
            {name: 'bbox'}, {name: 'bbox_ospn'}, {name: 'bbox_wgs84'},
            
            {name: 'type'}, {name: 'type_name'}, {name: 'vtype'},

            {name: 'number', type: 'string'},  {name: 'pad_number'}, 
            {name: 'weekday'}, {name: 'saturday'}, {name: 'sunday'},
            {name: 'inbound_name'}, {name: 'outbound_name'},
            {name: 'frequent'},

            {name: 'id'},
            {name: 'zone_id'},
            {name: 'stop_id'},
            {name: 'landmark_id'},
            {name: 'amenities'},
            {name: 'street_direction'},
            {name: 'providers'},
            {name: 'ada_boundary'},
            {name: 'district_boundary'},

            {name: 'spaces'},
            {name: 'routes'},
            {name: 'notes'},
            {name: 'use'}
    ],


    /** */
    solrDataToVectors : function(records, isMercator)
    {
        var retVal = [];
        for(var i = 0; i < records.length; i++)
        {
            var d = records[i].data;
            var x = d.x;
            var y = d.y;
            if(isMercator)
            {
                x = d.lon;
                y = d.lat;
            }

            var p = otp.utils.OpenLayersUtils.makePoint(x, y, isMercator);
            var v = new OpenLayers.Feature.Vector(p, d);
            d.feature = v;
            retVal.push(v);
        }
        return retVal;
    },

    /** get the elements from a SOLR record as an object (array) */
    solrRecordToObject : function(record)
    {
        var data = [];
        var el  = this.fields;
        for(var i in el)
        {
            var n = el[i].name;
            var r = record.get(n);
            if(n != null && r != null)
                data[n] = r;
        }

        return data;
    },

    /** @param layer is an OpenLayer Vector layer, to be used with a Grid Select plugin */
    makeSolrStore : function(url, config)
    {
        if(url == null)
        {
            if(otp.isLocalHost())
                url = '/js/otp/planner/test/solr-geo.json';
            else
                url = '/solr/select';
        }
        return otp.utils.ExtUtils.makeJsonStore(url, this.id, this.total, this.root, this.fields, config);
    },

    CLASS_NAME: "otp.utils.SolrUtils"
};
