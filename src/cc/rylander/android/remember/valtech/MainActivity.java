/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.ImageView;
import cc.rylander.android.remember.QuizRepository;


public class MainActivity extends Activity implements View.OnTouchListener
{
    private Paint whiteText = new Paint();
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Display display;
    private ImageView image;
    private Bitmap imageBitmap;
    private SharedPreferences preferences;
    private QuizRepository repository;
    private ViewConfiguration vc;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();

        if (preferences.getString("username", null) == null || preferences.getString("password", null) == null) {
            startActivity(new Intent(this, Preferences.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences.getString("username", null) != null && preferences.getString("password", null) != null) {
            if (null == repository) {
                final ProgressDialog progressDialog = ProgressDialog.show(this, "Ansluter...", null);
                new AsyncTask<Integer, Float, QuizRepository>() {
                    @Override
                    protected QuizRepository doInBackground(Integer... widthHeight) {
                        try {
                            return new CachingRepository(new ValtechQuizRepository(preferences.getString("username", ""),
                                    preferences.getString("password", ""), widthHeight[0], widthHeight[1]));
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(QuizRepository repo) {
                        progressDialog.dismiss();
                        if (null == repo) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("Anslutningen till intranätet misslyckades. " +
                                            "Kontrollera inställningarna för inloggning.").
                                    setPositiveButton("Ok", null).
                                    show();
                        } else {
                            MainActivity.this.repository = repo;
                            showImage();
                        }
                    }
                }.execute(display.getHeight(), display.getWidth());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void showImage() {
        if (null == repository) {
            return;
        }

        if (repository.isCached()) {
            useBitmap(repository.getMutableBitmap());

        } else {
            final ProgressDialog progressDialog = ProgressDialog.show(this, "Laddar bild...", null);
            new AsyncTask<Object, Float, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Object... notUsed) {
                    return repository.getMutableBitmap();
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    try {
                        useBitmap(bitmap);
                    } finally {
                        progressDialog.dismiss();                    
                    }
                }
            }.execute();
        }
    }

    private void useBitmap(Bitmap bitmap) {
        if (null != bitmap) {
            imageBitmap = bitmap;
            image.setImageBitmap(imageBitmap);

            textBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.RGB_565);
            textCanvas = new Canvas(textBitmap);
            printText(repository.getName());
        }
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
                    Math.abs(e1.getX() - e2.getX()) > swipeMinDistance &&
                    (e1.getY() < 0.4 * imageBitmap.getHeight() || e1.getY() > 0.6 * imageBitmap.getHeight()) &&
                    (e2.getY() < 0.4 * imageBitmap.getHeight() || e2.getY() > 0.6 * imageBitmap.getHeight())) {
                if (e1.getX() > e2.getX()) {
                    repository.next();
                } else {
                    repository.prev();
                }
                showImage();
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
                                imageBitmap.setPixel(x, y, Color.CYAN);
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
}
