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

otp.widgets.Dialogs = {

    showOkDialog : function(message, title) {
        var dialog = ich['otp-okDialog']({
            message : message,
            ok : _tr('OK')
        }).dialog({
            title : title,
            appendTo: 'body',
            modal: true,
            zIndex: 100000,
        });
        
        dialog.find(".okButton").button().click(function() {
            dialog.dialog("close");
        });
    },    
    
    showYesNoDialog : function(message, title, yesCallback, noCallback) {
        var dialog = ich['otp-yesNoDialog']({
            message : message
        }).dialog({
            title : title,
            appendTo: 'body',
            modal: true,
            zIndex: 100000,
        });
        
        dialog.find(".yesButton").button().click(function() {
            if(yesCallback) yesCallback.call(this);
            dialog.dialog("close");
        });

        dialog.find(".noButton").button().click(function() {
            if(noCallback) noCallback.call(this);
            dialog.dialog("close");
        });

    },    
    
    showInputDialog : function(message, title, callback) {
        var dialog = ich['otp-inputDialog']({
            message : message
        }).dialog({
            title : title,
            appendTo: 'body',
            modal: true,
            zIndex: 100000,
        });
        
        //dialog.resizable({ alsoResize: dialog.find('.textarea') });
        
        dialog.find(".okButton").button().click(function() {
            callback.call(this, dialog.find(".textarea").val());
            dialog.dialog("close");
        });

        dialog.find(".cancelButton").button().click(function() {
            dialog.dialog("close");
        });
    },

    showDateDialog : function(message, title, callback) {
        var dialog = ich['otp-dateDialog']({
            message : message
        }).dialog({
            title : title,
            appendTo: 'body',
            modal: true,
            zIndex: 100000,
            height: 300,
            width: 250,
        });
        
        dialog.find(".datepicker").datepicker();

        dialog.find(".okButton").button().click(function() {
            callback.call(this, dialog.find(".datepicker").datepicker('getDate'));
            dialog.dialog("close");
        });

        dialog.find(".cancelButton").button().click(function() {
            dialog.dialog("close");
        });
    },
};
