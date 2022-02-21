package com.example.prowd_android_template.util_object

import android.content.ContextWrapper

import android.content.Context
import java.util.*

object LocaleHelper {
    // <멤버 변수 공간>
    var locale: Locale? = null


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    fun wrap(context: Context?): Context? {
        if (null == context) {
            return context
        } else {
            return if (null == locale) {
                context
            } else {
                var innerContext = context
                val config = innerContext.resources.configuration

                Locale.setDefault(locale!!)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                innerContext = innerContext.createConfigurationContext(config)
                ContextWrapper(innerContext)
            }
        }
    }
}