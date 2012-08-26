package cc.rylander.android.remember.valtech;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogRepositoryFailed extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle("Ooops")
                .setMessage(cc.rylander.android.remember.valtech.R.string.repository_failed).
                        setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getActivity().finish();
                            }
                        })
                .create();
    }
}
