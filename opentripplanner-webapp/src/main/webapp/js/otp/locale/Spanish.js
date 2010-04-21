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

otp.namespace("otp.locale");

/**
  * @class
  */
otp.locale.Spanish = {

    contextMenu : 
    {
        fromHere         : "Salir desde aqu�",
        toHere           : "Llegar hasta aqu�",

        centerHere       : "Centrar mapa aqu�",
        zoomInHere       : "Acercar",
        zoomOutHere      : "Alejar",
        previous         : "�ltimo encuadre",
        next             : "Siguiente encuadre"
    },

    service : 
    {
        weekdays:  "d�as de la semana",
        saturday:  "S�bado",
        sunday:    "Domingo",
        schedule:  "Horario"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Fecha",
        loading    : "Cargando",
        searching  : "Buscando...",
        qEmptyText : "Direcci�n, intersecci�n,  punto de inte�s o Identificador de Parada..."
    },

    buttons: 
    {
        reverse       : "Cambiar",
        reverseTip    : "<b>Camibar or�gen-destino</b><br/>Plan a return trip by reversing this trip's start and end points, and adjusting the time forward.",
        reverseMiniTip: "Cambiar or�gen-destino",

        clear         : "Inicializar",
        clearTip      : "<b>Inicializar</b><br/>Inicializar el mapa y los botones activos.",

        fullScreen    : "Pantalla completa",
        fullScreenTip : "<b>Pantalla completa</b><br/>Mostrar - ocultar paneles",
        
        print         : "Imprimir",
        printTip      : "<b>Imprimir</b><br/>Imprimir este plan de viaje junto con las paradas.",
        
        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Send your thoughts or experiences with the map",

        submit       : "Enviar",
        clear        : "Borrar",
        ok           : "OK",
        cancel       : "Cancelar",
        yes          : "S�",
        no           : "No"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southEast:      "sureste",
        southWest:      "sudoeste",
        northEast:      "nordeste",
        northWest:      "noroeste",
        north:          "norte",
        west:           "oeste",
        south:          "sur",
        east:           "este",
        bound:          "l�mite",
        left:           "left",
        right:          "right",
        slightly_left:  "slight left",
        slightly_right: "slight right",
        hard_left:      "hard left",
        hard_right:     "hard right",
        'continue':     "continue a",
        to_continue:    "para continuer a",
        becomes:        "nombre de calle cambia a",
        at:             "a"
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "System Map"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Planificador de viajes Autobuses Interurbanos",
            tabTitle      : "Planificar un Viaje",
            inputTitle    : "Detalles del Viaje",
            optTitle      : "Preferencias (opcional)",
            submitMsg     : "Planificando su Viaje...",
            optionalTitle : "",
            date          : "Fecha",
            time          : "Hora",
            when          : "Tiempo",
            from          : "Desde",
            fromHere      : "Desde aqu�",
            to            : "Hasta",
            toHere        : "Hasta aqu�",
            minimize      : "Mostrar el",
            maxWalkDistance: "M�xima distancia hasta la parada",
            arriveDepart  : "Llegada/Salida a",
            mode          : "Modo de viaje",
            go            : "Empezar",
            planTrip      : "Planifique su Viaje",
            newTrip       : "Nuevo Viaje"
        },

        error:
        {
            title        : 'Trip Planner Error',
            deadMsg      : "Map Trip Planner is currently not responding. Please wait a few minutes to try again, or try the text trip planner (see link below).",
            geoFromMsg   : "Por favor, seleccione la posici�n de salida de su Viaje: ",
            geoToMsg     : "Por favor, seleccione la posici�n de llegada de su Viaje: "
        },
        
        // default messages from server if a message was not returned
        // TODO translate me
        msgcodes:
        {
        	200: "Plan OK",
        	500: "Server error",
        	400: "Trip out of bounds",
        	404: "Path not found",
        	406: "No transit times",
        	408: "Request timed out",
        	413: "Invalid parameter",
        	440: "From geocode not found",
        	450: "To geocode not found",
        	460: "Geocode from and to not found",
        	409: "Too close",
        	340: "Geocode from ambiguous",
        	350: "Geocode to ambiguous",
        	360: "Geocode from and to ambiguous"
        },

        options: 
        [
          ['TRANSFERS', 'M�nimo n�mero de Transbordos'],
          ['SAFE', 'Viaje m�s seguro'],
          ['QUICK', 'Viaje m�s corto'] 
        ],
    
        arriveDepart: 
        [
          ['false', 'Salida'], 
          ['true',  'Llegada']
        ],
    
        maxWalkDistance : 
        [
            ['500',   '500 metros'],
            ['1000',   '1 Km'],
            ['5000',   '5 Km'],
            ['10000',  '10 Km'],
            ['20000',  '20 Km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Tr�nsito'],
            ['BUSISH,TRAINISH,WALK', 'Bus y Tren'],
            ['BUSISH,WALK', 'Solo Bus'],
            ['TRAINISH,WALK', 'Solo Tren'],
            ['WALK', 'Solo a pi�'],
            ['BICYCLE', 'Bicicleta']
        ],

        wheelchair :
        [
            ['false', 'No se requiere un viaje con accesibilidad'],
            ['true', 'Se requiere un viaje adaptado a Silla de Ruedas']
        ]
    },

    CLASS_NAME : "otp.locale.Spanish"
};
