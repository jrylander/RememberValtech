package cc.rylander.android.remember;

import android.graphics.Bitmap;

import java.util.concurrent.ExecutionException;


public interface QuizRepository {
    Bitmap getMutableBitmap(int pos) throws ExecutionException, InterruptedException;
    String getName(int pos);
    boolean isCached(int pos);
    int nextPos(int pos);
    int prevPos(int pos);
}
