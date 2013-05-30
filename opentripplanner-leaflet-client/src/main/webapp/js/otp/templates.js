
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

otp.namespace("otp.templates");

(function(T, path, $) {


    T.div = '<div {{#id}}id="{{id}}" {{/id}}{{#cssClass}}class="{{cssClass}}" {{/cssClass}}{{#style}}style="{{style}}" {{/style}}/>';

    T.img = 
        '{{#wrapDiv}}<div {{#divId}}id="{{divId}}" {{/divId}}{{#divCssClass}}class="{{divCssClass}}" {{/divCssClass}}{{#divStyle}}style="{{divStyle}}" {{/divStyle}}>{{/wrapDiv}}' +
        '{{#wrapLink}}<a href="{{linkHref}}">{{/wrapLink}}' +
        '<img src="'+path+'{{& src}}" {{#id}}id="{{id}}" {{/id}}{{#cssClass}}class="{{cssClass}}" {{/cssClass}}{{#style}}style="{{style}}" {{/style}}/>' +
        '{{#wrapLink}}</a>{{/wrapLink}}' +
        '{{#wrapDiv}}</div>{{/wrapDiv}}';
        
    T.input = 
        '{{#label}}<div class="otp-defaultLabel">{{label}}</div>{{/label}}' +
        '<input type="text" {{#id}}id="{{id}}" {{/id}}class="{{cssClass}}">{{text}}</textarea>';
        
    T.textarea =
        '{{#label}}<div class="otp-defaultLabel">{{label}}</div>{{/label}}' +
        '<textarea {{#id}}id="{{id}}" {{/id}}class="{{cssClass}}">{{text}}</textarea>';

    T.button =
        '<button>{{text}}</button>';

    T.label =
        '<div class="otp-defaultLabel">{{text}}</div>';
     
})(otp.templates, (otp.config.resourcePath || ''), jQuery);


