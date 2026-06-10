package com.culoo.cusagl_4android

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class UiText(
    @param:StringRes val resId: Int,
    val args: List<Any> = emptyList()
) {
    companion object {
        fun resource(@StringRes resId: Int, vararg args: Any): UiText {
            return UiText(resId, args.toList())
        }
    }
}

@Composable
fun UiText.asString(): String {
    val resolvedArgs = args.map { arg ->
        if (arg is UiText) arg.asString() else arg
    }.toTypedArray()
    return stringResource(resId, *resolvedArgs)
}

fun Context.resolve(uiText: UiText): String {
    val resolvedArgs = uiText.args.map { arg ->
        if (arg is UiText) resolve(arg) else arg
    }.toTypedArray()
    return getString(uiText.resId, *resolvedArgs)
}
