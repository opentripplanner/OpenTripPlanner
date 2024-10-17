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

otp.namespace("otp.util");

/**
 * Utility routines for text/string operations
 */
 
otp.util.Text = {

    capitalizeFirstChar : function(str) {
        return str.charAt(0).toUpperCase() + str.slice(1);
    },
    
    ordinal : function(n) {
        var ordinals = {
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            1: _tr('first'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            2: _tr('second'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            3: _tr('third'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            4: _tr('fourth'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            5: _tr('fifth'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            6: _tr('sixth'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            7: _tr('seventh'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            8: _tr('eight'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            9: _tr('ninth'),
            //TRANSLATORS: Take roundabout to [nth] exit on [streetname] used
            //as ordinal_exit number parameter for roundabouts
            10: _tr('tenth')
        };
        if (n in ordinals) {
            return ordinals[n];
        } else {
            return n;
        }
    },
    
    isNumber : function(str) {
        return !isNaN(parseFloat(str)) && isFinite(str);
    },
    
    endsWith : function(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    },
    
    constructUrlParamString : function(params) {
        var encodedParams = [];
        for(param in params) {
            encodedParams.push(param+"="+ encodeURIComponent(params[param]));
        }
        return encodedParams.join("&");
    },


    // LZW functions adaped from jsolait library (LGPL)
    // via http://stackoverflow.com/questions/294297/javascript-implementation-of-gzip
    
    // LZW-compress a string
    lzwEncode : function(s) {
        var dict = {};
        var data = (s + "").split("");
        var out = [];
        var currChar;
        var phrase = data[0];
        var code = 256;
        for (var i=1; i<data.length; i++) {
            currChar=data[i];
            if (dict[phrase + currChar] != null) {
                phrase += currChar;
            }
            else {
                out.push(phrase.length > 1 ? dict[phrase] : phrase.charCodeAt(0));
                dict[phrase + currChar] = code;
                code++;
                phrase=currChar;
            }
        }
        out.push(phrase.length > 1 ? dict[phrase] : phrase.charCodeAt(0));
        for (var i=0; i<out.length; i++) {
            out[i] = String.fromCharCode(out[i]);
        }
        return out.join("");
    },

    // Decompress an LZW-encoded string
    lzwDecode : function(s) {
        var dict = {};
        var data = (s + "").split("");
        var currChar = data[0];
        var oldPhrase = currChar;
        var out = [currChar];
        var code = 256;
        var phrase;
        for (var i=1; i<data.length; i++) {
            var currCode = data[i].charCodeAt(0);
            if (currCode < 256) {
                phrase = data[i];
            }
            else {
               phrase = dict[currCode] ? dict[currCode] : (oldPhrase + currChar);
            }
            out.push(phrase);
            currChar = phrase.charAt(0);
            dict[code] = oldPhrase + currChar;
            code++;
            oldPhrase = phrase;
        }
        return out.join("");
    }
    
}

