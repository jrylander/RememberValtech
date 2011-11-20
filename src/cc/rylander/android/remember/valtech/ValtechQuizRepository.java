/*
 * Copyright (c) 2011 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import cc.rylander.android.remember.QuizRepository;
import se.valtech.intranet.client.android.APIClient;
import se.valtech.intranet.client.android.APIResponseParser;
import se.valtech.intranet.client.android.Employee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Johan Rylander (johan@rylander.cc>
 * on 2011-11-20
 */
public class ValtechQuizRepository implements QuizRepository {

    private List<Employee> employees;
    private Iterator<Employee> iter;
    private Employee current;
    private final APIClient client;

    public ValtechQuizRepository(String username, String password) {
        client = new APIClient(username, password, new APIResponseParser());
        employees = client.getEmployees();
    }

    public int size() {
        return employees == null ? 0 : employees.size();
    }

    public Bitmap getMutableBitmap(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        client.download(current.getImageUrl(), out);
        Bitmap src = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());

        // Calculate how to scale
        float xScale = (float) width / src.getWidth();
        float yScale = (float) height / src.getHeight();
        float scale = Math.min(xScale, yScale);

        return Bitmap.createScaledBitmap(src, (int) (src.getWidth() * scale), (int) (src.getHeight() * scale),
                true).copy(Bitmap.Config.RGB_565, true);
    }

    public String getName() {
        return current.getFirstName() + " " + current.getLastName();
    }

    public void next() {
        if (null == iter || !iter.hasNext()) {
            iter = employees.iterator();
        }
        current = iter.next();
    }
}
