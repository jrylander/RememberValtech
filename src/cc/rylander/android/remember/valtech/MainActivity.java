/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import cc.rylander.android.remember.QuizRepository;
import cc.rylander.android.remember.QuizRepositoryCallback;
import com.flurry.android.FlurryAgent;


public class MainActivity extends Activity implements View.OnTouchListener
{
    Paint white = new Paint();
    Paint cyan = new Paint();
    Paint imagePaint = new Paint();
    Bitmap textBitmap;
    Canvas textCanvas;
    ImageView image;
    Canvas imageCanvas;
    Bitmap imageSrc;
    QuizRepository repository;
    ValtechQuizRepository valtechRepo;
    ViewConfiguration vc;
    int pos;
    int direction = 1;
    static final int DIALOG_REPO_FAILED = 0;
    boolean repoIsBeingCreated;
    Settings prefs;
    boolean shouldCrop;
    int width;
    int height;
    boolean okToShowImage;
    float touchDownX;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        vc = ViewConfiguration.get(getApplicationContext());
        prefs = new Settings(this);
        shouldCrop = prefs.shouldCrop();

        image = (ImageView) findViewById(R.id.image);

        // Kick off image showing when we have the size of the image view
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (0 != image.getHeight() && 0 != image.getWidth()) {
                    width = image.getWidth();
                    height = image.getHeight();
                    textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    textCanvas = new Canvas(textBitmap);
                    final Bitmap imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    imageCanvas = new Canvas(imageBitmap);
                    image.setImageBitmap(imageBitmap);
                    okToShowImage = true;
                    fetchRepository();
                    image.setOnTouchListener(MainActivity.this);
                    image.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        white.setColor(Color.WHITE);
        white.setTextSize(55);
        white.setAntiAlias(true);

        cyan.setColor(Color.CYAN);

        imagePaint.setFilterBitmap(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (okToShowImage) {
            if (null == repository) {
                fetchRepository();
            } else if (prefs.shouldCrop() != shouldCrop) {
                showCurrentImage();
                shouldCrop = prefs.shouldCrop();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, "GK1LFL775BWZG4L4APFY");
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
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

    void fetchRepository() {
        if (null == repository && !repoIsBeingCreated) {
            repoIsBeingCreated = true;
            try {
                new ValtechQuizRepository(this,
                        new QuizRepositoryCallback<ValtechQuizRepository>() {
                            public void calledWhenDone(ValtechQuizRepository repository) {
                                repoIsBeingCreated = false;
                                if (null == repository) {
                                    showDialog(DIALOG_REPO_FAILED);
                                } else {
                                    MainActivity.this.valtechRepo = repository;
                                    MainActivity.this.repository = new CachingRepository(repository);
                                    showCurrentImage();
                                }
                            }
                        });
            } catch (Exception e) {
                repoIsBeingCreated = false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    synchronized void showCurrentImage() {
        if (null == repository) {
            return;
        }

        if (repository.isCached(pos)) {
            imageSrc = retryingGetBitMap();
            useBitmap(imageSrc);

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
                        if (null == bitmap) {
                            showDialog(DIALOG_REPO_FAILED);
                        } else {
                            imageSrc = bitmap;
                            useBitmap(imageSrc);
                        }
                    } finally {
                        progressDialog.dismiss();                    
                    }
                }
            }.execute();
        }
    }

    Bitmap retryingGetBitMap() {
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

    void useBitmap(Bitmap bitmap) {
        useBitmap(bitmap, 0);
    }
    void useBitmap(Bitmap bitmap, int offset) {
        if (null != bitmap) {
            imageCanvas.drawColor(Color.BLACK);

            imageCanvas.drawBitmap(bitmap, scaleAndCenter(bitmap, offset), imagePaint);
            image.invalidate();

            printText(repository.getName(pos));
        }
    }

    Matrix scaleAndCenter(Bitmap bitmap, int offset) {
        final Matrix matrix = new Matrix();
        float xScale = (float) width / bitmap.getWidth();
        float yScale = (float) height / bitmap.getHeight();
        float scale = shouldCrop ? Math.max(xScale, yScale) : Math.min(xScale, yScale);
        matrix.preScale(scale, scale);

        float dx = (width - bitmap.getWidth() * scale) / 2;
        float dy = (height - bitmap.getHeight() * scale) / 2;
        matrix.postTranslate(dx + offset, dy);

        return matrix;
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
            case R.id.settings:
                startActivity(new Intent(this, Preferences.class));
                return true;
        }
        return false;
    }

    void printText(String text) {
        textCanvas.drawColor(Color.BLACK);
        Rect bounds = new Rect();
        white.getTextBounds(text, 0, text.length(), bounds);
        textCanvas.drawText(text, (textCanvas.getWidth() - bounds.width()) / 2,
                (textCanvas.getHeight() - bounds.height()) / 2,
                white);
    }

    GestureDetector flingDetected = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
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
                isOutsideScratchArea(e1) && isOutsideScratchArea(e2)) {
                if (e1.getX() > e2.getX()) {
                    pos = repository.nextPos(pos);
                    direction = 1;
                } else {
                    pos = repository.prevPos(pos);
                    direction = -1;
                }
                showCurrentImage();
                return true;
            }
            return false;
        }
    });

    boolean isOutsideScratchArea(MotionEvent e) {
        final double topThreshold = 0.4 * height;
        final double bottomThreshold = 0.6 * height;
        return e.getY() < topThreshold || e.getY() > bottomThreshold;
    }

    public boolean onTouch(View view, MotionEvent event) {
        super.onTouchEvent(event);

        if (isOutsideScratchArea(event)) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchDownX = event.getX();
            }
            if (!flingDetected.onTouchEvent(event)) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    useBitmap(imageSrc, (int) (event.getX() - touchDownX));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    useBitmap(imageSrc);
                }
            }

        } else {
            boolean textHit = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    int midX = (int) event.getX();
                    int midY = (int) event.getY();
                    float size = event.getSize();
                    int outer = (int) (40 * size);

                    for (int x=midX-outer; x<midX+outer; x++) {
                        for (int y=midY-outer; y<midY+outer; y++) {
                            if (x >= 0 && x < width && y >= 0 && y < height) {
                                int pixel = textBitmap.getPixel(x, y);
                                if (Color.BLACK != pixel) {
                                    textHit = true;
                                    imageCanvas.drawPoint(x, y, cyan);
                                }
                            }
                        }
                    }
                    if (textHit) {
                        image.invalidate(new Rect(midX-outer, midY-outer, midX+outer, midY+outer));
                    }
            }

        }

        // Need to return true for e.g. down events or not move events will be received
        return true;
    }

}
