package eu.darken.octi.common.coil

import android.view.View
import android.widget.ImageView
import androidx.core.view.isInvisible
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import eu.darken.octi.modules.apps.core.AppsInfo

fun ImageRequest.Builder.loadingView(
    imageView: View,
    loadingView: View
) {
    listener(
        onStart = {
            loadingView.isInvisible = false
            imageView.isInvisible = true
        },
        onSuccess = { _, _ ->
            loadingView.isInvisible = true
            imageView.isInvisible = false
        }
    )
}

fun ImageView.loadAppIcon(pkg: AppsInfo.Pkg): Disposable? {
    val current = tag as? AppsInfo.Pkg
    if (current?.packageName == pkg.packageName) return null
    tag = pkg

    val request = ImageRequest.Builder(context).apply {
        data(pkg)
        target(this@loadAppIcon)
    }.build()

    return context.imageLoader.enqueue(request)
}