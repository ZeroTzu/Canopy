package com.teamname.canopy.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.teamname.canopy.R
import com.google.android.material.button.MaterialButton

class CustomCircleButtonLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.custom_circle_button_layout, this)
    }

    fun setIcon(iconRes: Int) {
        findViewById<MaterialButton>(R.id.customButton_materialButton).setIconResource(iconRes)
    }

    fun setText(text: String) {
        findViewById<TextView>(R.id.customButton_textView).text = text
    }

}