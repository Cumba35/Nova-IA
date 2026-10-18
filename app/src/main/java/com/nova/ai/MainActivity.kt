
package com.nova.ai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var chooseButton: Button
    private lateinit var downloadButton: Button

    private var selectedModelUri: Uri? = null
    private var selectedModelName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()
    }

    private fun createInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
        }

        val title = TextView(this).apply {
            text = "Nova AI"
            textSize = 32f
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Inteligencia artificial local"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        }

        status = TextView(this).apply {
            text = "Selecciona un modelo GGUF para comenzar."
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 24)
        }

        chooseButton = Button(this).apply {
            text = "SÍ, ELEGIR MODELO GGUF"
            setOnClickListener {
                chooseModel()
            }
        }

        downloadButton = Button(this).apply {
            text = "NO, DESCARGAR MODELO"
            setOnClickListener {
                showDownloadMessage()
            }
        }

        val info = TextView(this).apply {
            text = "Nova AI comprobará el archivo seleccionado. " +
                "Para generar respuestas también necesita " +
                "tener integrado el motor de IA."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }

        root.addView(
            title,
            matchWidth()
        )

        root.addView(
            subtitle,
            matchWidth()
        )

        root.addView(
            status,
            matchWidth()
        )

        root.addView(
            chooseButton,
            matchWidth()
        )

        root.addView(
            downloadButton,
            matchWidth()
        )

        root.addView(
            info,
            matchWidth()
        )

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun matchWidth(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(8)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun chooseModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

        try {
            startActivityForResult(intent, REQUEST_MODEL)
        } catch (e: Exception) {
            showError(
                "No se pudo abrir el selector de archivos.",
                e
            )
        }
    }

    @Deprecated("Compatibilidad con Android 9 y versiones posteriores")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode != REQUEST_MODEL) {
            return
        }

        if (resultCode != RESULT_OK) {
            status.text = "No se seleccionó ningún archivo."
            return
        }

        val uri = data?.data

        if (uri == null) {
            status.text =
                "No se recibió la ubicación del archivo."
            return
        }

        validateModel(uri, data.flags)
    }

    private fun validateModel(
        uri: Uri,
        resultFlags: Int
    ) {
        chooseButton.isEnabled = false
        status.text = "Comprobando el archivo seleccionado..."

        try {
            val readPermission =
                resultFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (readPermission != 0) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        readPermission
                    )
                } catch (_: Exception) {
                    // Algunos proveedores no permiten permisos persistentes.
                }
            }

            val fileName = getFileName(uri)

            if (!fileName.lowercase(Locale.ROOT).endsWith(".gguf")) {
                selectedModelUri = null
                selectedModelName = null

                status.text =
                    "El archivo seleccionado no es GGUF.\n\n" +
                    "Archivo: $fileName\n\n" +
                    "Selecciona un archivo terminado en .gguf."

                return
            }

            val headerIsValid = contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    val header = ByteArray(4)
                    var offset = 0

                    while (offset < header.size) {
                        val count = input.read(
                            header,
                            offset,
                            header.size - offset
                        )

                        if (count <= 0) {
                            break
                        }

                        offset += count
                    }

                    offset == 4 &&
                        header[0] == 'G'.code.toByte() &&
                        header[1] == 'G'.code.toByte() &&
                        header[2] == 'U'.code.toByte() &&
                        header[3] == 'F'.code.toByte()
                } ?: false

            if (!headerIsValid) {
                selectedModelUri = null
                selectedModelName = null

                status.text =
                    "No se pudo validar la cabecera GGUF.\n\n" +
                    "Archivo: $fileName\n\n" +
                    "Comprueba que el archivo esté completo " +
                    "y que no sea un archivo dañado."

                return
            }

            selectedModelUri = uri
            selectedModelName = fileName

            status.text =
                "Modelo GGUF reconocido:\n\n" +
                "$fileName\n\n" +
                "La cabecera es válida y el archivo se puede leer.\n\n" +
                "Estado: el motor de IA todavía debe iniciarse."

            showModelValidatedDialog(fileName)

        } catch (e: Exception) {
            selectedModelUri = null
            selectedModelName = null

            status.text =
                "Error al comprobar el modelo.\n\n" +
                (e.localizedMessage ?: "Error de lectura.")

        } finally {
            chooseButton.isEnabled = true
        }
    }

    private fun getFileName(uri: Uri): String {
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (columnIndex >= 0) {
                    val name = cursor.getString(columnIndex)

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        } catch (_: Exception) {
            // Se intentará utilizar un nombre alternativo.
        } finally {
            cursor?.close()
        }

        return uri.lastPathSegment ?: "modelo.gguf"
    }

    private fun showModelValidatedDialog(fileName: String) {
        AlertDialog.Builder(this)
            .setTitle("Modelo reconocido")
            .setMessage(
                "Archivo: $fileName\n\n" +
                "La comprobación básica ha terminado correctamente.\n\n" +
                "Todavía falta conectar e iniciar el motor llama.cpp " +
                "para que Nova AI pueda responder preguntas."
            )
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun showDownloadMessage() {
        AlertDialog.Builder(this)
            .setTitle("Descarga de modelos")
            .setMessage(
                "La descarga automática aún no está implementada.\n\n" +
                "Por ahora, descarga un modelo GGUF compatible " +
                "y selecciónalo desde el teléfono."
            )
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun showError(
        message: String,
        error: Exception
    ) {
        val details = error.localizedMessage
            ?: error.javaClass.simpleName

        AlertDialog.Builder(this)
            .setTitle("Nova AI")
            .setMessage("$message\n\n$details")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    companion object {
        private const val REQUEST_MODEL = 1001
    }
}
