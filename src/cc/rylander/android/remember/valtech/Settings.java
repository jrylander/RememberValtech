/*
 * Copyright (c) 2012 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Johan Rylander (johan@rylander.cc)
 * on 2012-02-02
 */
public class Settings {
    private final SharedPreferences prefs;

    public Settings(Activity activity) {
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public boolean shouldCrop() {
        return ! prefs.getString("preferenceCrop", "noCrop").equals("noCrop");
    }
}
