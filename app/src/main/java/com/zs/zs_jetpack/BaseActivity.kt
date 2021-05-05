package com.zs.zs_jetpack

import android.os.Bundle
import com.zs.base_library.base.BaseVmActivity
import com.zs.base_library.utils.PrefUtils
import com.zs.zs_jetpack.constants.Constants

abstract class BaseActivity : BaseVmActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        changeTheme()
        super.onCreate(savedInstanceState)
    }

    /**
     * 动态切换主题
     */
    open fun changeTheme() {
        val theme = PrefUtils.getBoolean(Constants.SP_THEME_KEY, false)
        if (theme) {
            setTheme(R.style.AppTheme_Night)
        } else {
            setTheme(R.style.AppTheme)
        }
    }

    override fun init(savedInstanceState: Bundle?) {

    }
}