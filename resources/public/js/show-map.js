/*global $:true, document: true, OpenLayers */
var map;
var load_main = function (opts) {
    'use strict';
    var height = $(document).height() - 80,
        i,
        mapKey = 'AhPr6BLPr7N4Y8xbymEkGJ338DXCuIz0BFUiwe655NvylmRUXcXKed-162H_cabU',
        mapInfo = $('#mapInfo'),
        bounds,
        tables = [],
        styles = [],
        layer;

    $('#show-map').height(height);
    $('#show-map').css('background', opts.mapInfo.bgcolor);


    var osmLayer = new OpenLayers.Layer.OSM("OpenStreetMap");
    var toner = new OpenLayers.Layer.Stamen("toner");
    var terrain = new OpenLayers.Layer.Stamen("terrain");
    var tonerLite = new OpenLayers.Layer.Stamen("toner-lite");



    var road = new OpenLayers.Layer.Bing({
        name: "Bing Road",
        key: mapKey,
        type: "Road"
    });


    var roadLite = new OpenLayers.Layer.Bing({
        name: "Bing Lite Road",
        key: mapKey,
        type: "Road"
    });
    roadLite.setOpacity(0.4);


    var bing = new OpenLayers.Layer.Bing({
        name: "Bing Hybird",
        key: mapKey,
        type: "Aerial"
    });
//    bing.setOpacity(0.6);
    

    var gphy = new OpenLayers.Layer.Google(
        "Google Physical",
        {type: google.maps.MapTypeId.TERRAIN}
    );

    var gmap = new OpenLayers.Layer.Google(
        "Google Streets", // the default
        {numZoomLevels: 20}
    );

    var aliasproj = new OpenLayers.Projection("EPSG:3857");
    aliasproj.projection = osmLayer.projection = aliasproj;

    map = new OpenLayers.Map('show-map', {
        projection: new OpenLayers.Projection("EPSG:3857"),
        displayProjection: new OpenLayers.Projection("EPSG:4326"),
        numZoomLevels: 20
    });

    map.events.register('zoomend', map, function (evnt) {
        $('#resolution').html('Scale: ' + map.getScale());
        $('#zoom').html('Zoom :' + map.getZoom());
        $('#bbox').html(map.getExtent().transform(map.projection, map.displayProjection).toArray().join(','));
    });

    for (i = 0; i < opts.mapInfo.layers.length; i++) {
        var layerInfo = opts.mapInfo.layers[i];
        tables.push(layerInfo.table);
        styles.push(layerInfo.style);
    }

    // for (var i = 0; i < tables.length; i += 1) {
    //     var table = tables[i],
    //         style = styles[i];

    //     map.addLayer(new OpenLayers.Layer.WMS(
    //         table, '/services',
    //         {layers: table, styles: style},
    //         {isBaseLayer: false}
    //     ));

    // };

    layer = new OpenLayers.Layer.WMS(
        "OpenLayers WMS",
        "/services",
        {
            layers: tables.join(','),
            styles: styles.join(',')
        }, {singleTile: false,
            isBaseLayer: true}
    );



    map.addLayer(layer);
    
    map.addLayers([road, roadLite, bing, tonerLite, toner, terrain, gphy, gmap, osmLayer]);

    map.addControl(new OpenLayers.Control.LayerSwitcher( 
        {'div': OpenLayers.Util.getElement('layerswitcher')}
    ));

    bounds = new OpenLayers.Bounds.fromArray(opts.mapInfo.bbox);
    map.zoomToExtent(bounds.transform(map.displayProjection, map.projection));

};
