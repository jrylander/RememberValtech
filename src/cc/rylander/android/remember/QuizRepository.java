package cc.rylander.android.remember;

import android.graphics.Bitmap;

import java.io.IOException;


public interface QuizRepository {
    Bitmap getBitmap(int pos) throws IOException;
    String getName(int pos);
    boolean isCached(int pos);
    int nextPos(int pos);
    int prevPos(int pos);
}
