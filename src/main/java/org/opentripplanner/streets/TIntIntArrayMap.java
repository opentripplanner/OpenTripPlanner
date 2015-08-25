package org.opentripplanner.streets;

import gnu.trove.TIntCollection;
import gnu.trove.function.TIntFunction;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;

import java.util.Map;

/**
 * Stub for a class that implements the Map interface backed by an array.
 */
public class TIntIntArrayMap implements TIntIntMap {

    int[] array;

    @Override
    public int getNoEntryKey() {
        return 0;
    }

    @Override
    public int getNoEntryValue() {
        return 0;
    }

    @Override
    public int put(int key, int value) {
        return 0;
    }

    @Override
    public int putIfAbsent(int key, int value) {
        return 0;
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> map) {

    }

    @Override
    public void putAll(TIntIntMap map) {

    }

    @Override
    public int get(int key) {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int remove(int key) {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public TIntSet keySet() {
        return null;
    }

    @Override
    public int[] keys() {
        return new int[0];
    }

    @Override
    public int[] keys(int[] array) {
        return new int[0];
    }

    @Override
    public TIntCollection valueCollection() {
        return null;
    }

    @Override
    public int[] values() {
        return new int[0];
    }

    @Override
    public int[] values(int[] array) {
        return new int[0];
    }

    @Override
    public boolean containsValue(int val) {
        return false;
    }

    @Override
    public boolean containsKey(int key) {
        return false;
    }

    @Override
    public TIntIntIterator iterator() {
        return null;
    }

    @Override
    public boolean forEachKey(TIntProcedure procedure) {
        return false;
    }

    @Override
    public boolean forEachValue(TIntProcedure procedure) {
        return false;
    }

    @Override
    public boolean forEachEntry(TIntIntProcedure procedure) {
        return false;
    }

    @Override
    public void transformValues(TIntFunction function) {

    }

    @Override
    public boolean retainEntries(TIntIntProcedure procedure) {
        return false;
    }

    @Override
    public boolean increment(int key) {
        return false;
    }

    @Override
    public boolean adjustValue(int key, int amount) {
        return false;
    }

    @Override
    public int adjustOrPutValue(int key, int adjust_amount, int put_amount) {
        return 0;
    }
}
