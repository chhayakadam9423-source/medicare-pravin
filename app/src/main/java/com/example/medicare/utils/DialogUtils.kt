package com.example.medicare.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.example.medicare.R
import com.google.android.material.snackbar.Snackbar

object DialogUtils {

    fun showSuccessDialog(
        context: Context,
        title: String,
        message: String,
        onDismiss: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_success, null)
        dialog.setContentView(view)

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val btnOk = view.findViewById<Button>(R.id.btnDialogOk)

        tvTitle.text = title
        tvMessage.text = message

        btnOk.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    fun showSnackbar(view: android.view.View, message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        if (isError) {
            snackbar.setBackgroundTint(Color.parseColor("#D32F2F"))
            snackbar.setTextColor(Color.WHITE)
        } else {
            snackbar.setBackgroundTint(Color.parseColor("#2E7D32"))
            snackbar.setTextColor(Color.WHITE)
        }
        snackbar.show()
    }
}
