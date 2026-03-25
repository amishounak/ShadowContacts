package com.shadowcontacts.app.utils

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import com.shadowcontacts.app.R

object IOSStyleDialog {

    /** Set dialog to 85% screen width minimum */
    private fun enforceMinWidth(dialog: AlertDialog) {
        dialog.window?.let { window ->
            val displayMetrics = window.context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt()
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    /** Public access for callers building their own AlertDialog */
    fun enforceMinWidthPublic(dialog: AlertDialog) = enforceMinWidth(dialog)

    fun showConfirm(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Delete",
        negativeText: String = "Cancel",
        isDanger: Boolean = false,
        onConfirm: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context, R.style.IOSDialogTheme)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveText) { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        builder.setNegativeButton(negativeText) { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
        enforceMinWidth(alertDialog)

        if (isDanger) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                context.getColor(R.color.danger)
            )
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                context.getColor(R.color.accent)
            )
        }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            context.getColor(R.color.accent)
        )
    }

    fun showInput(
        context: Context,
        title: String,
        hint: String = "",
        prefill: String = "",
        positiveText: String = "Save",
        negativeText: String = "Cancel",
        onSubmit: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            this.hint = hint
            setText(prefill)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setSelection(text.length)
        }

        val container = FrameLayout(context).apply {
            val dp16 = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(dp16 * 2, dp16, dp16 * 2, 0)
            addView(editText)
        }

        val builder = AlertDialog.Builder(context, R.style.IOSDialogTheme)
        builder.setTitle(title)
        builder.setView(container)
        builder.setPositiveButton(positiveText) { _, _ ->
            val text = editText.text.toString().trim()
            if (text.isNotEmpty()) onSubmit(text)
        }
        builder.setNegativeButton(negativeText, null)

        val alertDialog = builder.create()
        alertDialog.show()
        enforceMinWidth(alertDialog)

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            context.getColor(R.color.accent)
        )
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            context.getColor(R.color.accent)
        )
    }

    fun showThemePicker(
        context: Context,
        currentTheme: Int,
        onSelected: (Int) -> Unit
    ) {
        val themes = arrayOf("Light", "Dark", "Auto (System)")
        val builder = AlertDialog.Builder(context, R.style.IOSDialogTheme)
        builder.setTitle("Appearance")
        builder.setSingleChoiceItems(themes, currentTheme) { dialog, which ->
            onSelected(which)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)

        val alertDialog = builder.create()
        alertDialog.show()
        enforceMinWidth(alertDialog)

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            context.getColor(R.color.accent)
        )
    }
}
