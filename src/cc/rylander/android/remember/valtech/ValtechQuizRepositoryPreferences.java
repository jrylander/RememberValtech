package cc.rylander.android.remember.valtech;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Johan Rylander (johan@rylander.cc)
 * on jrylander
 */
public class ValtechQuizRepositoryPreferences {

    private final SharedPreferences prefs;
    private SharedPreferences.Editor edit;

    public ValtechQuizRepositoryPreferences(Activity activity) {
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public String getUsername() {
        return prefs.getString("username", "");
    }
    public void setUsername(String value) {
        edit.putString("username", value);
    }
    public void removeUsername() {
        edit.remove("username");
    }

    public String getPassword() {
        return prefs.getString("password", "");
    }
    public void setPassword(String value) {
        edit.putString("password", value);
    }
    public void removePassword() {
        edit.remove("password");
    }

    public void edit() {
        edit = prefs.edit();
    }
    public void commit() {
        edit.commit();
    }
}
