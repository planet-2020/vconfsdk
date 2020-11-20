package com.kedacom.vconf.sdk.utils.collection;

import java.util.ArrayDeque;

public class EvictingDeque<E> extends ArrayDeque<E> {
    private final int maxSize;

    public EvictingDeque(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException(String.format("maxSize (%s) must > 0", maxSize));
        }
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public void addFirst(E e) {
        if (size() == maxSize) {
            pollLast();
        }
        super.addFirst(e);
    }

    @Override
    public void addLast(E e) {
        if (size() == maxSize) {
            pollFirst();
        }
        super.addLast(e);
    }

    public E pickSecondLast(){
        if (size()<2){
            return null;
        }
        E e = pollLast();
        E secondLast = peekLast();
        offerLast(e);
        return secondLast;
    }

}
