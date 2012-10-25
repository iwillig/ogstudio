/*global $:true, document: true, OpenLayers */
var map;
var load_main = function (opts) {
    'use strict';
    var height = $(document).height() - 80,
        i,
        mapInfo = $('#mapInfo'),
        bounds,
        tables = [],
        styles = [],
        layer;

    $('#show-map').height(height);
    $('#show-map').css('background', opts.mapInfo.bgcolor);

    map = new OpenLayers.Map('show-map', {
        numZoomLevels: 20
    });

    map.events.register('zoomend', map, function (evnt) {
        $('#resolution').html('Scale: ' + map.getScale());
        $('#zoom').html('Zoom :' + map.getZoom());
        $('#bbox').html(map.getExtent().toArray().join(','));
    });

    for (i = 0; i < opts.mapInfo.layers.length; i++) {
        var layerInfo = opts.mapInfo.layers[i];
        tables.push(layerInfo.table);
        styles.push(layerInfo.style);
    }

    layer = new OpenLayers.Layer.WMS(
        "OpenLayers WMS",
        "/services",
        {
            layers: tables.join(','),
            styles: styles.join(',')
        }
    );
    map.addLayer(layer);

    map.addControl(new OpenLayers.Control.LayerSwitcher());
    map.addControl(new OpenLayers.Control.Permalink({anchor: true}));
    bounds = new OpenLayers.Bounds.fromArray(opts.mapInfo.bbox);
    map.zoomToExtent(bounds);

};