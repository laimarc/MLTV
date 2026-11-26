package com.marco.mltv

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView

/** HorizontalScrollView qui notifie ses changements de position (sync frise ↔ grille). */
class SyncHScroll @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : HorizontalScrollView(ctx, attrs) {

    var onScrollX: ((Int) -> Unit)? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollX?.invoke(l)
    }
}
