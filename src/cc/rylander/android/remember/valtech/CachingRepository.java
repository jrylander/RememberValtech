/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.graphics.Bitmap;
import android.util.Log;
import cc.rylander.android.remember.QuizRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Johan Rylander (johan@rylander.cc>
 * on 2011-12-03
 */
public class CachingRepository implements QuizRepository {

    QuizRepository delegate;
    final ReentrantLock currentLock = new ReentrantLock();
    final Condition currentIsValid = currentLock.newCondition();
    Bitmap previous, current;
    ExecutorService james = Executors.newFixedThreadPool(1);

    public CachingRepository(QuizRepository delegate) {
        this.delegate = delegate;
        james.execute(new UpdateCurrent());
    }

    class UpdateCurrent implements Runnable {
        public void run() {
            Bitmap mutableBitmap = delegate.getMutableBitmap();
            currentLock.lock();
            try {
                current = mutableBitmap;
                currentIsValid.signalAll();
            } finally {
                currentLock.unlock();
            }
        }
    }

    public int size() {
        return delegate.size();
    }

    public Bitmap getMutableBitmap() {
        currentLock.lock();
        try {
            while (null == current) {
                currentIsValid.await();
            }
            return current;
        } catch (InterruptedException e) {
            Log.w("RememberValtech", "Interrupted while waiting for image download", e);
        } finally {
            currentLock.unlock();
        }
        return null;
    }

    public String getName() {
        return delegate.getName();
    }

    public void next() {
        currentLock.lock();
        try {
            previous = current;
            current = null;
            delegate.next();
            james.execute(new UpdateCurrent());
        } finally {
            currentLock.unlock();
        }
    }

    public void prev() {
        currentLock.lock();
        try {
            if (null != previous) {
                current = previous;
                previous =  null;
                delegate.prev();
            }
        } finally {
            currentLock.unlock();
        }
    }

    public boolean isCached() {
        currentLock.lock();
        try {
            return null != current;
        } finally {
            currentLock.unlock();
        }
    }
}
