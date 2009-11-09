/*
 * Ext JS Library 3.0
 */

/**
 * @class Ext.ux.LinkButton
 * @extends Ext.Button
 * @see http://extjs.com/forum/showthread.php?t=54602
 */
Ext.ux.LinkButton = Ext.extend(Ext.Button, {
    template: new Ext.Template(
        '<table cellspacing="0" class="x-btn {3}"><tbody class="{4}">',
        '<tr><td class="x-btn-tl"><i>&amp;#160;</i></td><td class="x-btn-tc"></td><td class="x-btn-tr"><i>&amp;#160;</i></td></tr>',
        '<tr><td class="x-btn-ml"><i>&amp;#160;</i></td><td class="x-btn-mc"><em class="{5}" unselectable="on"><a href="{6}" target="{7}" class="x-btn-text {2}"><button>{0}</button></a></em></td><td class="x-btn-mr"><i>&amp;#160;</i></td></tr>',
        '<tr><td class="x-btn-bl"><i>&amp;#160;</i></td><td class="x-btn-bc"></td><td class="x-btn-br"><i>&amp;#160;</i></td></tr>',
        '</tbody></table>').compile(),

    buttonSelector : 'a:first',

/* new
   params: {},

    baseParams: {},

    XgetTemplateArgs: function() {
        return [this.text || ' ', this.href, this.target || "_self", this.getHref(), this.target]);
    },
    
    getHref: function() {
        var result = this.href;
        var p = Ext.urlEncode(Ext.apply(Ext.apply({}, this.baseParams), this.params));
        if (p.length) {
            result += ((this.href.indexOf('?') == -1) ? '?' : '&') + p;
        }
        return result;
    },
    setParams: function(p) {
        this.params = p;
        this.el.child(this.buttonSelector, true).href = this.getHref();
    },
*/
    onClick : function(e){
        if(e.button != 0){
            return;
        }
        if(!this.disabled){
            this.fireEvent("click", this, e);
            if(this.handler){
                this.handler.call(this.scope || this, this, e);
            }
        }
    }
});

// Add xtype
Ext.ComponentMgr.registerType('linkbutton', Ext.ux.LinkButton);
