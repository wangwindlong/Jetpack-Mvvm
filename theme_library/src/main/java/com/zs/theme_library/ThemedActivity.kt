package com.zs.theme_library

import android.os.Bundle
import com.zs.base_library.base.BaseVmActivity
import com.zs.base_library.utils.PrefUtils

abstract class ThemedActivity: BaseVmActivity() {
    val SP_THEME_KEY = "sp_theme_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        changeTheme()
        super.onCreate(savedInstanceState)
    }

    /**
     * 动态切换主题
     */
    open fun changeTheme() {
        val theme = PrefUtils.getBoolean(SP_THEME_KEY, false)
        if (theme) {
            setTheme(R.style.AppTheme_Night)
        } else {
            setTheme(R.style.AppTheme)
        }
    }
}