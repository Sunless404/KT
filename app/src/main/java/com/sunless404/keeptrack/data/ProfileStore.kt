package com.sunless404.keeptrack.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File

/** Perfil del usuario: nombre visible, biografía y foto. */
data class Profile(
    val displayName: String = "",
    val bio: String = "",
    /** Foto en JPEG (256 px); null = sin foto. */
    val photo: ByteArray? = null,
    val memberSince: Long = 0,
) {
    override fun equals(other: Any?): Boolean = other is Profile && displayName == other.displayName &&
        bio == other.bio && memberSince == other.memberSince && photo.contentEquals(other.photo)

    override fun hashCode(): Int = displayName.hashCode() * 31 + bio.hashCode()
}

/** Guarda el perfil en el móvil (ajustes + archivo de la foto). */
class ProfileStore(private val context: Context, private val prefs: Prefs) {
    private val photoFile = File(context.filesDir, "avatar.jpg")

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    /** Se avisa al cambiar algo, para subirlo al perfil público. */
    var onChanged: (Profile) -> Unit = {}

    private fun load(): Profile {
        val since = prefs.sp.getLong("memberSince", 0L).takeIf { it > 0 } ?: System.currentTimeMillis().also {
            prefs.sp.edit().putLong("memberSince", it).apply()
        }
        return Profile(
            displayName = prefs.sp.getString("displayName", "").orEmpty(),
            bio = prefs.sp.getString("bio", "").orEmpty(),
            photo = photoFile.takeIf { it.exists() }?.readBytes(),
            memberSince = since,
        )
    }

    fun update(displayName: String, bio: String, notify: Boolean = true) {
        val name = displayName.trim().take(MAX_NAME)
        val b = bio.trim().take(MAX_BIO)
        prefs.sp.edit().putString("displayName", name).putString("bio", b).apply()
        _profile.value = _profile.value.copy(displayName = name, bio = b)
        if (notify) onChanged(_profile.value)
    }

    fun setPhoto(bytes: ByteArray?, notify: Boolean = true) {
        if (bytes == null) photoFile.delete() else photoFile.writeBytes(bytes)
        _profile.value = _profile.value.copy(photo = bytes)
        if (notify) onChanged(_profile.value)
    }

    /** Lee una imagen de la galería, la recorta en cuadrado y la reduce a 256 px. */
    fun photoFromUri(uri: Uri): ByteArray {
        val src: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ ->
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) } ?: error("formato no soportado")
        }
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, PHOTO_PX, PHOTO_PX, true)
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            out.toByteArray()
        }
    }

    companion object {
        const val MAX_NAME = 40
        const val MAX_BIO = 160
        const val PHOTO_PX = 256
    }
}
