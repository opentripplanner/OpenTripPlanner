package org.opentripplanner.framework.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;

/** An instance of CoordinateSequence that can be efficiently extended */
public final class CoordinateArrayListSequence implements CoordinateSequence, Cloneable {

  private ArrayList<Coordinate> coordinates;

  public CoordinateArrayListSequence() {
    coordinates = new ArrayList<>();
  }

  public CoordinateArrayListSequence(ArrayList<Coordinate> coordinates) {
    this.coordinates = new ArrayList<>(coordinates);
  }

  @Override
  public int getDimension() {
    return 2;
  }

  @Override
  public Coordinate getCoordinate(int i) {
    return coordinates.get(i);
  }

  @Override
  public Coordinate getCoordinateCopy(int i) {
    return new Coordinate(coordinates.get(i));
  }

  @Override
  public void getCoordinate(int index, Coordinate coord) {
    Coordinate internalCoord = coordinates.get(index);
    coord.x = internalCoord.x;
    coord.y = internalCoord.y;
  }

  @Override
  public double getX(int index) {
    return coordinates.get(index).x;
  }

  @Override
  public double getY(int index) {
    return coordinates.get(index).y;
  }

  @Override
  public double getOrdinate(int index, int ordinateIndex) {
    return switch (ordinateIndex) {
      case 0 -> coordinates.get(index).x;
      case 1 -> coordinates.get(index).y;
      default -> throw new IllegalArgumentException(
        "ordinateIndex out of range[0, 1]: " + ordinateIndex
      );
    };
  }

  @Override
  public int size() {
    return coordinates.size();
  }

  @Override
  public void setOrdinate(int index, int ordinateIndex, double value) {
    switch (ordinateIndex) {
      case 0 -> coordinates.get(index).x = value;
      case 1 -> coordinates.get(index).y = value;
      default -> throw new UnsupportedOperationException();
    }
  }

  @Override
  public Coordinate[] toCoordinateArray() {
    return coordinates.toArray(new Coordinate[0]);
  }

  @Override
  public Envelope expandEnvelope(Envelope env) {
    for (Coordinate c : coordinates) {
      env.expandToInclude(c);
    }
    return env;
  }

  @Override
  public CoordinateSequence copy() {
    return new CoordinateArrayListSequence(coordinates);
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public CoordinateArrayListSequence clone() {
    return new CoordinateArrayListSequence(coordinates);
  }

  public void extend(Coordinate[] newCoordinates) {
    coordinates.addAll(Arrays.asList(newCoordinates));
  }

  public void extend(Coordinate[] newCoordinates, int start) {
    extend(newCoordinates, start, newCoordinates.length);
  }

  public void extend(Coordinate[] newCoordinates, int start, int end) {
    coordinates.addAll(Arrays.asList(newCoordinates).subList(start, end));
  }

  public void add(Coordinate newCoordinate) {
    coordinates.add(newCoordinate);
  }

  public void clear() {
    coordinates = new ArrayList<>();
  }
}
