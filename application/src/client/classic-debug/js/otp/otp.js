/* Copyright (c) 2006-2012 by OpenLayers Contributors (see authors.txt for 
 * full list of contributors). Published under the 2-clause BSD license.
 * See license.txt in the otp distribution or repository for the
 * full text of the license. */


// define otp, if not already done so...
if(otp == null)
    var otp = {};

otp.CLASS_NAME = "otp";


/**
 * Function: namespace
 * Create a namespace given a string, eg: otp.namespace("otp.widget.ui");
 *
 * Parameters:
 * ns      - {String || Array} A string representing a namespace or an array of strings representing multiple namespaces.  E.g. "some.name.space".
 * context - {Object} Optional object to which additional names will be added.  Default is the window object.
 */
otp.namespace = function(ns, context) 
{
    ns = (typeof ns == 'string') ? [ns] : ns;
    context = context || window;
    var num   = ns.length;
    for(var i=0; i<num; ++i)
    {
        var parts = ns[i].split('.');
        var base  = parts.shift();
        if(typeof context[base] == 'undefined') {
            context[base] = {};
        }
        if(parts.length > 0) {
            otp.namespace([parts.join('.')], context[base]);
        }
    }
};


/**
 * Constructor: otp.Class
 * Base class used to construct all other classes. Includes support for 
 *     multiple inheritance. 

 * To create a new otp-style class, use the following syntax:
 * (code)
 *     var MyClass = otp.Class(prototype);
 * (end)
 *
 * To create a new otp-style class with multiple inheritance, use the
 *     following syntax:
 * (code)
 *     var MyClass = otp.Class(Class1, Class2, prototype);
 * (end)
 * 
 * Note that instanceof reflection will only reveal Class1 as superclass.
 *
 */
 
 
otp.Class = function() {
    var len = arguments.length;
    var P = arguments[0];
    var F = arguments[len-1];

    var C = typeof F.initialize == "function" ?
        F.initialize :
        function(){ P.prototype.initialize.apply(this, arguments); };

    if (len > 1) {
        var newArgs = [C, P].concat(
                Array.prototype.slice.call(arguments).slice(1, len-1), F);
        otp.inherit.apply(null, newArgs);
    } else {
        C.prototype = F;
    }
    return C;
};


/**
 * set member variables in the destination object, as long as that same element is defined in the destination 
 * (and the source has a value other than null).  It might overwrite an existing variable.
 * 
 * @param {Object} destination
 * @param {Object} source
 * @param {Object} getAll == true, we'll assign the value to the desination regarless if desination has an existing slot
 */
otp.configure = function(destination, source, getAll) {

    destination = destination || {};
    if(source)
    {
        for(var property in source)
        {
            if(property == "CLASS_NAME") continue;  // don't override the name property

            var value = source[property];
            var exist = destination[property];

            // copy the variable to the destination as long as it's not null, and the source is either a simple  object or an Ext Template
            if(value != null && typeof value !== 'function' && (exist !== undefined || getAll == true))
            {
                destination[property] = value;
            }

            // copy null value'd attriubtes if we want to 'getAll'
            if(value == null && getAll == true)
            {
                destination[property] = value;
            }
        }

        /**
         * IE doesn't include the toString property when iterating over an object's
         * properties with the for(property in object) syntax.  Explicitly check if
         * the source has its own toString property.
         */

        /*
         * FF/Windows < 2.0.0.13 reports "Illegal operation on WrappedNative
         * prototype object" when calling hawOwnProperty if the source object
         * is an instance of window.Event.
         */

        var sourceIsEvt = typeof window.Event == "function" && source instanceof window.Event;
        if(!sourceIsEvt && source.hasOwnProperty && source.hasOwnProperty('toString')) 
        {
            destination.toString = source.toString;
        }
    }
    return destination;
};


/**
 * Function: otp.inherit
 *
 * Parameters:
 * C - {Object} the class that inherits
 * P - {Object} the superclass to inherit from
 *
 * In addition to the mandatory C and P parameters, an arbitrary number of
 * objects can be passed, which will extend C.
 */
otp.inherit = function(C, P) {
   var F = function() {};
   F.prototype = P.prototype;
   C.prototype = new F;
   var i, l, o;
   for(i=2, l=arguments.length; i<l; i++) {
       o = arguments[i];
       if(typeof o === "function") {
           o = o.prototype;
       }
       otp.Util.extend(C.prototype, o);
   }
};

/**
 * APIFunction: extend
 * Copy all properties of a source object to a destination object.  Modifies
 *     the passed in destination object.  Any properties on the source object
 *     that are set to undefined will not be (re)set on the destination object.
 *
 * Parameters:
 * destination - {Object} The object that will be modified
 * source - {Object} The object with properties to be set on the destination
 *
 * Returns:
 * {Object} The destination object.
 */
otp.Util = otp.Util || {};
otp.Util.extend = function(destination, source) {
    destination = destination || {};
    if (source) {
        for (var property in source) {
            var value = source[property];
            if (value !== undefined) {
                destination[property] = value;
            }
        }

        /**
         * IE doesn't include the toString property when iterating over an object's
         * properties with the for(property in object) syntax.  Explicitly check if
         * the source has its own toString property.
         */

        /*
         * FF/Windows < 2.0.0.13 reports "Illegal operation on WrappedNative
         * prototype object" when calling hawOwnProperty if the source object
         * is an instance of window.Event.
         */

        var sourceIsEvt = typeof window.Event == "function"
                          && source instanceof window.Event;

        if (!sourceIsEvt
           && source.hasOwnProperty && source.hasOwnProperty("toString")) {
            destination.toString = source.toString;
        }
    }
    return destination;
};
