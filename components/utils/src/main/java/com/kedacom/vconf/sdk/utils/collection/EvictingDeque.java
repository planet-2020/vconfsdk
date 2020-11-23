package com.kedacom.vconf.sdk.utils.collection;

import java.util.ArrayDeque;
/**
 * 驱逐式双向队列
 *
 * 使用该队列需指定一个容量，该队列的项数量不会超过该容量。
 * 当队列满时，再添加项，若从尾部添加则挤掉首部的项，若从首部添加则挤掉尾部的项。
 * 该类非线程安全。
 * 该类不接受null项。
 */
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

}
