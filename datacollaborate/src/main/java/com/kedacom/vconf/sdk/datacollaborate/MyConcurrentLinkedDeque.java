


package com.kedacom.vconf.sdk.datacollaborate;

import android.os.Build;

import com.kedacom.vconf.sdk.base.KLog;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;

import androidx.annotation.NonNull;

public class MyConcurrentLinkedDeque<E> implements Iterable<E>{

    private Deque<E> deque;

    public MyConcurrentLinkedDeque(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            deque = new ConcurrentLinkedDeque<>();
            KLog.p("deque is ConcurrentLinkedDeque");
        }else{
            deque = new LinkedBlockingDeque<>();
            KLog.p("deque is LinkedBlockingDeque");
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

    public boolean remove(E e){
        return deque.remove(e);
    }

    public boolean isEmpty(){
        return deque.isEmpty();
    }

    public int size(){
        return deque.size();
    }

    public void clear(){
        deque.clear();
    }

    public void addAll(MyConcurrentLinkedDeque<? extends E> myConcurrentLinkedDeque){
        deque.addAll(myConcurrentLinkedDeque.deque);
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return deque.iterator();
    }

    public Iterator<E> descendingIterator(){
        return deque.descendingIterator();
    }

}
