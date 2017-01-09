var test = require('tape').test,
    assert = require('assert'),
    fs = require('fs'),
    g = require('../GeoRSSToGeoJSON').GeoRSSToGeoJSON;

if (!process.browser) {
    var xmldom = require('xmldom');
}

test('Simple GeoRSS - Point', function(t) {
    t.deepEqual(
        g(toDOM(fs.readFileSync('test/data/simple-with-lat-long.xml'))),
        JSON.parse(fs.readFileSync('test/data/simple-with-lat-long.geojson')),
        'simple GeoRSS with <geo:lat> and <geo:long> tags'
    );
    t.deepEqual(
        g(toDOM(fs.readFileSync('test/data/simple-georss-point.xml'))),
        JSON.parse(fs.readFileSync('test/data/simple-georss-point.geojson')),
        'simple GeoRSS with <georss:point> tag'
    );
    t.deepEqual(
        g(toDOM(fs.readFileSync('test/data/items-with-no-geo-are-skipped.xml'))),
        JSON.parse(fs.readFileSync('test/data/items-with-no-geo-are-skipped.geojson')),
        'Items with no geocoding are skipped'
    );
    t.end();
});
test('Simple GeoRSS - LineString', function(t) {
    t.deepEqual(
        g(toDOM(fs.readFileSync('test/data/simple-georss-line.xml'))),
        JSON.parse(fs.readFileSync('test/data/simple-georss-line.geojson')),
        'simple GeoRSS with <georss:line> tag'
    );
    t.end();
});
test('Simple GeoRSS - Polygon', function(t) {
    t.deepEqual(
        g(toDOM(fs.readFileSync('test/data/simple-georss-polygon.xml'))),
        JSON.parse(fs.readFileSync('test/data/simple-georss-polygon.geojson')),
        'simple GeoRSS with <georss:polygon> tag'
    );
    t.end();
});
test('Enclosure', function(t) {
    t.deepEqual(
        g(toDOM(fs.readFileSync('test/data/enclosure-image.xml'))),
        JSON.parse(fs.readFileSync('test/data/enclosure-image.geojson')),
        'enclosure type image is imported as img'
    );
    t.end();
});
test('media:content', function(t) {
    t.deepEqual(
        g(toDOM(fs.readFileSync('test/data/media-content-image.xml'))),
        JSON.parse(fs.readFileSync('test/data/media-content-image.geojson')),
        'media:content type image is imported as img'
    );
    t.end();
});

function toDOM(_) {
    if (typeof DOMParser === 'undefined') {
        return (new xmldom.DOMParser()).parseFromString(_.toString());
    } else {
        return (new DOMParser()).parseFromString(_, 'text/xml');
    }
}
