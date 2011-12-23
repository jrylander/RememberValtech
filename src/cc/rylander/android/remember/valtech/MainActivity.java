/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import cc.rylander.android.remember.QuizRepository;
import cc.rylander.android.remember.QuizRepositoryCallback;


public class MainActivity extends Activity implements View.OnTouchListener
{
    private Paint whiteText = new Paint();
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Display display;
    private ImageView image;
    private Bitmap imageBitmap;
    private QuizRepository repository;
    private ValtechQuizRepository valtechRepo;
    private ViewConfiguration vc;
    private int pos;
    private int direction = 1;
    static final int DIALOG_REPO_FAILED = 0;
    private boolean repoIsBeingCreated;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (DIALOG_REPO_FAILED == id) {
            return new AlertDialog.Builder(MainActivity.this).
                    setMessage(R.string.repository_failed).
                    setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }).
                    show();
        }
        return  null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRepository();
    }

    private void fetchRepository() {
        if (null == repository && !repoIsBeingCreated) {
            repoIsBeingCreated = true;
            try {
                new ValtechQuizRepository(this,
                        display.getHeight(), display.getWidth(), new QuizRepositoryCallback<ValtechQuizRepository>() {
                    public void calledWhenDone(ValtechQuizRepository repository) {
                        repoIsBeingCreated = false;
                        if (null == repository) {
                            showDialog(DIALOG_REPO_FAILED);
                        } else {
                            MainActivity.this.valtechRepo = repository;
                            MainActivity.this.repository = new CachingRepository(repository);
                            showImage();
                        }
                    }
                });
            } catch (Exception e) {
                repoIsBeingCreated = false;
            }
        }
    }


    @SuppressWarnings("unchecked")
    private synchronized void showImage() {
        if (null == repository) {
            return;
        }

        if (repository.isCached(pos)) {
            useBitmap(retryingGetBitMap());

        } else {
            final ProgressDialog progressDialog = ProgressDialog.show(this, getText(R.string.loading_image), null);
            new AsyncTask<Object, Float, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Object... notUsed) {
                    return retryingGetBitMap();
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

    private Bitmap retryingGetBitMap() {
        Bitmap bitmap = null;
        int attempts = 0;
        while(null == bitmap && attempts++ < 10) {
            try {
                bitmap = repository.getBitmap(pos);
            } catch (Exception e) {
                if (direction > 0) {
                    pos = repository.nextPos(pos);
                } else {
                    pos = repository.prevPos(pos);
                }
            }
        }
        return bitmap;
    }

    private void useBitmap(Bitmap bitmap) {
        if (null != bitmap) {
            imageBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);

            image.setImageBitmap(imageBitmap);

            textBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.RGB_565);
            textCanvas = new Canvas(textBitmap);
            whiteText.setTextSize(((float) imageBitmap.getHeight() / display.getHeight()) * 40);
            printText(repository.getName(pos));
        } else {
            showDialog(DIALOG_REPO_FAILED);
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
            case R.id.logout:
                if (null != repository) {
                    valtechRepo.logout();
                    valtechRepo = null;
                    repository = null;
                    onResume();
                }
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
                    pos = repository.nextPos(pos);
                    direction = 1;
                } else {
                    pos = repository.prevPos(pos);
                    direction = -1;
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
        vc = ViewConfiguration.get(getApplicationContext());

        image = (ImageView) findViewById(R.id.image);
        image.setOnTouchListener(this);

        whiteText.setColor(Color.WHITE);
        whiteText.setAntiAlias(true);
    }
}
