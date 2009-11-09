
Ext.namespace("GeoExt.tree");GeoExt.tree.LayerContainer=Ext.extend(Ext.tree.TreeNode,{layerStore:null,defaults:null,constructor:function(config){this.layerStore=config.layerStore;this.defaults=config.defaults;GeoExt.tree.LayerContainer.superclass.constructor.apply(this,arguments);},render:function(bulkRender){if(!this.rendered){if(!this.layerStore){this.layerStore=GeoExt.MapPanel.guess().layers;}
this.layerStore.each(function(record){this.addLayerNode(record);},this);this.layerStore.on({"add":this.onStoreAdd,"remove":this.onStoreRemove,scope:this});}
GeoExt.tree.LayerContainer.superclass.render.call(this,bulkRender);},onStoreAdd:function(store,records,index){if(!this._reordering){var nodeIndex=this.recordIndexToNodeIndex(index+records.length-1);for(var i=0;i<records.length;++i){this.addLayerNode(records[i],nodeIndex);}}},onStoreRemove:function(store,record,index){if(!this._reordering){this.removeLayerNode(record);}},recordIndexToNodeIndex:function(index){var store=this.layerStore;var count=store.getCount();var nodeCount=this.childNodes.length;var nodeIndex=-1;for(var i=count-1;i>=0;--i){if(store.getAt(i).get("layer").displayInLayerSwitcher){++nodeIndex;if(index===i||nodeIndex>nodeCount-1){break;}}};return nodeIndex;},nodeIndexToRecordIndex:function(index){var store=this.layerStore;var count=store.getCount();var nodeIndex=-1;for(var i=count-1;i>=0;--i){if(store.getAt(i).get("layer").displayInLayerSwitcher){++nodeIndex;if(index===nodeIndex){break;}}}
return i;},addLayerNode:function(layerRecord,index){index=index||0;var layer=layerRecord.get("layer");if(layer.displayInLayerSwitcher===true){var node=new GeoExt.tree.LayerNode(Ext.applyIf({iconCls:layer.isBaseLayer?'baselayer-icon':'layer-icon',layer:layer,layerStore:this.layerStore},this.defaults));var sibling=this.item(index);if(sibling){this.insertBefore(node,sibling);}else{this.appendChild(node);}
node.on("move",this.onChildMove,this);}},removeLayerNode:function(layerRecord){var layer=layerRecord.get("layer");if(layer.displayInLayerSwitcher==true){var node=this.findChildBy(function(node){return node.layer==layer;});if(node){node.un("move",this.onChildMove,this);node.remove();}}},onChildMove:function(tree,node,oldParent,newParent,index){if(oldParent===newParent){var newRecordIndex=this.nodeIndexToRecordIndex(index);var oldRecordIndex=this.layerStore.findBy(function(record){return record.get("layer")===node.layer;});var record=this.layerStore.getAt(oldRecordIndex);this._reordering=true;this.layerStore.remove(record);this.layerStore.insert(newRecordIndex,[record]);delete this._reordering;}},destroy:function(){if(this.layerStore){this.layerStore.un("add",this.onStoreAdd,this);this.layerStore.un("remove",this.onStoreRemove,this);}
GeoExt.tree.LayerContainer.superclass.destroy.apply(this,arguments);}});Ext.tree.TreePanel.nodeTypes.gx_layercontainer=GeoExt.tree.LayerContainer;Ext.namespace("GeoExt.tree");GeoExt.tree.BaseLayerContainer=Ext.extend(GeoExt.tree.LayerContainer,{constructor:function(config){config.text=config.text||"Base Layer";GeoExt.tree.BaseLayerContainer.superclass.constructor.apply(this,arguments);},addLayerNode:function(layerRecord){var layer=layerRecord.get("layer");if(layer.isBaseLayer==true){GeoExt.tree.BaseLayerContainer.superclass.addLayerNode.call(this,layerRecord);}},removeLayerNode:function(layerRecord){var layer=layerRecord.get("layer");if(layer.isBaseLayer==true){GeoExt.tree.BaseLayerContainer.superclass.removeLayerNode.call(this,layerRecord);}}});Ext.tree.TreePanel.nodeTypes.gx_baselayercontainer=GeoExt.tree.BaseLayerContainer;Ext.namespace("GeoExt");GeoExt.SliderTip=Ext.extend(Ext.Tip,{hover:true,minWidth:10,minWidth:10,offsets:[0,-10],dragging:false,init:function(slider){slider.on({dragstart:this.onSlide,drag:this.onSlide,dragend:this.hide,destroy:this.destroy,scope:this});if(this.hover){slider.on("render",this.registerThumbListeners,this);}
this.slider=slider;},registerThumbListeners:function(){this.slider.thumb.on({"mouseover":function(){this.onSlide(this.slider);this.dragging=false;},"mouseout":function(){if(!this.dragging){this.hide.apply(this,arguments);}},scope:this});},onSlide:function(slider){this.dragging=true;this.show();this.body.update(this.getText(slider));this.doAutoWidth();this.el.alignTo(slider.thumb,'b-t?',this.offsets);},getText:function(slider){return slider.getValue();}});Ext.namespace("GeoExt");GeoExt.ZoomSlider=Ext.extend(Ext.Slider,{map:null,baseCls:"gx-zoomslider",aggressive:false,updating:false,initComponent:function(){GeoExt.ZoomSlider.superclass.initComponent.call(this);if(this.map){if(this.map instanceof GeoExt.MapPanel){this.map=this.map.map;}
this.bind(this.map);}
if(this.aggressive===true){this.on('change',this.changeHandler,this);}else{this.on('changecomplete',this.changeHandler,this);}
this.on("beforedestroy",this.unbind,this);},onRender:function(){GeoExt.ZoomSlider.superclass.onRender.apply(this,arguments);this.el.addClass(this.baseCls);},afterRender:function(){Ext.Slider.superclass.afterRender.apply(this,arguments);this.update();},addToMapPanel:function(panel){this.on({render:function(){var el=this.getEl();el.setStyle({position:"absolute",zIndex:panel.map.Z_INDEX_BASE.Control});el.on({mousedown:this.stopMouseEvents,click:this.stopMouseEvents});},scope:this});this.bind(panel.map);},stopMouseEvents:function(e){e.stopEvent();},removeFromMapPanel:function(panel){var el=this.getEl();el.un("mousedown",this.stopMouseEvents,this);el.un("click",this.stopMouseEvents,this);this.unbind();},bind:function(map){this.map=map;this.map.events.on({zoomend:this.update,changebaselayer:this.initZoomValues,scope:this});if(this.map.baseLayer){this.initZoomValues();}},unbind:function(){if(this.map){this.map.events.un({zoomend:this.update,changebaselayer:this.initZoomValues,scope:this});}},initZoomValues:function(){var layer=this.map.baseLayer;if(this.initialConfig.minValue===undefined){this.minValue=layer.minZoomLevel||0;}
if(this.initialConfig.maxValue===undefined){this.maxValue=layer.maxZoomLevel||layer.numZoomLevels-1;}},getZoom:function(){return this.getValue();},getScale:function(){return OpenLayers.Util.getScaleFromResolution(this.map.getResolutionForZoom(this.getValue()),this.map.getUnits());},getResolution:function(){return this.map.getResolutionForZoom(this.getValue());},changeHandler:function(){if(this.map&&!this.updating){this.map.zoomTo(this.getValue());}},update:function(){if(this.rendered&&this.map){this.updating=true;this.setValue(this.map.getZoom());this.updating=false;}}});Ext.reg('gx_zoomslider',GeoExt.ZoomSlider);Ext.namespace("GeoExt.data");GeoExt.data.WMSCapabilitiesReader=function(meta,recordType){meta=meta||{};if(!meta.format){meta.format=new OpenLayers.Format.WMSCapabilities();}
if(!(typeof recordType==="function")){recordType=GeoExt.data.LayerRecord.create(recordType||meta.fields||[{name:"name",type:"string"},{name:"abstract",type:"string"},{name:"queryable",type:"boolean"},{name:"formats"},{name:"styles"},{name:"llbbox"},{name:"minScale"},{name:"maxScale"},{name:"prefix"}]);}
GeoExt.data.WMSCapabilitiesReader.superclass.constructor.call(this,meta,recordType);};Ext.extend(GeoExt.data.WMSCapabilitiesReader,Ext.data.DataReader,{read:function(request){var data=request.responseXML;if(!data||!data.documentElement){data=request.responseText;}
return this.readRecords(data);},readRecords:function(data){if(typeof data==="string"||data.nodeType){data=this.meta.format.read(data);}
var url=data.capability.request.getmap.href;var records=[],layer;for(var i=0,len=data.capability.layers.length;i<len;i++){layer=data.capability.layers[i];if(layer.name){records.push(new this.recordType(Ext.apply(layer,{layer:new OpenLayers.Layer.WMS(layer.title||layer.name,url,{layers:layer.name})})));}}
return{totalRecords:records.length,success:true,records:records};}});Ext.namespace("GeoExt.data");GeoExt.data.LayerStoreMixin={map:null,reader:null,constructor:function(config){config=config||{};config.reader=config.reader||new GeoExt.data.LayerReader({},config.fields);delete config.fields;var map=config.map instanceof GeoExt.MapPanel?config.map.map:config.map;delete config.map;if(config.layers){config.data=config.layers;}
delete config.layers;var options={initDir:config.initDir};delete config.initDir;arguments.callee.superclass.constructor.call(this,config);if(map){this.bind(map,options);}},bind:function(map,options){if(this.map){return;}
this.map=map;options=options||{};var initDir=options.initDir;if(options.initDir==undefined){initDir=GeoExt.data.LayerStore.MAP_TO_STORE|GeoExt.data.LayerStore.STORE_TO_MAP;}
var layers=map.layers.slice(0);if(initDir&GeoExt.data.LayerStore.STORE_TO_MAP){this.each(function(record){this.map.addLayer(record.get("layer"));},this);}
if(initDir&GeoExt.data.LayerStore.MAP_TO_STORE){this.loadData(layers,true);}
map.events.on({"changelayer":this.onChangeLayer,"addlayer":this.onAddLayer,"removelayer":this.onRemoveLayer,scope:this});this.on({"load":this.onLoad,"clear":this.onClear,"add":this.onAdd,"remove":this.onRemove,"update":this.onUpdate,scope:this});this.data.on({"replace":this.onReplace,scope:this});},unbind:function(){if(this.map){this.map.events.un({"changelayer":this.onChangeLayer,"addlayer":this.onAddLayer,"removelayer":this.onRemoveLayer,scope:this});this.un("load",this.onLoad,this);this.un("clear",this.onClear,this);this.un("add",this.onAdd,this);this.un("remove",this.onRemove,this);this.data.un("replace",this.onReplace,this);this.map=null;}},onChangeLayer:function(evt){var layer=evt.layer;var recordIndex=this.findBy(function(rec,id){return rec.get("layer")===layer;});if(recordIndex>-1){var record=this.getAt(recordIndex);if(evt.property==="order"){if(!this._adding&&!this._removing){var layerIndex=this.map.getLayerIndex(layer);if(layerIndex!==recordIndex){this._removing=true;this.remove(record);delete this._removing;this._adding=true;this.insert(layerIndex,[record]);delete this._adding;}}}else if(evt.property==="name"){record.set("title",layer.name);}else{this.fireEvent("update",this,record,Ext.data.Record.EDIT);}}},onAddLayer:function(evt){if(!this._adding){var layer=evt.layer;this._adding=true;this.loadData([layer],true);delete this._adding;}},onRemoveLayer:function(evt){if(this.map.unloadDestroy){if(!this._removing){var layer=evt.layer;this._removing=true;this.remove(this.getById(layer.id));delete this._removing;}}else{this.unbind();}},onLoad:function(store,records,options){if(!Ext.isArray(records)){records=[records];}
if(options&&!options.add){this._removing=true;for(var i=this.map.layers.length-1;i>=0;i--){this.map.removeLayer(this.map.layers[i]);}
delete this._removing;var len=records.length;if(len>0){var layers=new Array(len);for(var j=0;j<len;j++){layers[j]=records[j].get("layer");}
this._adding=true;this.map.addLayers(layers);delete this._adding;}}},onClear:function(store){this._removing=true;for(var i=this.map.layers.length-1;i>=0;i--){this.map.removeLayer(this.map.layers[i]);}
delete this._removing;},onAdd:function(store,records,index){if(!this._adding){this._adding=true;var layer;for(var i=records.length-1;i>=0;--i){layer=records[i].get("layer");this.map.addLayer(layer);if(index!==this.map.layers.length-1){this.map.setLayerIndex(layer,index);}}
delete this._adding;}},onRemove:function(store,record,index){if(!this._removing){var layer=record.get("layer");if(this.map.getLayer(layer.id)!=null){this._removing=true;this.removeMapLayer(record);delete this._removing;}}},onUpdate:function(store,record,operation){if(operation===Ext.data.Record.EDIT){var layer=record.get("layer");var title=record.get("title");if(title!==layer.name){layer.setName(title);}}},removeMapLayer:function(record){this.map.removeLayer(record.get("layer"));},onReplace:function(key,oldRecord,newRecord){this.removeMapLayer(oldRecord);},destroy:function(){this.unbind();GeoExt.data.LayerStore.superclass.destroy.call(this);}};GeoExt.data.LayerStore=Ext.extend(Ext.data.Store,GeoExt.data.LayerStoreMixin);GeoExt.data.LayerStore.MAP_TO_STORE=1;GeoExt.data.LayerStore.STORE_TO_MAP=2;Ext.namespace("GeoExt","GeoExt.data");GeoExt.data.LayerReader=function(meta,recordType){meta=meta||{};if(!(recordType instanceof Function)){recordType=GeoExt.data.LayerRecord.create(recordType||meta.fields||{});}
GeoExt.data.LayerReader.superclass.constructor.call(this,meta,recordType);};Ext.extend(GeoExt.data.LayerReader,Ext.data.DataReader,{totalRecords:null,readRecords:function(layers){var records=[];if(layers){var recordType=this.recordType,fields=recordType.prototype.fields;var i,lenI,j,lenJ,layer,values,field,v;for(i=0,lenI=layers.length;i<lenI;i++){layer=layers[i];values={};for(j=0,lenJ=fields.length;j<lenJ;j++){field=fields.items[j];v=layer[field.mapping||field.name]||field.defaultValue;v=field.convert(v);values[field.name]=v;}
values.layer=layer;records[records.length]=new recordType(values,layer.id);}}
return{records:records,totalRecords:this.totalRecords!=null?this.totalRecords:records.length};}});Ext.namespace("GeoExt.form");GeoExt.form.BasicForm=Ext.extend(Ext.form.BasicForm,{protocol:null,doAction:function(action,options){if(action=="search"){options=Ext.applyIf(options||{},{protocol:this.protocol});action=new GeoExt.form.SearchAction(this,options);}
return GeoExt.form.BasicForm.superclass.doAction.call(this,action,options);},search:function(options){return this.doAction("search",options);}});Ext.namespace('GeoExt','GeoExt.data');GeoExt.data.FeatureReader=function(meta,recordType){meta=meta||{};if(!(recordType instanceof Function)){recordType=GeoExt.data.FeatureRecord.create(recordType||meta.fields||{});}
GeoExt.data.FeatureReader.superclass.constructor.call(this,meta,recordType);};Ext.extend(GeoExt.data.FeatureReader,Ext.data.DataReader,{totalRecords:null,read:function(response){return this.readRecords(response.features);},readRecords:function(features){var records=[];if(features){var recordType=this.recordType,fields=recordType.prototype.fields;var i,lenI,j,lenJ,feature,values,field,v;for(i=0,lenI=features.length;i<lenI;i++){feature=features[i];values={};if(feature.attributes){for(j=0,lenJ=fields.length;j<lenJ;j++){field=fields.items[j];if(/[\[\.]/.test(field.mapping)){try{v=new Function("obj","return obj."+field.mapping)(feature.attributes);}catch(e){v=field.defaultValue;}}
else{v=feature.attributes[field.mapping||field.name]||field.defaultValue;}
v=field.convert(v);values[field.name]=v;}}
values.feature=feature;values.state=feature.state;values.fid=feature.fid;records[records.length]=new recordType(values,feature.id);}}
return{records:records,totalRecords:this.totalRecords!=null?this.totalRecords:records.length};}});Ext.namespace("GeoExt.form");GeoExt.form.SearchAction=Ext.extend(Ext.form.Action,{type:"search",response:null,constructor:function(form,options){GeoExt.form.SearchAction.superclass.constructor.call(this,form,options);},run:function(){var o=this.options;var f=GeoExt.form.toFilter(this.form);if(o.clientValidation===false||this.form.isValid()){this.response=o.protocol.read(Ext.applyIf({filter:f,callback:this.handleResponse,scope:this},o));}else if(o.clientValidation!==false){this.failureType=Ext.form.Action.CLIENT_INVALID;this.form.afterAction(this,false);}},handleResponse:function(response){this.response=response;if(response.success()){this.form.afterAction(this,true);}else{this.form.afterAction(this,false);}
var o=this.options;if(o.callback){o.callback.call(o.scope,response);}}});Ext.namespace("GeoExt.data");GeoExt.data.FeatureRecord=Ext.data.Record.create([{name:"feature"},{name:"state"},{name:"fid"}]);GeoExt.data.FeatureRecord.create=function(o){var f=Ext.extend(GeoExt.data.FeatureRecord,{});var p=f.prototype;p.fields=new Ext.util.MixedCollection(false,function(field){return field.name;});GeoExt.data.FeatureRecord.prototype.fields.each(function(f){p.fields.add(f);});if(o){for(var i=0,len=o.length;i<len;i++){p.fields.add(new Ext.data.Field(o[i]));}}
f.getField=function(name){return p.fields.get(name);};return f;};Ext.namespace('GeoExt');GeoExt.LegendWMS=Ext.extend(Ext.Panel,{imageFormat:"image/gif",layer:null,bodyBorder:false,initComponent:function(){GeoExt.LegendWMS.superclass.initComponent.call(this);this.createLegend();},getLegendUrl:function(layerName){return this.layer.getFullRequestString({REQUEST:"GetLegendGraphic",WIDTH:null,HEIGHT:null,EXCEPTIONS:"application/vnd.ogc.se_xml",LAYER:layerName,LAYERS:null,SRS:null,FORMAT:this.imageFormat});},createLegend:function(){var layers=this.layer.params.LAYERS.split(",");for(var i=0,len=layers.length;i<len;i++){var layerName=layers[i];var legend=new GeoExt.LegendImage({url:this.getLegendUrl(layerName)});this.add(legend);}}});Ext.namespace("GeoExt.data");GeoExt.data.LayerRecord=Ext.data.Record.create([{name:"layer"},{name:"title",type:"string",mapping:"name"}]);GeoExt.data.LayerRecord.create=function(o){var f=Ext.extend(GeoExt.data.LayerRecord,{});var p=f.prototype;p.fields=new Ext.util.MixedCollection(false,function(field){return field.name;});GeoExt.data.LayerRecord.prototype.fields.each(function(f){p.fields.add(f);});if(o){for(var i=0,len=o.length;i<len;i++){p.fields.add(new Ext.data.Field(o[i]));}}
f.getField=function(name){return p.fields.get(name);};return f;};Ext.namespace("GeoExt");GeoExt.Popup=Ext.extend(Ext.Window,{anchored:true,map:null,panIn:true,unpinnable:true,feature:null,lonlat:null,animCollapse:false,draggable:false,shadow:false,popupCls:"gx-popup",ancCls:null,initComponent:function(){if(this.map instanceof GeoExt.MapPanel){this.map=this.map.map;}
if(!this.map&&this.feature&&this.feature.layer){this.map=this.feature.layer.map;}
if(!this.feature&&this.lonlat){this.feature=new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(this.lonlat.lon,this.lonlat.lat));}
if(this.anchored){this.addAnchorEvents();}
this.baseCls=this.popupCls+" "+this.baseCls;this.elements+=',anc';GeoExt.Popup.superclass.initComponent.call(this);},onRender:function(ct,position){GeoExt.Popup.superclass.onRender.call(this,ct,position);this.ancCls=this.popupCls+"-anc";this.createElement("anc",this.el);},initTools:function(){if(this.unpinnable){this.addTool({id:'unpin',handler:this.unanchorPopup.createDelegate(this,[])});}
GeoExt.Popup.superclass.initTools.call(this);},show:function(){GeoExt.Popup.superclass.show.apply(this,arguments);if(this.anchored){this.position();if(this.panIn&&!this._mapMove){this.panIntoView();}}},setSize:function(w,h){if(this.anc){var ancSize=this.getAnchorElement().getSize();if(typeof w=='object'){h=w.height-ancSize.height;w=w.width;}else if(!isNaN(h)){h=h-ancSize.height;}}
GeoExt.Popup.superclass.setSize.call(this,w,h);},position:function(){var centerLonLat=this.feature.geometry.getBounds().getCenterLonLat();if(this._mapMove===true){var visible=this.map.getExtent().containsLonLat(centerLonLat);if(visible!==this.isVisible()){this.setVisible(visible);}}
if(this.isVisible()){var centerPx=this.map.getViewPortPxFromLonLat(centerLonLat);var mapBox=Ext.fly(this.map.div).getBox();var anchorSelector="div."+this.ancCls;

var anc = this.anc.down(anchorSelector) || this.anc;
var dx = anc.getLeft(true) + anc.getWidth() / 2;

var dy=this.el.getHeight();this.setPosition(centerPx.x+mapBox.x-dx,centerPx.y+mapBox.y-dy);}},getAnchorElement:function(){var anchorSelector="div."+this.ancCls;var anc=Ext.get(this.el.child(anchorSelector));return anc;},unanchorPopup:function(){this.removeAnchorEvents();this.draggable=true;this.header.addClass("x-window-draggable");this.dd=new Ext.Window.DD(this);this.getAnchorElement().remove();this.anc=null;this.tools.unpin.hide();},panIntoView:function(){var centerLonLat=this.feature.geometry.getBounds().getCenterLonLat();var centerPx=this.map.getViewPortPxFromLonLat(centerLonLat);var mapBox=Ext.fly(this.map.div).getBox();var popupPos=this.getPosition(true);popupPos[0]-=mapBox.x;popupPos[1]-=mapBox.y;var panelSize=[mapBox.width,mapBox.height];var popupSize=this.getSize();var newPos=[popupPos[0],popupPos[1]];var padding=this.map.paddingForPopups;if(popupPos[0]<padding.left){newPos[0]=padding.left;}else if(popupPos[0]+popupSize.width>panelSize[0]-padding.right){newPos[0]=panelSize[0]-padding.right-popupSize.width;}
if(popupPos[1]<padding.top){newPos[1]=padding.top;}else if(popupPos[1]+popupSize.height>panelSize[1]-padding.bottom){newPos[1]=panelSize[1]-padding.bottom-popupSize.height;}
var dx=popupPos[0]-newPos[0];var dy=popupPos[1]-newPos[1];this.map.pan(dx,dy);},onMapMove:function(){this._mapMove=true;this.position();delete this._mapMove;},addAnchorEvents:function(){this.map.events.on({"move":this.onMapMove,scope:this});this.on({"resize":this.position,"collapse":this.position,"expand":this.position,scope:this});},removeAnchorEvents:function(){this.map.events.un({"move":this.onMapMove,scope:this});this.un("resize",this.position,this);this.un("collapse",this.position,this);this.un("expand",this.position,this);},beforeDestroy:function(){if(this.anchored){this.removeAnchorEvents();}
GeoExt.Popup.superclass.beforeDestroy.call(this);}});Ext.reg('gx_popup',GeoExt.Popup);Ext.namespace("GeoExt.form");GeoExt.form.FormPanel=Ext.extend(Ext.form.FormPanel,{protocol:null,createForm:function(){delete this.initialConfig.listeners;return new GeoExt.form.BasicForm(null,this.initialConfig);},search:function(options){this.getForm().search(options);}});Ext.reg("gx_formpanel",GeoExt.form.FormPanel);Ext.namespace("GeoExt.tree");GeoExt.tree.OverlayLayerContainer=Ext.extend(GeoExt.tree.LayerContainer,{constructor:function(config){config.text=config.text||"Overlays";GeoExt.tree.OverlayLayerContainer.superclass.constructor.apply(this,arguments);},addLayerNode:function(layerRecord){var layer=layerRecord.get("layer");if(layer.isBaseLayer==false){GeoExt.tree.OverlayLayerContainer.superclass.addLayerNode.call(this,layerRecord);}},removeLayerNode:function(layerRecord){var layer=layerRecord.get("layer");if(layer.isBaseLayer==false){GeoExt.tree.OverlayLayerContainer.superclass.removeLayerNode.call(this,layerRecord);}}});Ext.tree.TreePanel.nodeTypes.gx_overlaylayercontainer=GeoExt.tree.OverlayLayerContainer;Ext.namespace('GeoExt');GeoExt.LegendPanel=Ext.extend(Ext.Panel,{dynamic:true,showTitle:true,labelCls:null,bodyStyle:'',layerStore:null,initComponent:function(){GeoExt.LegendPanel.superclass.initComponent.call(this);},onRender:function(){GeoExt.LegendPanel.superclass.onRender.apply(this,arguments);if(!this.layerStore){this.layerStore=GeoExt.MapPanel.guess().layers;}
this.layerStore.each(function(record){this.addLegend(record);},this);if(this.dynamic){this.layerStore.on({"add":this.onStoreAdd,"remove":this.onStoreRemove,"update":this.onStoreUpdate,scope:this});}
this.doLayout();},recordIndexToPanelIndex:function(index){var store=this.layerStore;var count=store.getCount();var panelIndex=-1;var legendCount=this.items?this.items.length:0;for(var i=count-1;i>=0;--i){var layer=store.getAt(i).get("layer");var legendGenerator=GeoExt["Legend"+layer.CLASS_NAME.split(".").pop()];if(layer.displayInLayerSwitcher&&legendGenerator&&(store.getAt(i).get("hideInLegend")!==true)){++panelIndex;if(index===i||panelIndex>legendCount-1){break;}}}
return panelIndex;},onStoreUpdate:function(store,record,operation){var layer=record.get('layer');var legend=this.getComponent(layer.id);if((this.showTitle&&!record.get('hideTitle'))&&(legend.items.get(0).text!==record.get('title'))){legend.items.get(0).setText(record.get('title'));}
if(legend){legend.setVisible(layer.getVisibility()&&layer.displayInLayerSwitcher&&!record.get('hideInLegend'));if(record.get('legendURL')){var items=legend.findByType('gx_legendimage');for(var i=0,len=items.length;i<len;i++){items[i].setUrl(record.get('legendURL'));}}}},onStoreAdd:function(store,records,index){var panelIndex=this.recordIndexToPanelIndex(index+records.length-1);for(var i=0,len=records.length;i<len;i++){this.addLegend(records[i],panelIndex);}
this.doLayout();},onStoreRemove:function(store,record,index){this.removeLegend(record);},removeLegend:function(record){var legend=this.getComponent(record.get('layer').id);if(legend){this.remove(legend,true);this.doLayout();}},createLegendSubpanel:function(record){var layer=record.get('layer');var mainPanel=this.createMainPanel(record);if(mainPanel!==null){var legend;if(record.get('legendURL')){legend=new GeoExt.LegendImage({url:record.get('legendURL')});mainPanel.add(legend);}else{var legendGenerator=GeoExt["Legend"+layer.CLASS_NAME.split(".").pop()];if(legendGenerator){legend=new legendGenerator({layer:layer});mainPanel.add(legend);}}}
return mainPanel;},addLegend:function(record,index){index=index||0;var layer=record.get('layer');var legendSubpanel=this.createLegendSubpanel(record);if(legendSubpanel!==null){legendSubpanel.setVisible(layer.getVisibility());this.insert(index,legendSubpanel);}},createMainPanel:function(record){var layer=record.get('layer');var panel=null;var legendGenerator=GeoExt["Legend"+layer.CLASS_NAME.split(".").pop()];if(layer.displayInLayerSwitcher&&!record.get('hideInLegend')&&legendGenerator){var panelConfig={id:layer.id,border:false,bodyBorder:false,bodyStyle:this.bodyStyle,items:[new Ext.form.Label({text:(this.showTitle&&!record.get('hideTitle'))?layer.name:'',cls:'x-form-item x-form-item-label'+
(this.labelCls?' '+this.labelCls:'')})]};panel=new Ext.Panel(panelConfig);}
return panel;},onDestroy:function(){if(this.layerStore){this.layerStore.un("add",this.onStoreAdd,this);this.layerStore.un("remove",this.onStoreRemove,this);this.layerStore.un("update",this.onStoreUpdate,this);}
GeoExt.LegendPanel.superclass.onDestroy.apply(this,arguments);}});Ext.reg('gx_legendpanel',GeoExt.LegendPanel);Ext.namespace("GeoExt.form");GeoExt.form.toFilter=function(form,logicalOp){if(form instanceof Ext.form.FormPanel){form=form.getForm();}
var filters=[],values=form.getValues(false);for(var prop in values){var s=prop.split("__");var value=values[prop],type;if(s.length>1&&(type=GeoExt.form.toFilter.FILTER_MAP[s[1]])!==undefined){prop=s[0];}else{type=OpenLayers.Filter.Comparison.EQUAL_TO;}
filters.push(new OpenLayers.Filter.Comparison({type:type,value:value,property:prop}));}
return new OpenLayers.Filter.Logical({type:logicalOp||OpenLayers.Filter.Logical.AND,filters:filters});};GeoExt.form.toFilter.FILTER_MAP={"eq":OpenLayers.Filter.Comparison.EQUAL_TO,"ne":OpenLayers.Filter.Comparison.NOT_EQUAL_TO,"lt":OpenLayers.Filter.Comparison.LESS_THAN,"le":OpenLayers.Filter.Comparison.LESS_THAN_OR_EQUAL_TO,"gt":OpenLayers.Filter.Comparison.GREATER_THAN,"ge":OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO,"like":OpenLayers.Filter.Comparison.LIKE};Ext.namespace("GeoExt.tree");GeoExt.tree.LayerNodeUI=Ext.extend(Ext.tree.TreeNodeUI,{radio:null,constructor:function(config){GeoExt.tree.LayerNodeUI.superclass.constructor.apply(this,arguments);},render:function(bulkRender){GeoExt.tree.LayerNodeUI.superclass.render.call(this,bulkRender);var a=this.node.attributes;if(a.radioGroup&&this.radio===null){this.radio=Ext.DomHelper.insertAfter(this.checkbox,['<input type="radio" class="x-tree-node-radio" name="',a.radioGroup,'_radio"></input>'].join(""));}},onClick:function(e){if(e.getTarget('input[type=radio]',1)){this.fireEvent("radiochange",this.node);}else{GeoExt.tree.LayerNodeUI.superclass.onClick.call(this,e);}},toggleCheck:function(value){GeoExt.tree.LayerNodeUI.superclass.toggleCheck.call(this,value);var node=this.node;var layer=this.node.layer;node.visibilityChanging=true;if(this.checkbox&&(layer.getVisibility()!=this.isChecked())){layer.setVisibility(this.isChecked());}
node.visibilityChanging=false;},destroy:function(){delete this.radio;GeoExt.tree.LayerNodeUI.superclass.destroy.call(this);}});GeoExt.tree.LayerNode=Ext.extend(Ext.tree.TreeNode,{layer:null,layerStore:null,childNodeType:null,visibilityChanging:false,constructor:function(config){config.leaf=config.leaf||!config.children;config.iconCls=typeof config.iconCls=="undefined"&&!config.children?"layer-icon":config.iconCls;config.checked=false;this.defaultUI=this.defaultUI||GeoExt.tree.LayerNodeUI;this.addEvents("radiochange");Ext.apply(this,{layer:config.layer,layerStore:config.layerStore,childNodeType:config.childNodeType});GeoExt.tree.LayerNode.superclass.constructor.apply(this,arguments);},render:function(bulkRender){var layer=this.layer instanceof OpenLayers.Layer&&this.layer;if(!layer){if(!this.layerStore||this.layerStore=="auto"){this.layerStore=GeoExt.MapPanel.guess().layers;}
var i=this.layerStore.findBy(function(o){return o.get("title")==this.layer;},this);if(i!=-1){layer=this.layerStore.getAt(i).get("layer");}}
if(!this.rendered||!layer){var ui=this.getUI();if(layer){this.layer=layer;if(!this.text){this.text=layer.name;}
if(this.childNodeType){this.addChildNodes();}
ui.show();ui.toggleCheck(layer.getVisibility());this.addVisibilityEventHandlers();this.attributes.checked=layer.getVisibility();}else{ui.hide();}
if(this.layerStore instanceof GeoExt.data.LayerStore){this.addStoreEventHandlers(layer);}}
GeoExt.tree.LayerNode.superclass.render.call(this,bulkRender);},addVisibilityEventHandlers:function(){this.layer.events.on({"visibilitychanged":this.onLayerVisibilityChanged,scope:this});this.on({"checkchange":this.onCheckChange,scope:this});},onLayerVisibilityChanged:function(){if(!this.visibilityChanging&&this.attributes.checked!=this.layer.getVisibility()){this.getUI().toggleCheck(this.layer.getVisibility());}},onCheckChange:function(node,checked){if(checked&&this.layer.isBaseLayer&&this.layer.map){this.layer.map.setBaseLayer(this.layer);}
this.layer.setVisibility(checked);},addStoreEventHandlers:function(){this.layerStore.on({"add":this.onStoreAdd,"remove":this.onStoreRemove,"update":this.onStoreUpdate,scope:this});},onStoreAdd:function(store,records,index){var l;for(var i=0;i<records.length;++i){l=records[i].get("layer");if(this.layer==l){this.getUI().show();}else if(this.layer==l.name){this.render(bulkRender);return;}}},onStoreRemove:function(store,record,index){if(this.layer==record.get("layer")){this.getUI().hide();}},onStoreUpdate:function(store,record,operation){var layer=record.get("layer");if(this.layer==layer&&this.text!==layer.name){this.setText(layer.name);}},addChildNodes:function(){if(typeof this.childNodeType=="string"){Ext.tree.TreePanel.nodeTypes[this.childNodeType].add(this);}else if(typeof this.childNodeType.add==="function"){this.childNodeType.add(this);}},destroy:function(){var layer=this.layer;if(layer instanceof OpenLayers.Layer){layer.events.un({"visibilitychanged":this.onLayerVisibilityChanged,scope:this});}
delete this.layer;var layerStore=this.layerStore;if(layerStore){layerStore.un("add",this.onStoreAdd,this);layerStore.un("remove",this.onStoreRemove,this);layerStore.un("update",this.onStoreUpdate,this);}
delete this.layerStore;this.un("checkchange",this.onCheckChange,this);GeoExt.tree.LayerNode.superclass.destroy.call(this);}});Ext.tree.TreePanel.nodeTypes.gx_layer=GeoExt.tree.LayerNode;Ext.namespace('GeoExt');GeoExt.LegendImage=Ext.extend(Ext.BoxComponent,{url:null,defaultImgSrc:null,imgCls:null,initComponent:function(){GeoExt.LegendImage.superclass.initComponent.call(this);if(this.defaultImgSrc===null){this.defaultImgSrc=Ext.BLANK_IMAGE_URL;}
this.autoEl={tag:"img","class":(this.imgCls?this.imgCls:""),src:this.defaultImgSrc};},setUrl:function(url){this.url=url;var el=this.getEl();if(el){el.un("error",this.onImageLoadError,this);el.on("error",this.onImageLoadError,this,{single:true});el.dom.src=url;}},onRender:function(ct,position){GeoExt.LegendImage.superclass.onRender.call(this,ct,position);if(this.url){this.setUrl(this.url);}},onDestroy:function(){var el=this.getEl();if(el){el.un("error",this.onImageLoadError,this);}
GeoExt.LegendImage.superclass.onDestroy.apply(this,arguments);},onImageLoadError:function(){this.getEl().dom.src=this.defaultImgSrc;}});Ext.reg('gx_legendimage',GeoExt.LegendImage);Ext.namespace("GeoExt");GeoExt.ZoomSliderTip=Ext.extend(GeoExt.SliderTip,{template:'<div>Zoom Level: {zoom}</div>'+'<div>Resolution: {resolution}</div>'+'<div>Scale: 1 : {scale}</div>',compiledTemplate:null,init:function(slider){this.compiledTemplate=new Ext.Template(this.template);GeoExt.ZoomSliderTip.superclass.init.call(this,slider);},getText:function(slider){var data={zoom:slider.getZoom(),resolution:slider.getResolution(),scale:Math.round(slider.getScale())};return this.compiledTemplate.apply(data);}});Ext.namespace("GeoExt.data");GeoExt.data.ScaleStore=Ext.extend(Ext.data.Store,{map:null,constructor:function(config){var map=(config.map instanceof GeoExt.MapPanel?config.map.map:config.map);delete config.map;config=Ext.applyIf(config,{reader:new Ext.data.JsonReader({},["level","resolution","scale"])});GeoExt.data.ScaleStore.superclass.constructor.call(this,config);if(map){this.bind(map);}},bind:function(map,options){this.map=(map instanceof GeoExt.MapPanel?map.map:map);this.map.events.register('changebaselayer',this,this.populateFromMap);if(this.map.baseLayer){this.populateFromMap();}else{this.map.events.register('addlayer',this,this.populateOnAdd);}},unbind:function(){if(this.map){this.map.events.unregister('addlayer',this,this.populateOnAdd);this.map.events.unregister('changebaselayer',this,this.populateFromMap);delete this.map;}},populateOnAdd:function(evt){if(evt.layer.isBaseLayer){this.populateFromMap();this.map.events.unregister('addlayer',this,this.populateOnAdd);}},populateFromMap:function(){var zooms=[];var resolutions=this.map.baseLayer.resolutions;var units=this.map.baseLayer.units;for(var i=resolutions.length-1;i>=0;i--){var res=resolutions[i];zooms.push({level:i,resolution:res,scale:OpenLayers.Util.getScaleFromResolution(res,units)});}
this.loadData(zooms);}});Ext.namespace("GeoExt.data");GeoExt.data.FeatureStoreMixin={layer:null,reader:null,addFeatureFilter:null,addRecordFilter:null,constructor:function(config){config=config||{};config.reader=config.reader||new GeoExt.data.FeatureReader({},config.fields);var layer=config.layer;delete config.layer;if(config.features){config.data=config.features;}
delete config.features;var options={initDir:config.initDir};delete config.initDir;arguments.callee.superclass.constructor.call(this,config);if(layer){this.bind(layer,options);}},bind:function(layer,options){if(this.layer){return;}
this.layer=layer;options=options||{};var initDir=options.initDir;if(options.initDir==undefined){initDir=GeoExt.data.FeatureStore.LAYER_TO_STORE|GeoExt.data.FeatureStore.STORE_TO_LAYER;}
var features=layer.features.slice(0);if(initDir&GeoExt.data.FeatureStore.STORE_TO_LAYER){var records=this.getRange();for(var i=records.length-1;i>=0;i--){this.layer.addFeatures([records[i].get("feature")]);}}
if(initDir&GeoExt.data.FeatureStore.LAYER_TO_STORE){this.loadData(features,true);}
layer.events.on({"featuresadded":this.onFeaturesAdded,"featuresremoved":this.onFeaturesRemoved,"featuremodified":this.onFeatureModified,scope:this});this.on({"load":this.onLoad,"clear":this.onClear,"add":this.onAdd,"remove":this.onRemove,"update":this.onUpdate,scope:this});},unbind:function(){if(this.layer){this.layer.events.un({"featuresadded":this.onFeaturesAdded,"featuresremoved":this.onFeaturesRemoved,"featuremodified":this.onFeatureModified,scope:this});this.un("load",this.onLoad,this);this.un("clear",this.onClear,this);this.un("add",this.onAdd,this);this.un("remove",this.onRemove,this);this.un("update",this.onUpdate,this);this.layer=null;}},onFeaturesAdded:function(evt){if(!this._adding){var features=evt.features,toAdd=features;if(typeof this.addFeatureFilter=="function"){toAdd=[];var i,len,feature;for(var i=0,len=features.length;i<len;i++){feature=features[i];if(this.addFeatureFilter(feature)!==false){toAdd.push(feature);}}}
this._adding=true;this.loadData(toAdd,true);delete this._adding;}},onFeaturesRemoved:function(evt){if(!this._removing){var features=evt.features,feature,record,i;for(i=features.length-1;i>=0;i--){feature=features[i];record=this.getById(feature.id);if(record!==undefined){this._removing=true;this.remove(record);delete this._removing;}}}},onFeatureModified:function(evt){if(!this._updating){var feature=evt.feature;var record=this.getById(feature.id);if(record!==undefined){record.beginEdit();attributes=feature.attributes;if(attributes){var fields=this.recordType.prototype.fields;for(var i=0,len=fields.length;i<len;i++){var field=fields.items[i];var v=attributes[field.mapping||field.name]||field.defaultValue;v=field.convert(v);record.set(field.name,v);}}
record.set("state",feature.state);record.set("fid",feature.fid);record.set("feature",feature);this._updating=true;record.endEdit();delete this._updating;}}},addFeaturesToLayer:function(records){var i,len,features,record;if(typeof this.addRecordFilter=="function"){features=[]
for(i=0,len=records.length;i<len;i++){record=records[i];if(this.addRecordFilter(record)!==false){features.push(record.get("feature"));}}}else{features=new Array((len=records.length));for(i=0;i<len;i++){features[i]=records[i].get("feature");}}
if(features.length>0){this._adding=true;this.layer.addFeatures(features);delete this._adding;}},onLoad:function(store,records,options){if(!options||options.add!==true){this._removing=true;this.layer.removeFeatures(this.layer.features);delete this._removing;this.addFeaturesToLayer(records);}},onClear:function(store){this._removing=true;this.layer.removeFeatures(this.layer.features);delete this._removing;},onAdd:function(store,records,index){if(!this._adding){this.addFeaturesToLayer(records);}},onRemove:function(store,record,index){if(!this._removing){var feature=record.get("feature");if(this.layer.getFeatureById(feature.id)!=null){this._removing=true;this.layer.removeFeatures([record.get("feature")]);delete this._removing;}}},onUpdate:function(store,record,operation){if(!this._updating){var feature=record.get("feature");if(record.fields){var cont=this.layer.events.triggerEvent("beforefeaturemodified",{feature:feature});if(cont!==false){record.fields.each(function(field){feature.attributes[field.mapping||field.name]=record.get(field.name);});this._updating=true;this.layer.events.triggerEvent("featuremodified",{feature:feature});delete this._updating;if(this.layer.getFeatureById(feature.id)!=null){this.layer.drawFeature(feature);}}}}}};GeoExt.data.FeatureStore=Ext.extend(Ext.data.Store,GeoExt.data.FeatureStoreMixin);GeoExt.data.FeatureStore.LAYER_TO_STORE=1;GeoExt.data.FeatureStore.STORE_TO_LAYER=2;Ext.namespace("GeoExt");GeoExt.MapPanel=Ext.extend(Ext.Panel,{map:null,layers:null,center:null,zoom:null,extent:null,initComponent:function(){if(!(this.map instanceof OpenLayers.Map)){this.map=new OpenLayers.Map(Ext.applyIf(this.map||{},{allOverlays:true}));}
var layers=this.layers;if(!layers||layers instanceof Array){this.layers=new GeoExt.data.LayerStore({layers:layers,map:this.map});}
if(typeof this.center=="string"){this.center=OpenLayers.LonLat.fromString(this.center);}else if(this.center instanceof Array){this.center=new OpenLayers.LonLat(this.center[0],this.center[1]);}
if(typeof this.extent=="string"){this.extent=OpenLayers.Bounds.fromString(this.extent);}else if(this.extent instanceof Array){this.extent=OpenLayers.Bounds.fromArray(this.extent);}
GeoExt.MapPanel.superclass.initComponent.call(this);},updateMapSize:function(){if(this.map){this.map.updateSize();}},renderMap:function(){var map=this.map;var setCenter=map.setCenter;map.setCenter=function(){};map.render(this.body.dom);map.setCenter=setCenter;if(map.layers.length>0){if(this.center||this.zoom!=null){map.setCenter(this.center,this.zoom);}else if(this.extent){map.zoomToExtent(this.extent);}else{map.zoomToMaxExtent();}}},afterRender:function(){GeoExt.MapPanel.superclass.afterRender.apply(this,arguments);if(!this.ownerCt){this.renderMap();}else{this.ownerCt.on("move",this.updateMapSize,this);this.ownerCt.on({"afterlayout":{fn:this.renderMap,scope:this,single:true}});}},onResize:function(){GeoExt.MapPanel.superclass.onResize.apply(this,arguments);this.updateMapSize();},onBeforeAdd:function(item){if(typeof item.addToMapPanel==="function"){item.addToMapPanel(this);}
GeoExt.MapPanel.superclass.onBeforeAdd.apply(this,arguments);},remove:function(item,autoDestroy){if(typeof item.removeFromMapPanel==="function"){item.removeFromMapPanel(this);}
GeoExt.MapPanel.superclass.remove.apply(this,arguments);},beforeDestroy:function(){if(this.ownerCt){this.ownerCt.un("move",this.updateMapSize,this);}
if(!this.initialConfig.map||!(this.initialConfig.map instanceof OpenLayers.Map)){if(this.map&&this.map.destroy){this.map.destroy();}}
delete this.map;GeoExt.MapPanel.superclass.beforeDestroy.apply(this,arguments);}});GeoExt.MapPanel.guess=function(){return Ext.ComponentMgr.all.find(function(o){return o instanceof GeoExt.MapPanel;});};Ext.reg('gx_mappanel',GeoExt.MapPanel);Ext.namespace('GeoExt','GeoExt.data');GeoExt.data.ProtocolProxy=function(config){GeoExt.data.ProtocolProxy.superclass.constructor.call(this);Ext.apply(this,config);};Ext.extend(GeoExt.data.ProtocolProxy,Ext.data.DataProxy,{protocol:null,abortPrevious:true,response:null,load:function(params,reader,callback,scope,arg){if(this.fireEvent("beforeload",this,params)!==false){var o={params:params||{},request:{callback:callback,scope:scope,arg:arg},reader:reader};var cb=OpenLayers.Function.bind(this.loadResponse,this,o);if(this.abortPrevious){this.abortRequest();}
var options={params:params,callback:cb,scope:this};Ext.applyIf(options,arg);this.response=this.protocol.read(options);}else{callback.call(scope||this,null,arg,false);}},abortRequest:function(){if(this.response){var response=this.response;if(response.priv&&typeof response.priv.abort=="function"){response.priv.abort();this.response=null;}}},loadResponse:function(o,response){if(response.success()){var result=o.reader.read(response);this.fireEvent("load",this,o,o.request.arg);o.request.callback.call(o.request.scope,result,o.request.arg,true);}else{this.fireEvent("loadexception",this,o,response);o.request.callback.call(o.request.scope,null,o.request.arg,false);}}});Ext.namespace("GeoExt");GeoExt.Action=Ext.extend(Ext.Action,{control:null,map:null,uScope:null,uHandler:null,uToggleHandler:null,uCheckHandler:null,constructor:function(config){this.uScope=config.scope;this.uHandler=config.handler;this.uToggleHandler=config.toggleHandler;this.uCheckHandler=config.checkHandler;config.scope=this;config.handler=this.pHandler;config.toggleHandler=this.pToggleHandler;config.checkHandler=this.pCheckHandler;var ctrl=this.control=config.control;delete config.control;if(ctrl){if(config.map){config.map.addControl(ctrl);delete config.map;}
ctrl.events.on({activate:this.onCtrlActivate,deactivate:this.onCtrlDeactivate,scope:this});}
arguments.callee.superclass.constructor.call(this,config);},pHandler:function(cmp){var ctrl=this.control;if(ctrl&&ctrl.type==OpenLayers.Control.TYPE_BUTTON){ctrl.trigger();}
if(this.uHandler){this.uHandler.apply(this.uScope,arguments);}},pToggleHandler:function(cmp,state){this.changeControlState(state);if(this.uToggleHandler){this.uToggleHandler.apply(this.uScope,arguments);}},pCheckHandler:function(cmp,state){this.changeControlState(state);if(this.uCheckHandler){this.uCheckHandler.apply(this.uScope,arguments);}},changeControlState:function(state){if(state){if(!this._activating){this._activating=true;this.control.activate();this._activating=false;}}else{if(!this._deactivating){this._deactivating=true;this.control.deactivate();this._deactivating=false;}}},onCtrlActivate:function(){var ctrl=this.control;if(ctrl.type==OpenLayers.Control.TYPE_BUTTON){this.enable();}else{this.safeCallEach("toggle",[true]);this.safeCallEach("setChecked",[true]);}},onCtrlDeactivate:function(){var ctrl=this.control;if(ctrl.type==OpenLayers.Control.TYPE_BUTTON){this.disable();}else{this.safeCallEach("toggle",[false]);this.safeCallEach("setChecked",[false]);}},safeCallEach:function(fnName,args){var cs=this.items;for(var i=0,len=cs.length;i<len;i++){if(cs[i][fnName]){cs[i][fnName].apply(cs[i],args);}}}});Ext.namespace("GeoExt.data");GeoExt.data.WMSCapabilitiesStore=function(c){c=c||{};GeoExt.data.WMSCapabilitiesStore.superclass.constructor.call(this,Ext.apply(c,{proxy:c.proxy||(!c.data?new Ext.data.HttpProxy({url:c.url,disableCaching:false,method:"GET"}):undefined),reader:new GeoExt.data.WMSCapabilitiesReader(c,c.fields)}));};Ext.extend(GeoExt.data.WMSCapabilitiesStore,Ext.data.Store);GeoExt.VERSION_NUMBER='release-0.5';

Ext.namespace('GeoExt.grid');

/** api: constructor
 *  .. class:: FeatureSelectionModel
 *
 *      A row selection model which enables automatic selection of features
 *      in the map when rows are selected in the grid and vice-versa.
 */

/** api: example
 *  Sample code to create a feature grid with a feature selection model:
 *  
 *  .. code-block:: javascript
 *
 *       var gridPanel = new Ext.grid.GridPanel({
 *          title: "Feature Grid",
 *          region: "east",
 *          store: store,
 *          width: 320,
 *          columns: [{
 *              header: "Name",
 *              width: 200,
 *              dataIndex: "name"
 *          }, {
 *              header: "Elevation",
 *              width: 100,
 *              dataIndex: "elevation"
 *          }],
 *          sm: new GeoExt.grid.FeatureSelectionModel() 
 *      });
 */

GeoExt.grid.FeatureSelectionModelMixin = {

    /** api: config[autoActivateControl]
     *  ``Boolean`` If true the select feature control is activated and
     *  deactivated when binding and unbinding. Defaults to true.
     */
    autoActivateControl: true,

    /** api: config[layerFromStore]
     *  ``Boolean`` If true, and if the constructor is passed neither a
     *  layer nor a select feature control, a select feature control is
     *  created using the layer found in the grid's store. Set it to
     *  false if you want to manually bind the selection model to a
     *  layer. Defaults to true.
     */
    layerFromStore: true,

    /** api: config[selectControl]
     *
     *  ``OpenLayers.Control.SelectFeature`` A select feature control. If not
     *  provided one will be created.  If provided any "layer" config option
     *  will be ignored, and its "multiple" option will be used to configure
     *  the selectionModel.  If an ``Object`` is provided here, it will be
     *  passed as config to the SelectFeature constructor, and the "layer"
     *  config option will be used for the layer.
     */

    /** private: property[selectControl] 
     *  ``OpenLayers.Control.SelectFeature`` The select feature control 
     *  instance. 
     */ 
    selectControl: null, 
    
    /** api: config[layer]
     *  ``OpenLayers.Layer.Vector`` The vector layer used for the creation of
     *  the select feature control, it must already be added to the map. If not
     *  provided, the layer bound to the grid's store, if any, will be used.
     */

    /** private: property[bound]
     *  ``Boolean`` Flag indicating if the selection model is bound.
     */
    bound: false,
    
    /** private: property[superclass]
     *  ``Ext.grid.AbstractSelectionModel`` Our superclass.
     */
    superclass: null,

    /** private */
    constructor: function(config) {
        config = config || {};
        if(config.selectControl instanceof OpenLayers.Control.SelectFeature) { 
            if(!config.singleSelect) {
                var ctrl = config.selectControl;
                config.singleSelect = !(ctrl.multiple || !!ctrl.multipleKey);
            }
        } else if(config.layer instanceof OpenLayers.Layer.Vector) {
            this.selectControl = this.createSelectControl(
                config.layer, config.selectControl
            );
            delete config.layer;
            delete config.selectControl;
        }
        this.superclass = arguments.callee.superclass;
        this.superclass.constructor.call(this, config);
    },
    
    /** private: method[initEvents]
     *
     *  Called after this.grid is defined
     */
    initEvents: function() {
        this.superclass.initEvents.call(this);
        if(this.layerFromStore) {
            var layer = this.grid.getStore() && this.grid.getStore().layer;
            if(layer &&
               !(this.selectControl instanceof OpenLayers.Control.SelectFeature)) {
                this.selectControl = this.createSelectControl(
                    layer, this.selectControl
                );
            }
        }
        if(this.selectControl) {
            this.bind(this.selectControl);
        }
    },

    /** private: createSelectControl
     *  :param layer: ``OpenLayers.Layer.Vector`` The vector layer.
     *  :param config: ``Object`` The select feature control config.
     *
     *  Create the select feature control.
     */
    createSelectControl: function(layer, config) {
        config = config || {};
        var singleSelect = config.singleSelect !== undefined ?
                           config.singleSelect : this.singleSelect;
        config = OpenLayers.Util.extend({
            toggle: true,
            multipleKey: singleSelect ? null :
                (Ext.isMac ? "metaKey" : "ctrlKey")
        }, config);
        var selectControl = new OpenLayers.Control.SelectFeature(
            layer, config
        );
        layer.map.addControl(selectControl);
        return selectControl;
    },
    
    /** api: method[bind]
     *
     *  :param obj: ``OpenLayers.Layer.Vector`` or
     *  ``OpenLayers.Control.SelectFeature`` The object this selection model
     *      should be bound to, either a vector layeer or a select feature
     *      control.
     *  :param options: ``Object`` An object with a "controlConfig"
     *      property referencing the configuration object to pass to the
     *      ``OpenLayers.Control.SelectFeature`` constructor.
     *  :return: ``OpenLayers.Control.SelectFeature`` The select feature
     *  control this selection model uses.
     *
     *  Bind the selection model to a layer or a SelectFeature control.
     */
    bind: function(obj, options) {
        if(!this.bound) {
            options = options || {};
            this.selectControl = obj;
            if(obj instanceof OpenLayers.Layer.Vector) {
                this.selectControl = this.createSelectControl(
                    obj, options.controlConfig
                );
            }
            if(this.autoActivateControl) {
                this.selectControl.activate();
            }
            var layers = this.getLayers();
            for(var i = 0, len = layers.length; i < len; i++) {
                layers[i].events.on({
                    featureselected: this.featureSelected,
                    featureunselected: this.featureUnselected,
                    scope: this
                });
            }
            this.on("rowselect", this.rowSelected, this);
            this.on("rowdeselect", this.rowDeselected, this);
            this.bound = true;
        }
        return this.selectControl;
    },
    
    /** api: method[unbind]
     *  :return: ``OpenLayers.Control.SelectFeature`` The select feature
     *      control this selection model used.
     *
     *  Unbind the selection model from the layer or SelectFeature control.
     */
    unbind: function() {
        var selectControl = this.selectControl;
        if(this.bound) {
            var layers = this.getLayers();
            for(var i = 0, len = layers.length; i < len; i++) {
                layers[i].events.un({
                    featureselected: this.featureSelected,
                    featureunselected: this.featureUnselected,
                    scope: this
                });
            }
            this.un("rowselect", this.rowSelected, this);
            this.un("rowdeselect", this.rowDeselected, this);
            if(this.autoActivateControl) {
                selectControl.deactivate();
            }
            this.selectControl = null;
            this.bound = false;
        }
        return selectControl;
    },
    
    /** private: method[featureSelected]
     *  :param evt: ``Object`` An object with a feature property referencing
     *                         the selected feature.
     */
    featureSelected: function(evt) {
        if(!this._selecting) {
            var store = this.grid.store;
            var row = store.findBy(function(record, id) {
                return record.data.feature == evt.feature;
            });
            if(row != -1 && !this.isSelected(row)) {
                this._selecting = true;
                this.selectRow(row, !this.singleSelect);
                this._selecting = false;
                // focus the row in the grid to ensure it is visible
                this.grid.getView().focusRow(row);
            }
        }
    },
    
    /** private: method[featureUnselected]
     *  :param evt: ``Object`` An object with a feature property referencing
     *                         the unselected feature.
     */
    featureUnselected: function(evt) {
        if(!this._selecting) {
            var store = this.grid.store;
            var row = store.findBy(function(record, id) {
                return record.data.feature == evt.feature;
            });
            if(row != -1 && this.isSelected(row)) {
                this._selecting = true;
                this.deselectRow(row); 
                this._selecting = false;
                this.grid.getView().focusRow(row);
            }
        }
    },
    
    /** private: method[rowSelected]
     *  :param model: ``Ext.grid.RowSelectModel`` The row select model.
     *  :param row: ``Integer`` The row index.
     *  :param record: ``Ext.data.Record`` The record.
     */
    rowSelected: function(model, row, record) {
        var feature = record.data.feature;
        if(!this._selecting && feature) {
            var layers = this.getLayers();
            for(var i = 0, len = layers.length; i < len; i++) {
                if(layers[i].selectedFeatures.indexOf(feature) == -1) {
                    this._selecting = true;
                    this.selectControl.select(feature);
                    this._selecting = false;
                    break;
                }
            }
         }
    },
    
    /** private: method[rowDeselected]
     *  :param model: ``Ext.grid.RowSelectModel`` The row select model.
     *  :param row: ``Integer`` The row index.
     *  :param record: ``Ext.data.Record`` The record.
     */
    rowDeselected: function(model, row, record) {
        var feature = record.data.feature;
        if(!this._selecting && feature) {
            var layers = this.getLayers();
            for(var i = 0, len = layers.length; i < len; i++) {
                if(layers[i].selectedFeatures.indexOf(feature) != -1) {
                    this._selecting = true;
                    this.selectControl.unselect(feature);
                    this._selecting = false;
                    break;
                }
            }
        }
    },

    /** private: method[getLayers]
     *  Return the layers attached to the select feature control.
     */
    getLayers: function() {
        return this.selectControl.layers || [this.selectControl.layer];
    }
};

GeoExt.grid.FeatureSelectionModel = Ext.extend(
    Ext.grid.RowSelectionModel,
    GeoExt.grid.FeatureSelectionModelMixin
);
