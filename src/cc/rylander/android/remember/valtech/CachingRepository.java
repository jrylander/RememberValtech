/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.graphics.Bitmap;
import cc.rylander.android.remember.QuizRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Johan Rylander (johan@rylander.cc>
 * on 2011-12-03
 */
public class CachingRepository implements QuizRepository {

    QuizRepository delegate;
    Map<Integer,Future<Bitmap>> cache = Collections.synchronizedMap(new HashMap<Integer, Future<Bitmap>>());
    ExecutorService james = Executors.newFixedThreadPool(3);

    public CachingRepository(QuizRepository delegate) {
        this.delegate = delegate;
    }

    public Bitmap getMutableBitmap(int pos) throws ExecutionException, InterruptedException {
        if (cache.containsKey(pos)) return cache.get(pos).get();
        return delegate.getMutableBitmap(pos);
    }

    public String getName(int pos) {
        return delegate.getName(pos);
    }

    public boolean isCached(int pos) {
        return cache.containsKey(pos) && cache.get(pos).isDone();
    }

    public int prevPos(int pos) {
        return delegate.prevPos(pos);
    }

    public int nextPos(int pos) {
        return delegate.nextPos(pos);
    }
}
