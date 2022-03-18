package com.example.prowd_android_template.native_wrapper

import android.content.res.AssetManager

object NativeWrapperAssetManagerTest {
    init {
        System.loadLibrary("native_wrapper_asset_manager_test")
    }

    // [래핑 함수]
    external fun getAssetTextString(
        assetManager: AssetManager,
        dat_file_name: String): String
}