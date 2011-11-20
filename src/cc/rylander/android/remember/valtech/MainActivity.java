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
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

public class MainActivity extends Activity implements View.OnTouchListener, View.OnClickListener
{
    private Paint whiteText = new Paint();
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Display display;
    private ImageView image;
    private Bitmap imageBitmap;
    private SharedPreferences preferences;
    private ValtechQuizRepository repository;


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
                showNextImage();
            }
        } catch (Exception e) {
            startActivity(new Intent(this, Preferences.class));
        }
    }

    private void showNextImage() throws IOException {
        repository.next();
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

    public boolean onTouch(View view, MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                for (int i=0; i<=event.getHistorySize(); i++) {
                    for (int j=0; j<event.getPointerCount(); j++) {
                        int midX;
                        int midY;
                        float size;
                        if (event.getHistorySize() == i) {
                            midX = (int) event.getX(j);
                            midY = (int) event.getY(j);
                            size = event.getSize(j);
                        } else {
                            midX = (int) event.getHistoricalX(j, i);
                            midY = (int) event.getHistoricalY(j, i);
                            size = event.getHistoricalSize(j, i);
                        }
                        int outer = (int) (75 * size);

                        for (int x=midX-outer; x<midX+outer; x += 3) {
                            for (int y=midY-outer; y<midY+outer; y += 3) {
                                if (x >= 0 && x < textBitmap.getWidth() &&
                                    y >= 0 && y < textBitmap.getHeight()) {
                                    int pixel = textBitmap.getPixel(x, y);
                                    if (Color.BLACK != pixel) {
                                        imageBitmap.setPixel(x, y, Color.BLACK);
                                    }
                                }
                            }
                        }
                        image.invalidate(new Rect(midX-outer, midY-outer, midX+outer, midY+outer));
                    }
                }
            }
        }
        return true;
    }

    private void init() {
        display = getWindowManager().getDefaultDisplay();
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        image = (ImageView) findViewById(R.id.image);
        image.setOnTouchListener(this);

        Button button = (Button) findViewById(R.id.next);
        button.setOnClickListener(this);

        whiteText.setColor(Color.WHITE);
        whiteText.setTextSize(30);
        whiteText.setAntiAlias(true);
    }

    public void onClick(View view) {
        boolean found = false;
        if (null != repository) {
            while(!found) {
                repository.next();
                try {
                    showNextImage();
                    found = true;
                } catch (IOException e) {
                    Log.w("RememberValtech", "Unable to download image", e);
                }
            }
        }
    }
}
