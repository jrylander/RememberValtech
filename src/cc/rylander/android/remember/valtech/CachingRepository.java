/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.graphics.Bitmap;
import android.util.Log;
import cc.rylander.android.remember.QuizRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Johan Rylander (johan@rylander.cc>
 * on 2011-12-03
 */
public class CachingRepository implements QuizRepository {

    QuizRepository delegate;

    ExecutorService james = Executors.newFixedThreadPool(2);

    final static int CACHE_SIZE = 5;
    LinkedList<CacheEntry> cache = new LinkedList<CacheEntry>();
    class CacheEntry {
        int pos;
        Future<Bitmap> bitmap;

        CacheEntry(int pos, Future<Bitmap> bitmap) {
            this.pos = pos;
            this.bitmap = bitmap;
        }
    }

    Future<Bitmap> cachedBitmap(int pos) {
        for (CacheEntry entry : cache) {
            if (entry.pos == pos) return entry.bitmap;
        }
        return null;
    }

    void updateCacheWith(final int pos, boolean forward) {
        if (null != cachedBitmap(pos)) return;

        final CacheEntry entry = new CacheEntry(pos, james.submit(new Callable<Bitmap>() {
            public Bitmap call() throws Exception {
                return CachingRepository.this.delegate.getBitmap(pos);
            }
        }));
        if (forward) {
            cache.addLast(entry);
            if (cache.size() > CACHE_SIZE) cache.removeFirst();
        } else {
            cache.addFirst(entry);
            if (cache.size() > CACHE_SIZE) cache.removeLast();
        }
    }

    public CachingRepository(QuizRepository delegate) {
        this.delegate = delegate;
        updateCacheWith(delegate.prevPos(0), true);
        updateCacheWith(0, true);
        for (int i=0, toFetch = delegate.nextPos(0); i < CACHE_SIZE - 2; i++) {
            updateCacheWith(toFetch, true);
            toFetch = delegate.nextPos(toFetch);
        }
    }

    public Bitmap getBitmap(int pos) throws IOException {
        if (null != cachedBitmap(pos)) {
            try {
                return cachedBitmap(pos).get();
            } catch (Exception e) {
                Log.w("RememberValtech", "Unable to download image", e);
                throw new IOException(e.getMessage());
            }
        }
        return delegate.getBitmap(pos);
    }

    public String getName(int pos) {
        return delegate.getName(pos);
    }

    public boolean isCached(int pos) {
        return null != cachedBitmap(pos) && cachedBitmap(pos).isDone();
    }

    public int prevPos(int pos) {
        final int prevPos = delegate.prevPos(pos);
        int toFetch = prevPos;
        for (int i=0; i < Math.min(2, CACHE_SIZE); i++) {
            updateCacheWith(toFetch, false);
            toFetch = delegate.prevPos(toFetch);
        }
        return prevPos;
    }

    public int nextPos(int pos) {
        final int nextPos = delegate.nextPos(pos);
        int toFetch = nextPos;
        for (int i=0; i < CACHE_SIZE - 1; i++) {
            updateCacheWith(toFetch, true);
            toFetch = delegate.nextPos(toFetch);
        }
        return nextPos;
    }
}
