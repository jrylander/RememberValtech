/*
 * Copyright (c) 2012 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by Johan Rylander (johan@rylander.cc)
 * on 2012-02-02
 */
public class Preferences extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
