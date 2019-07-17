


package com.kedacom.vconf.sdk.utils.collection;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class CompatibleConcurrentLinkedDeque<E> implements Iterable<E>, Collection<E> {

    private Deque<E> deque;

    public CompatibleConcurrentLinkedDeque(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            deque = new ConcurrentLinkedDeque<>();
        }else{
            deque = new LinkedBlockingDeque<>();
        }
    }

    public boolean offerFirst(E e){
        return deque.offerFirst(e);
    }

    public boolean offerLast(E e){
        return deque.offerLast(e);
    }

    public E pollFirst(){
        return deque.pollFirst();
    }

    public E pollLast(){
        return deque.pollLast();
    }

    public E peekFirst(){
        return deque.peekFirst();
    }

    public E peekLast(){
        return deque.peekLast();
    }


    public boolean isEmpty(){
        return deque.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return deque.contains(o);
    }

    public int size(){
        return deque.size();
    }

    public void clear(){
        deque.clear();
    }


    @NonNull
    @Override
    public Iterator<E> iterator() {
        return deque.iterator();
    }

    @Override
    public Object[] toArray() {
        return deque.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return deque.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return deque.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return deque.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return deque.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return deque.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return deque.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return deque.retainAll(c);
    }

    public Iterator<E> descendingIterator(){
        return deque.descendingIterator();
    }

}
