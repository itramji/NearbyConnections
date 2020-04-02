package com.bornbytes.walkietalkie

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar


inline fun <reified T> Context.startActivity() {
    startActivity(Intent(this, T::class.java))
}

fun Activity.showSnackBar(msg: String) {
    Snackbar.make(this.findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()

}

fun Context.toast(msg: String?) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}