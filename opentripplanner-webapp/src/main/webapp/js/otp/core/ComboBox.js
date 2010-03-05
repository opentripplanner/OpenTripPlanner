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
 * Simple Ext Text form with Cookie backed memory
 * @class
 */
otp.core.ComboBox = {

    m_form       : null,
    m_store      : null,
    m_template   : null,
    maxNumValues : 10,
    id           : 'cb.id',
    name         : 'cb.name',
    label        : 'Form',
    display      : 'display',
    emptyText    : '...',
		cls					 : '',

    /**
     * constructor of sorts
     */
    initialize : function(config)
    {
        console.log("enter Text constructor");
        otp.configure(this, config);

        this.m_store = new Ext.data.SimpleStore({
            fields: [this.display],
            data: Ext.state.Manager.get(this.id, [])
        });

        this.m_template = new Ext.XTemplate(
            '<tpl for=".">',
            '<div class="x-combo-list-item" >',
              '{' + this.display + '}',
            '</div>',
            '</tpl>'
        );

        this.m_form  = new Ext.form.ComboBox({
            id:            this.id,
            hiddenName:    this.name,
            fieldLabel:    this.label,
            displayField:  this.display,
            tpl:           this.m_template,
            emptyText:     this.emptyText,
            valueNotFoundText: '',
            store:         this.m_store,
            mode:          'local',
            anchor:        '95%',
            triggerAction: 'all',
            allowBlank:    false,
            typeAhead:     false,
            resizable:     true,
            maxHeight :    150,
            lazyRender:    false,
            selectOnFocus: true,
						hideLabel: true,
						cls: this.cls
        });
    },

    /**
     * persist Ext ComboBox's text field content into a Cookie 
     */
    persist : function(text) 
    {
        // either use passed in text
        if(Ext.isEmpty(text))
        {
            // or use form's raw value to persist
            text = this.getRawValue();
            if(Ext.isEmpty(text))
                return;
        }

        // and check to see if this text value already exists in the cache
        this.m_store.clearFilter(false);

        // note: this find method will not allow prefixes to be added (eg: '2' will fail if there's already a value of '2155')
        var ff = this.m_store.find(this.display, text, 0, false, true);
        if(ff < 0) 
        {
            var data = [[text]];
            var count = this.m_store.getTotalCount();
            var limit = count > this.maxNumValues ? this.maxNumValues - 1 : count;

            for (var i = 0; i < limit; i++)
                data.push([this.m_store.getAt(i).get(this.display)]);

            // the value doesn't yet exist here, so store it
            var p = Ext.state.Manager.getProvider();
            if(p)
            {
                p.expires = otp.util.DateUtils.addDays(365);
                Ext.state.Manager.set(this.id, data); 
            }

            this.m_store.loadData(data);
        }

        // here for debug purposes
        ff = null;
    },

    /** return ExtComboBox */
    getComboBox : function()
    {
        return this.m_form;
    },
    
    /** return ExtComboBox's current text value */
    getRawValue : function()
    {
        return this.m_form.getRawValue();
    },
    
    /** set the value of this combo box's text */
    setRawValue : function(val)
    {
        this.m_form.setRawValue(val);
    },
    
    /** TODO allow the form to load it's store from another store, in combo with a template */
    load : function(store, template)
    {
        
    },

    clear : function()
    {
        this.m_form.collapse();
        this.m_form.reset();
    },

    collapse : function()
    {
        this.m_form.collapse();
    },

    /** */
    reverse : function(combo)
    {
        try
        {
            var tmp = this.getRawValue();
            this.setRawValue(combo.getRawValue());
            combo.setRawValue(tmp);
        }
        catch(e)
        {}
    },
    
    CLASS_NAME : "otp.core.ComboBox"
};
otp.core.ComboBox = new otp.Class(otp.core.ComboBox);