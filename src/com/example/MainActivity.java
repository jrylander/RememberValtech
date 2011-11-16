package com.example;

import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends Activity implements View.OnTouchListener
{
    private Paint whiteText = new Paint();
    private Bitmap textBitmap;
    private Canvas textCanvas;
    private Display display;
    private ImageView image;
    private Bitmap imageBitmap;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();

        showImage();
        printText("Johan Rylander");
    }

    private void showImage() {
        imageBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.johan_rylander).copy(Bitmap.Config.RGB_565, true);
        image.setImageBitmap(imageBitmap);
    }

    private void printText(String text) {
        textCanvas.drawColor(Color.BLACK);
        Rect bounds = new Rect();
        whiteText.getTextBounds(text, 0, text.length(), bounds);
        textCanvas.drawText(text, (display.getWidth() - bounds.width()) / 2,
                (display.getHeight() - bounds.height()) / 2,
                whiteText);
    }

    public boolean onTouch(View view, MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                int x = (int) event.getX();
                int y = (int) event.getY();
                for (int i=x-10; i<x+10; i++) {
                    for (int j=y-10; j<y+10; j++) {
                        if (i >= 0 && i < textBitmap.getWidth() &&
                            j >= 0 && j < textBitmap.getHeight()) {
                            int pixel = textBitmap.getPixel(i, j);
                            if (Color.BLACK != pixel) {
                                imageBitmap.setPixel(i, j, Color.BLACK);
                            }
                        }
                    }
                }
                image.invalidate(new Rect(x-10, y-10, x+10, y+10));
            }
        }
        return true;
    }

    private void init() {
        display = getWindowManager().getDefaultDisplay();

        image = (ImageView) findViewById(R.id.image);
        image.setOnTouchListener(this);

        textBitmap = Bitmap.createBitmap(display.getWidth(), display.getHeight(), Bitmap.Config.RGB_565);
        textCanvas = new Canvas(textBitmap);

        whiteText.setColor(Color.WHITE);
        whiteText.setTextSize(24);
        whiteText.setAntiAlias(true);
    }
}
