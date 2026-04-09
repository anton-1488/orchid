package com.subgraph.orchid.data;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class RandomList<E> {
    private static final SecureRandom random = new SecureRandom();
    private final List<E> list;

    public RandomList() {
        list = new ArrayList<>();
    }

    public synchronized boolean add(E o) {
        if (!contains(o)) {
            return list.add(o);
        }
        return false;
    }

    public boolean contains(E o) {
        return list.contains(o);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public synchronized void clear() {
        list.clear();
    }

    public synchronized boolean remove(E o) {
        return list.remove(o);
    }

    public int size() {
        return list.size();
    }

    public E getRandomElement() {
        int id = random.nextInt(list.size());
        return list.get(id);
    }
}
