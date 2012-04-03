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

            // GET data from SOLR, and place it into the form (and call POI to plot on map)
            var name = record.data['name'];
            var lat  = record.data['lat'];
            var lon  = record.data['lon'];
            this.PARENT.setNameLatLon(name, lat, lon, record.data, true);
            this.PARENT.selectPoiCB(lon, lat, name, true);

            this.collapse();
            console.log("exit SolrComboBox.selectCB " + record + " " + index);
        }
        catch(e)
        {
            console.log("EXCEPTION: SolrComboBox.selectCB " + e);
        }
    },

    /** stub functionality to be overridden */
    selectPoiCB : function(x, y, text, moveMap)
    {
        try
        {
            this.poi.removeIntermediate(this.m_intermediate);
            this.m_intermediate = null;

            if(otp.util.Constants.fromFormID == this.id)
                this.poi.setFrom(x, y, text, moveMap);
            else if(otp.util.Constants.toFormID == this.id)
                this.poi.setTo(x, y, text, moveMap);
            else
                this.m_intermediate = this.poi.addIntermediate(x, y, text, moveMap);
        }
        catch(e)
        {}
    },


    /** SolrComboBox.hoverCB stub (when hover on a SOLR result list item -- good for painting a POI arrow on the map) */
    hoverCB : function(record, index)
    {
        try
        {
            var c = otp.util.ObjUtils.getNamedCoordRecord(record.data, this.poi.isMercator);
            this.hoverPoiCB(c.x, c.y, c.name, false);
        }
        catch(e)
        {
            console.log("EXCEPTION: SolrComboBox.hoverCB " + e);
        }
    },

    /** draw highlighting icon */
    hoverPoiCB : function(x, y, text, moveMap)
    {
        this.poi.removeIntermediate(this.m_intermediate);
        this.m_intermediate = this.poi.addIntermediate(x, y, text, moveMap);
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
        var retVal = otp.util.SolrUtils.makeSolrStore(this.url, {baseParams:{wt:'json', qt:'dismax', rows:this.maxNumValues}});
        return retVal;
    },

    /** Make an Ext search form */
    _makeForm : function() 
    {
        var sel    = 'div.' + this.divID;
        var parent = this;

        var retVal = new Ext.form.ComboBox({
            id:            this.id,
            cls:           this.cls,
            hiddenName:    this.name,
            fieldLabel:    this.label,
            displayField:  this.display,
            msgTarget:     this.msgTarget,
            tpl:           this.template,
            emptyText:     this.emptyText,
            loadingText:   this.locale.indicators.searching,
            valueNotFoundText: '',
            anchor:        this.anchor,
            PARENT:        parent,
            store:         this.store,
            itemSelector:  sel,
            onSelect:      this.selectCB,
            queryParam:    'q',
            minChars:      1,
            pageSize:      10,
            editable:      true,
            typeAhead:     false,
            hideTrigger:   false,
            hideLabel:     true,
            selectOnFocus: true,
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
