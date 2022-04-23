package com.semibit.ezandroidutils.models;

public class KeyValuePair<K,V> {
    public K key;
    public V val;

    public KeyValuePair(K key, V val) {
        this.key = key;
        this.val = val;
    }
}
