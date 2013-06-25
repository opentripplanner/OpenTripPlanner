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


otp.modules.calltaker.MailablesWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
    
    initialize : function(id, module) {
        this.module = module;

        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            //cssClass : 'otp-calltaker-mailablesWidget',
            title : "Mailables",
            closeable : true,
            persistOnClose : true,
            openInitially : false,            
        });

        ich['otp-calltaker-mailablesWidget']({
            widgetId : this.id,
            mailables : this.module.options.mailables
        }).appendTo(this.mainDiv);
        this.center();

    }
});
