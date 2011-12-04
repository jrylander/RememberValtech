/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.graphics.Bitmap;
import cc.rylander.android.remember.QuizRepository;

import java.io.IOException;

/**
 * Created by Johan Rylander (johan@rylander.cc>
 * on 2011-12-03
 */
public class CachingRepository implements QuizRepository {

    QuizRepository delegate;

    public CachingRepository(QuizRepository delegate) {
        this.delegate = delegate;
    }

    public int size() {
        return delegate.size();
    }

    public Bitmap getMutableBitmap(int width, int height) throws IOException {
        return delegate.getMutableBitmap(width, height);
    }

    public String getName() {
        return delegate.getName();
    }

    public void next() {
        delegate.next();
    }

    public void prev() {
        delegate.prev();
    }
}
