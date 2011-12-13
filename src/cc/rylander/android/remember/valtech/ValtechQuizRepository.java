/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import cc.rylander.android.remember.QuizRepository;
import se.valtech.intranet.client.android.APIClient;
import se.valtech.intranet.client.android.APIResponseParser;
import se.valtech.intranet.client.android.Employee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by Johan Rylander (johan@rylander.cc>
 * on 2011-11-20
 */
public class ValtechQuizRepository implements QuizRepository {

    private List<Employee> employees;
    private final APIClient client;
    private int width, height;

    public ValtechQuizRepository(String username, String password, int width, int height) {
        this.width = width;
        this.height = height;
        client = new APIClient(username, password, new APIResponseParser());
        employees = client.getEmployees();
        Collections.shuffle(employees);
    }

    public Bitmap getMutableBitmap(int pos) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean done = false;
        while(! done) {
            try {
                client.download(employees.get(pos).getImageUrl(), out);
                done = true;
            } catch (IOException e) {
                Log.w("RememberValtech", "Unable to download image", e);
            }
        }

        Bitmap src = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());

        // Calculate how to scale
        float xScale = (float) width / src.getWidth();
        float yScale = (float) height / src.getHeight();
        float scale = Math.min(xScale, yScale);

        return Bitmap.createScaledBitmap(src, (int) (src.getWidth() * scale), (int) (src.getHeight() * scale),
                true).copy(Bitmap.Config.RGB_565, true);
    }

    public String getName(int pos) {
        return employees.get(pos).getFirstName() + " " + employees.get(pos).getLastName();
    }

    public int nextPos(int pos) {
        return (pos + 1) % employees.size();
    }

    public int prevPos(int pos) {
        if (pos <= 0 ) return employees.size() - 1;
        return pos - 1;
    }

    public boolean isCached(int pos) {
        return false;
    }
}
