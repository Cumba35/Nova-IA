
package com.nova.ai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
        }

        val title = TextView(this).apply {
            text = "Nova AI"
            textSize = 30f
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "¿Ya tienes descargado el modelo de IA?"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 28)
        }

        val chooseButton = Button(this).apply {
            text = "Sí, elegir modelo GGUF"
            setOnClickListener { chooseModel() }
        }

        val downloadButton = Button(this).apply {
            text = "No, descargar modelo"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Descargar modelo")
                    .setMessage(
                        "La descarga se habilitará cuando " +
                        "integremos el motor de IA."
                    )
                    .setPositiveButton("Aceptar", null)
                    .show()
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(
            chooseButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        layout.addView(
            downloadButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(layout)
    }

    private fun chooseModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivityForResult(intent, REQUEST_MODEL)
    }

    @Deprecated("Usamos este método para compatibilidad con Android 9")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MODEL && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data

            if (uri != null) {
                val name = uri.lastPathSegment ?: "archivo seleccionado"

                status.text =
                    "Archivo seleccionado:\n$name\n\n" +
                    "Falta comprobar el modelo y cargar el motor."
            }
        }
    }

    companion object {
        private const val REQUEST_MODEL = 1001
    }
}
