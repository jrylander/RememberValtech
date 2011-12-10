package cc.rylander.android.remember;

import android.graphics.Bitmap;


public interface QuizRepository {
    int size();
    Bitmap getMutableBitmap();
    String getName();
    void next();
    void prev();
    boolean isCached();
}
