package rjoerod.savemywalk.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import static rjoerod.savemywalk.fragment.ConfirmationFragment.Operation.DELETE;


/**
 * Class controls dialog boxes
 * https://developer.android.com/guide/topics/ui/dialogs.htmls
 */
public class ConfirmationFragment extends DialogFragment {

    public enum Operation {SAVE, START_TRACKING, STOP_TRACKING, DELETE}
    private String title;
    private Operation operation;
    private String routeID;
    public ConfirmationListener mListener; // Instance of the event listener interface

    // Static factory for non-"DELETE" operations
    public static ConfirmationFragment newDialogInstance(String title, Operation operation)
    {
        ConfirmationFragment newDialogFragment = new ConfirmationFragment();
        newDialogFragment.title = title;   // Dynamic title
        newDialogFragment.operation = operation;
        return newDialogFragment;
    }

    // Static factory for "DELETE" operations
    public static ConfirmationFragment newDeleteItemInstance(String title, String routeID) {
        ConfirmationFragment newDialogFragment = new ConfirmationFragment();
        newDialogFragment.title = title;   // Dynamic title
        newDialogFragment.operation = DELETE;
        newDialogFragment.routeID = routeID;
        return newDialogFragment;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(title)
                .setPositiveButton(rjoerod.savemywalk.R.string.ok,
                                                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the positive button event back to the host activity
                        mListener.onDialogPositiveClick(ConfirmationFragment.this);
                    }
                })
                .setNegativeButton(rjoerod.savemywalk.R.string.cancel,
                                                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the the negative button event back to the host activity
                        mListener.onDialogNegativeClick(ConfirmationFragment.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface ConfirmationListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    public Operation getOperation() {
        return this.operation;
    }

    public String getRouteID() {
        return this.routeID;
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity activity = (Activity) context;;

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ConfirmationListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
}
