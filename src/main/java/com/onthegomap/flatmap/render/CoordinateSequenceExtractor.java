package com.onthegomap.flatmap.render;

import com.onthegomap.flatmap.FeatureCollector;
import com.onthegomap.flatmap.geo.GeoUtils;
import com.onthegomap.flatmap.geo.TileCoord;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CoordinateSequenceExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateSequenceExtractor.class);

  static Geometry reassembleLineString(List<List<CoordinateSequence>> geoms) {
    Geometry geom;
    List<LineString> lineStrings = new ArrayList<>();
    for (List<CoordinateSequence> inner : geoms) {
      for (CoordinateSequence coordinateSequence : inner) {
        lineStrings.add(GeoUtils.JTS_FACTORY.createLineString(coordinateSequence));
      }
    }
    geom = GeoUtils.createMultiLineString(lineStrings);
    return geom;
  }

  @NotNull
  static Geometry reassemblePolygon(FeatureCollector.Feature feature, TileCoord tile,
    List<List<CoordinateSequence>> geoms) {
    Geometry geom;
    int numGeoms = geoms.size();
    Polygon[] polygons = new Polygon[numGeoms];
    for (int i = 0; i < numGeoms; i++) {
      List<CoordinateSequence> group = geoms.get(i);
      LinearRing first = GeoUtils.JTS_FACTORY.createLinearRing(group.get(0));
      LinearRing[] rest = new LinearRing[group.size() - 1];
      for (int j = 1; j < group.size(); j++) {
        CoordinateSequence seq = group.get(j);
        CoordinateSequences.reverse(seq);
        rest[j - 1] = GeoUtils.JTS_FACTORY.createLinearRing(seq);
      }
      polygons[i] = GeoUtils.JTS_FACTORY.createPolygon(first, rest);
    }
    geom = GeoUtils.JTS_FACTORY.createMultiPolygon(polygons);
    return geom;
  }

  static List<List<CoordinateSequence>> extractGroups(Geometry geom, double minSize) {
    List<List<CoordinateSequence>> result = new ArrayList<>();
    extractGroups(geom, result, minSize);
    return result;
  }

  private static void extractGroups(Geometry geom, List<List<CoordinateSequence>> groups, double minSize) {
    if (geom.isEmpty()) {
      // ignore
    } else if (geom instanceof GeometryCollection) {
      for (int i = 0; i < geom.getNumGeometries(); i++) {
        extractGroups(geom.getGeometryN(i), groups, minSize);
      }
    } else if (geom instanceof Polygon polygon) {
      extractGroupsFromPolygon(groups, minSize, polygon);
    } else if (geom instanceof LinearRing linearRing) {
      extractGroups(GeoUtils.JTS_FACTORY.createPolygon(linearRing), groups, minSize);
    } else if (geom instanceof LineString lineString) {
      if (lineString.getLength() >= minSize) {
        groups.add(List.of(lineString.getCoordinateSequence()));
      }
    } else {
      throw new RuntimeException("unrecognized geometry type: " + geom.getGeometryType());
    }
  }

  private static void extractGroupsFromPolygon(List<List<CoordinateSequence>> groups, double minSize, Polygon polygon) {
    CoordinateSequence outer = polygon.getExteriorRing().getCoordinateSequence();
    double outerArea = Area.ofRingSigned(outer);
    if (outerArea > 0) {
      CoordinateSequences.reverse(outer);
    }
    if (Math.abs(outerArea) >= minSize) {
      List<CoordinateSequence> group = new ArrayList<>(1 + polygon.getNumInteriorRing());
      groups.add(group);
      group.add(outer);
      for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
        CoordinateSequence inner = polygon.getInteriorRingN(i).getCoordinateSequence();
        double innerArea = Area.ofRingSigned(inner);
        if (innerArea > 0) {
          CoordinateSequences.reverse(inner);
        }
        if (Math.abs(innerArea) >= minSize) {
          group.add(inner);
        }
      }
    }
  }
}