package cc.rylander.android.remember;

import android.graphics.Bitmap;

import java.io.IOException;
import java.net.MalformedURLException;

public interface QuizRepository {
    int size();
    Bitmap getMutableBitmap(int width, int height) throws IOException;
    String getName();
    void next();
}
