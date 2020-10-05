package de.dralle.wetherapp

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class MessageDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity != null) {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle(R.string.need_gps_title)
            builder.setMessage(R.string.need_gps_reason)
            builder.setNeutralButton(
                R.string.gps_dialog_ok,
                DialogInterface.OnClickListener { dialog, id -> Log.d(tag, "OK") })
            // Create the AlertDialog object and return it
            return builder.create()
        } else {
            throw Exception("Need reference to activity")
        }
    }
}