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

/**
 * This file should be compatible with other OTP client JS code (see otp.js).
 * 
 * TODO Reuse the same file?
 */

var otp = otp || {}; // namespace

/**
 * Set the global locale.
 * 
 * @param locale
 *            The locale to use (eg. otp.locale.English).
 */
otp.setLocale = function(locale) {
    otp.locale = locale;
};

/**
 * Function: namespace Create a namespace given a string, eg:
 * otp.namespace("otp.analyst");
 * 
 * @param ns
 *            {String || Array} A string representing a namespace or an array of
 *            strings representing multiple namespaces. E.g. "some.name.space".
 * @param context
 *            {Object} Optional object to which additional names will be added.
 *            Default is the window object.
 */
otp.namespace = function(ns, context) {
    ns = ns instanceof Array ? ns : [ ns ];
    context = context || window;
    for (var i = 0; i < ns.length; ++i) {
        var parts = ns[i].split('.');
        var base = parts.shift();
        if (typeof context[base] == 'undefined') {
            context[base] = {};
        }
        if (parts.length > 0) {
            otp.namespace([ parts.join('.') ], context[base]);
        }
    }
};

/**
 * Constructor: otp.Class Base class used to construct all other classes.
 * Includes support for multiple inheritance.
 * 
 * To create a new otp-style class, use the following syntax:
 * 
 * <pre>
 * var MyClass = otp.Class(prototype);
 * </pre>
 * 
 * To create a new otp-style class with multiple inheritance, use the following
 * syntax:
 * 
 * <pre>
 * var MyClass = otp.Class(Class1, Class2, prototype);
 * </pre>
 * 
 * Note that instanceof reflection will only reveal Class1 as superclass.
 * 
 */
otp.Class = function() {
    var len = arguments.length;
    var P = arguments[0];
    var F = arguments[len - 1];

    var C = typeof F.initialize == "function" ? F.initialize : function() {
        P.prototype.initialize.apply(this, arguments);
    };

    if (len > 1) {
        var newArgs = [ C, P ].concat(Array.prototype.slice.call(arguments).slice(1, len - 1), F);
        otp.inherit.apply(null, newArgs);
    } else {
        C.prototype = F;
    }
    return C;
};

/**
 * Function: otp.inherit
 * 
 * Parameters: C - {Object} the class that inherits P - {Object} the superclass
 * to inherit from
 * 
 * In addition to the mandatory C and P parameters, an arbitrary number of
 * objects can be passed, which will extend C.
 */
otp.inherit = function(C, P) {
    var F = function() {
    };
    F.prototype = P.prototype;
    C.prototype = new F;
    var i, l, o;
    for (i = 2, l = arguments.length; i < l; i++) {
        o = arguments[i];
        if (typeof o === "function") {
            o = o.prototype;
        }
        otp.Util.extend(C.prototype, o);
    }
};

/**
 * APIFunction: extend Copy all properties of a source object to a destination
 * object. Modifies the passed in destination object. Any properties on the
 * source object that are set to undefined will not be (re)set on the
 * destination object.
 * 
 * Parameters: destination - {Object} The object that will be modified source -
 * {Object} The object with properties to be set on the destination
 * 
 * Returns: {Object} The destination object.
 */
otp.Util = otp.Util || {};
otp.Util.extend = function(destination, source) {
    destination = destination || {};
    if (source) {
        for ( var property in source) {
            var value = source[property];
            if (value !== undefined) {
                destination[property] = value;
            }
        }

        /**
         * IE doesn't include the toString property when iterating over an
         * object's properties with the for(property in object) syntax.
         * Explicitly check if the source has its own toString property.
         */

        /*
         * FF/Windows < 2.0.0.13 reports "Illegal operation on WrappedNative
         * prototype object" when calling hawOwnProperty if the source object is
         * an instance of window.Event.
         */
        var sourceIsEvt = typeof window.Event == "function" && source instanceof window.Event;

        if (!sourceIsEvt && source.hasOwnProperty && source.hasOwnProperty("toString")) {
            destination.toString = source.toString;
        }
    }
    return destination;
};
