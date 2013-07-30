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
 * for firebug lite -- if we don't have a console loggeravailable, then we add those methods here (and then 
 * any logging calls made w/out firebug.js included will go to stup routes
 */
if(window.console == undefined || window.console.log == undefined)
{
    var names = [
                 "log", "debug", "info", "warn", "error", "assert", "dir", "dirxml",
                 "group", "groupEnd", "time", "timeEnd", "count", "trace", "profile", "profileEnd"
        ];

    window.console = {};
    for (var i = 0; i < names.length; ++i)
        window.console[names[i]] = function(){};
}

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
 * a blank method (with a log warning).
 * 
 * idea is to use in classes where you intend to override a class...use defaultMethod as the initializer
 * that way, you get a warning when you forget to override.  For example:
 * 
 * Class {
 *   myFunction : otp.defaultMethod,
 * }
 * in the above...if this.myFucntion() is not overwritten by customer, I will tell you about it via the defaultMethod.
 */
otp.defaultMethod = function()
{ 
    console.log("NOTE: you're calling a otp.defaultMethod() - please make sure all logic is implemented.");
};


// IMPORTANT: otp.Class & otp.extend was 'borrowed' from OpenLayers.Class & OpenLayers.Util.extend.  
//            Including them here allows otp to run w/out dependence on OpenLayers 
//

/* Code below is Copyright (c) 2006-2008 MetaCarta, Inc., published under the Clear BSD
 * license.  See http://svn.openlayers.org/trunk/openlayers/license.txt for the
 * full text of the license.
 */

/**
 * Constructor: otp.Class
 * Base class used to construct all other classes. Includes support for multiple inheritance.  
 * To create a new otp-style class, use the following syntax:
 * > var MyClass = otp.Class(prototype);
 *
 * To create a new otp-style class with multiple inheritance, use the
 *     following syntax:
 * > var MyClass = otp.Class(Class1, Class2, prototype);
 * Note that instanceof reflection will only reveil Class1 as superclass.
 * Class2 ff are mixins.
 * @class
 */
otp.Class = function() {
    var Class = function() {
        this.initialize.apply(this, arguments);
    };
    var extended = {};
    var parent;
    var initialize;
    var len=arguments.length;
    for(var i=0; i<len; ++i)
    {
        if(typeof arguments[i] == "function")
        {
            // make the class passed as the first argument the superclass
            if(i == 0 && len > 1) {
                // replace the initialize method with an empty function,
                // because we do not want to create a real instance here
                initialize = arguments[i].prototype.initialize;
                arguments[i].prototype.initialize = function() {};
                // the line below makes sure that the new class has a
                // superclass
                extended = new arguments[i];
                // restore the original initialize method
                arguments[i].prototype.initialize = initialize;
            }
            // get the prototype of the superclass
            parent = arguments[i].prototype;
        } else {
            // in this case we're extending with the prototype
            parent = arguments[i];
        }
        otp.extend(extended, parent);
    }

   // decorate new object with otp.js methods
   extended.swapElements = otp.swapElements;

    Class.prototype = extended;
    return Class;
};

/** useful for debugging */
otp.isLocalHost = function() {
    try {
        return document.location.href.indexOf('localhost') > 0;
    } catch(e) {
    }
    return false;
};


/** swap element values with another object */
otp.swapElements = function(object, elementName) {
    if(object == null || elementName == null) return;

    try {
        var tmp = this[elementName];
        this[elementName]   = object[elementName];
        object[elementName] = tmp;
    }
    catch(e) {
        console.log("EXCEPTION otp.swap(): couldn't swap element values named " + elementName + "\n" + e );
    }
};


/**
 * APIFunction: extend
 * 
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
otp.extend = function(destination, source) {
    destination = destination || {};
    if(source) {
        for(var property in source) {
            var value = source[property];
            if(value !== undefined) {
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

        if(!sourceIsEvt
           && source.hasOwnProperty && source.hasOwnProperty('toString')) {
            destination.toString = source.toString;
        }
    }
    return destination;
};


/**
 * like extend, except that the destination must also have a definition of the object to be overridden
 */
otp.override = function(destination, source) {
    destination = destination || {};
    if(source) 
    {
        for(var property in source) 
        {
            var v1 = source[property];
            var v2 = destination[property];
            if(v1 !== undefined && v2 !== undefined) 
            {
                this._copyProp(property, destination, v1);
            }
        }
    }
    return destination;
};


/**
 * just the opposite of override ... add as long as there's no definition of the object to be overridden in the target
 */
otp.inherit = function(destination, source) {
    destination = destination || {};
    if(source) 
    {
        for(var property in source) 
        {
            var v1 = source[property];
            var v2 = destination[property];
            if(v1 !== undefined && v2 == null)
            {
                this._copyProp(property, destination, v1);
            }
        }

        var sourceIsEvt = typeof window.Event == "function" && source instanceof window.Event;
        if(!sourceIsEvt && source.hasOwnProperty && source.hasOwnProperty('toString')) 
        {
            destination.toString = source.toString;
        }
    }
    return destination;
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
 * Clone a given object 
 * 
 * Taken from: http://keithdevens.com/weblog/archive/2007/Jun/07/javascript.clone
 * 
 * @param {Object} obj
 */
otp.clone = function(obj, dest)
{
    if(obj == null || typeof(obj) != 'object' || obj.contentType == "application/xml") 
        return obj;
    
    var copied = dest || {};
    var temp = new obj.constructor(); // changed (twice)
    copied[obj] = temp;
    for(var key in obj)
    {
        if (copied[obj[key]])
          temp[key] = copied[obj[key]];
        else {
          temp[key] = otp.clone(obj[key], copied);
          copied[obj[key]] = temp[key];
        }
    }
    
      return temp;
};

/** private method */
otp._copyProp = function(property, destination, value)
{
    if(property == "CLASS_NAME" || property == "CLAZZ_NAME")
    {   
        var pre = "";
        if(destination.CLASS_INHERITANCE == null)
             destination.CLASS_INHERITANCE = "";
        else
            pre = ";";

        destination.CLASS_INHERITANCE += pre + value;
    }
    else 
    {
        destination[property] = value;
    }
};

