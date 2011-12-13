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

    final static int CACHE_SIZE = 10;
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

    void updateCacheWith(final int pos) {
        if (null != cachedBitmap(pos)) return;

        cache.add(new CacheEntry(pos, james.submit(new Callable<Bitmap>() {
            public Bitmap call() throws Exception {
                return CachingRepository.this.delegate.getMutableBitmap(pos);
            }
        })));
        if (cache.size() > CACHE_SIZE) cache.removeFirst();
    }

    public CachingRepository(QuizRepository delegate) {
        this.delegate = delegate;
        updateCacheWith(0);
        updateCacheWith(delegate.nextPos(0));
        updateCacheWith(delegate.nextPos(delegate.nextPos(0)));
        updateCacheWith(delegate.prevPos(0));
        updateCacheWith(delegate.prevPos(delegate.prevPos(0)));
    }

    public Bitmap getMutableBitmap(int pos) throws IOException {
        if (null != cachedBitmap(pos)) {
            try {
                return cachedBitmap(pos).get();
            } catch (Exception e) {
                Log.w("RememberValtech", "Unable to download image", e);
                throw new IOException(e.getMessage());
            }
        }
        return delegate.getMutableBitmap(pos);
    }

    public String getName(int pos) {
        return delegate.getName(pos);
    }

    public boolean isCached(int pos) {
        return null != cachedBitmap(pos) && cachedBitmap(pos).isDone();
    }

    public int prevPos(int pos) {
        final int prevPos = delegate.prevPos(pos);
        updateCacheWith(delegate.prevPos(prevPos));
        updateCacheWith(delegate.prevPos(delegate.prevPos(prevPos)));
        return prevPos;
    }

    public int nextPos(int pos) {
        final int nextPos = delegate.nextPos(pos);
        updateCacheWith(delegate.nextPos(nextPos));
        updateCacheWith(delegate.nextPos(delegate.nextPos(nextPos)));
        return nextPos;
    }
}
