/*
 * Copyright (c) 2012 Johan Rylander (johan@rylander.cc). All rights reserved.
 */

package cc.rylander.android.remember.valtech;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import cc.rylander.android.remember.QuizRepository;
import cc.rylander.android.remember.QuizRepositoryCallback;
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
    private APIClient client;
    private QuizRepositoryCallback<ValtechQuizRepository> callback;
    private Activity activity;
    private AlertDialog loginDialog;
    private EditText passwordField;
    private EditText loginField;
    private CheckBox shouldStorePassword;
    private final ValtechQuizRepositorySettings prefs;


    public ValtechQuizRepository(Activity activity, QuizRepositoryCallback<ValtechQuizRepository> callback) {
        this.callback = callback;
        this.activity = activity;

        prefs = new ValtechQuizRepositorySettings(activity);
        String username = prefs.getUsername();
        String password = prefs.getPassword();
        client = new APIClient(username, password, new APIResponseParser());

        if ("".equals(username) || "".equals(password)) {
            showLoginDialog();
        } else {
            login(username, password);
        }
    }

    public void logout() {
        client = null;
        prefs.edit();
        prefs.removeUsername();
        prefs.removePassword();
        prefs.commit();
    }

    @SuppressWarnings("unchecked")
    void login(String username, String password) {
        client = new APIClient(username, password, new APIResponseParser());
        if (isEmpty(username) || isEmpty(password)) {
            showLoginDialog();
        } else {
            final ProgressDialog loginProgress = ProgressDialog.show(activity, activity.getText(R.string.loggingIn), null);
            new AsyncTask<Object, Object, Boolean>() {
                @Override
                protected Boolean doInBackground(Object... objects) {
                    return client.authenticate();
                }

                @Override
                protected void onPostExecute(Boolean authenticated) {
                    loginProgress.dismiss();
                    final ProgressDialog connectProgress = ProgressDialog.show(activity, activity.getText(R.string.connecting), null);
                    new AsyncTask<Object, Object, List<Employee>>() {
                        @Override
                        protected List<Employee> doInBackground(Object... notUsed) {
                            try {
                                List<Employee> employees = client.getEmployees();
                                Collections.shuffle(employees);
                                return employees;
                            } catch (Exception e) {
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(List<Employee> _employees) {
                            connectProgress.dismiss();
                            if (null != _employees) {
                                employees = _employees;
                                callback.calledWhenDone(ValtechQuizRepository.this);
                            } else {
                                showLoginDialog();
                            }
                        }
                    }.execute();
                }
            }.execute();
        }
    }

    private boolean isEmpty(String str) {
        return str == null || "".equals(str.trim());
    }

    DialogInterface.OnCancelListener onLoginCancelled = new DialogInterface.OnCancelListener() {
        public void onCancel(DialogInterface dialogInterface) {
            dismissDialogs();
            callback.calledWhenDone(null);
        }
    };

    DialogInterface.OnClickListener onLoginCancel = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
        }
    };

    DialogInterface.OnClickListener onLoginAttempt = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialogInterface, int i) {
            dismissDialogs();

            prefs.edit();
            prefs.setUsername(loginField.getText().toString());
            if (shouldStorePassword.isChecked()) {
                prefs.setPassword(passwordField.getText().toString());
            } else {
                prefs.removePassword();
            }
            prefs.commit();

            login(loginField.getText().toString(), passwordField.getText().toString());
        }
    };

    private void showLoginDialog() {
        View dialogLayout = activity.getLayoutInflater().inflate(R.layout.login,
                (ViewGroup) activity.findViewById(R.id.login_layout_root));
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(dialogLayout)
                .setOnCancelListener(onLoginCancelled)
                .setPositiveButton(R.string.login, onLoginAttempt)
                .setNegativeButton(R.string.cancel, onLoginCancel)
                .setTitle(R.string.valtech_login_title);
        loginDialog = builder.show();
        loginField = (EditText) loginDialog.findViewById(R.id.login);
        passwordField = (EditText) loginDialog.findViewById(R.id.password);
        shouldStorePassword = (CheckBox) loginDialog.findViewById(R.id.savePassword);

        String username = prefs.getUsername();
        loginField.setText(username);
        String password = prefs.getPassword();
        if (!isEmpty(password)) {
            shouldStorePassword.setChecked(true);
            passwordField.setText(password);
        }
    }

    private void dismissDialogs() {
        if (null != loginDialog) {
            loginDialog.dismiss();
            loginDialog = null;
        }
    }

    public Bitmap getBitmap(int pos) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        client.download(employees.get(pos).getImageUrl(), out);

        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inInputShareable = true;
        opts.inPurgeable = true;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size(), opts);
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
