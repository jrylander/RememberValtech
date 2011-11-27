/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;

import java.io.IOException;

public class MainActivity extends Activity implements View.OnTouchListener
{
    private Paint whiteText = new Paint();
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Display display;
    private ImageView image;
    private Bitmap imageBitmap;
    private SharedPreferences preferences;
    private ValtechQuizRepository repository;
    private ViewConfiguration vc;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();

        // Show prefs until we have a login ...
        if (preferences.getString("username", null) == null || preferences.getString("password", null) == null) {
            startActivity(new Intent(this, Preferences.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (preferences.getString("username", null) != null && preferences.getString("password", null) != null) {
                repository = new ValtechQuizRepository(preferences.getString("username", ""),
                        preferences.getString("password", ""));
                showImage();
            }
        } catch (Exception e) {
            startActivity(new Intent(this, Preferences.class));
        }
    }

    private void showImage() throws IOException {
        imageBitmap = repository.getMutableBitmap(display.getWidth(), display.getHeight());
        image.setImageBitmap(imageBitmap);

        textBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.RGB_565);
        textCanvas = new Canvas(textBitmap);
        printText(repository.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, Preferences.class));
                return true;
        }
        return false;
    }

    private void printText(String text) {
        textCanvas.drawColor(Color.BLACK);
        Rect bounds = new Rect();
        whiteText.getTextBounds(text, 0, text.length(), bounds);
        textCanvas.drawText(text, (textCanvas.getWidth() - bounds.width()) / 2,
                (textCanvas.getHeight() - bounds.height()) / 2,
                whiteText);
    }

    GestureDetector maybeMoveToNext = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            // Needed for onFling to work
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final int swipeMinDistance = vc.getScaledTouchSlop();
            final int swipeThresholdVelocity = vc.getScaledMinimumFlingVelocity();
            if (e1 != null && Math.abs(velocityX) > swipeThresholdVelocity &&
                    e1.getX() - e2.getX() > swipeMinDistance) {
                try {
                    loadNextImage();
                    showImage();
                } catch (IOException e) {
                    Log.e("RememberValtech", "While trying to show image", e);
                }
                return true;
            }
            return false;
        }
    });

    public boolean onTouch(View view, MotionEvent event) {
        super.onTouchEvent(event);

        boolean textHit = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                int midX;
                int midY;
                float size;
                midX = (int) event.getX();
                midY = (int) event.getY();
                size = event.getSize();
                int outer = (int) (40 * size);

                for (int x=midX-outer; x<midX+outer; x++) {
                    for (int y=midY-outer; y<midY+outer; y++) {
                        if (x >= 0 && x < textBitmap.getWidth() &&
                                y >= 0 && y < textBitmap.getHeight()) {
                            int pixel = textBitmap.getPixel(x, y);
                            if (Color.BLACK != pixel) {
                                textHit = true;
                                imageBitmap.setPixel(x, y, Color.BLACK);
                            }
                        }
                    }
                }
                if (textHit) {
                    image.invalidate(new Rect(midX-outer, midY-outer, midX+outer, midY+outer));
                }
            }
        }

        return textHit || maybeMoveToNext.onTouchEvent(event);
    }

    private void init() {
        display = getWindowManager().getDefaultDisplay();
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        vc = ViewConfiguration.get(getApplicationContext());

        image = (ImageView) findViewById(R.id.image);
        image.setOnTouchListener(this);

        whiteText.setColor(Color.WHITE);
        whiteText.setTextSize(30);
        whiteText.setAntiAlias(true);
    }

    private void loadNextImage() {
        boolean found = false;
        if (null != repository) {
            while(!found) {
                repository.next();
                try {
                    showImage();
                    found = true;
                } catch (IOException e) {
                    Log.w("RememberValtech", "Unable to download image", e);
                }
            }
        }
    }
}
