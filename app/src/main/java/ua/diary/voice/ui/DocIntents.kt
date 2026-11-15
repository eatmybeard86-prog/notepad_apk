package ua.diary.voice.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Позначка, що додаток сам відкриває зовнішній екран, тож повертатися в нього можна без відбитка. */
object ExternalLaunch {
    @Volatile private var pending = false
    fun mark() { pending = true }
    fun consume(): Boolean = pending.also { pending = false }
}

private fun uriFor(ctx: Context, file: File) =
    FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)

/** Відкриває документ у сторонній програмі (переглядач PDF, Word, Google Docs…). Повертає false, якщо програми немає. */
fun openDocument(ctx: Context, file: File, mime: String): Boolean {
    if (!file.exists()) return false
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uriFor(ctx, file), mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        ExternalLaunch.mark()
        ctx.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        ExternalLaunch.consume()
        false
    }
}

/** Надіслати документ (месенджер, пошта, збереження у файли). */
fun shareDocument(ctx: Context, file: File, mime: String, title: String) {
    if (!file.exists()) return
    val uri = uriFor(ctx, file)
    val send = Intent(Intent.ACTION_SEND)
        .setType(mime)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(title, uri)
    ExternalLaunch.mark()
    ctx.startActivity(Intent.createChooser(send, "Надіслати"))
}
