package com.cgi.eoss.ftep.model.internal;

import lombok.Value;

@Value(staticConstructor = "of")
public class Pair<K, V> {
    K key;
    V value;
}