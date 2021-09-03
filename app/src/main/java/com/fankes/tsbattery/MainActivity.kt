/*
 * Copyright (C) 2021. Fankes Studio(qzmmcn@163.com)
 *
 * This file is part of TSBattery.
 *
 * TSBattery is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TSBattery is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This file is Created by fankes on 2021/9/4.
 */
@file:Suppress(
    "DEPRECATION", "SetTextI18n", "SetWorldReadable", "WorldReadableFiles",
    "LocalVariableName"
)

package com.fankes.tsbattery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.utils.widget.ImageFilterView
import com.fankes.tsbattery.hook.HookMain
import com.fankes.tsbattery.utils.FileUtils
import com.gyf.immersionbar.ImmersionBar
import java.io.File

@Keep
class MainActivity : AppCompatActivity() {

    companion object {

        private const val moduleVersion = BuildConfig.VERSION_NAME
        private const val moduleSupport = "QQ 8.5.5~8.8.23、TIM 2+"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*禁止系统夜间模式对自己造成干扰*/
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        /*隐藏系统的标题栏*/
        supportActionBar?.hide()
        /*初始化沉浸状态栏*/
        ImmersionBar.with(this)
            .statusBarColor("#FFFFFFFF")
            .autoDarkModeEnable(false)
            .statusBarDarkFont(true)
            .navigationBarColor("#FFFFFFFF")
            .navigationBarDarkIcon(true)
            .fitsSystemWindows(true)
            .init()
        /*判断 Hook 状态*/
        if (isHooked()) {
            findViewById<LinearLayout>(R.id.main_lin_status).setBackgroundResource(R.drawable.green_round)
            findViewById<ImageFilterView>(R.id.main_img_status).setImageResource(R.mipmap.succcess)
            findViewById<TextView>(R.id.main_text_status).text = "模块已激活"
        } else
            AlertDialog.Builder(this)
                .setTitle("模块没有激活")
                .setMessage(
                    "检测到模块没有激活，模块需要 Xposed 环境依赖，同时需要系统拥有 Root 权限(太极阴可以免 Root)，请自行查看本页面使用帮助与说明第三条。\n" +
                            "太极、应用转生、梦境(Pine)和第三方 Xposed 激活后可能不会提示激活，若想验证是否激活请打开“提示模块运行信息”自行检查，如果生效就代表模块运行正常，这里的激活状态只是一个显示意义上的存在。"
                )
                .setPositiveButton("我知道了", null)
                .setCancelable(false)
                .show()
        /*设置文本*/
        findViewById<TextView>(R.id.main_text_version).text = "当前版本：$moduleVersion"
        findViewById<TextView>(R.id.main_text_support).text = "支持 $moduleSupport"
        /*初始化 View*/
        val protectModeSwitch = findViewById<SwitchCompat>(R.id.protect_mode_switch)
        val hideIconInLauncherSwitch = findViewById<SwitchCompat>(R.id.hide_icon_in_launcher_switch)
        val notifyModuleInfoSwitch = findViewById<SwitchCompat>(R.id.notify_module_info_switch)
        /*获取 Sp 存储的信息*/
        protectModeSwitch.isChecked = getBoolean("_white_mode")
        hideIconInLauncherSwitch.isChecked = getBoolean("_hide_icon")
        notifyModuleInfoSwitch.isChecked = getBoolean("_tip_run_info")
        protectModeSwitch.setOnCheckedChangeListener { btn, b ->
            if (!btn.isPressed) return@setOnCheckedChangeListener
            putBoolean("_white_mode", b)
        }
        hideIconInLauncherSwitch.setOnCheckedChangeListener { btn, b ->
            if (!btn.isPressed) return@setOnCheckedChangeListener
            putBoolean("_hide_icon", b)
            packageManager.setComponentEnabledSetting(
                ComponentName(this@MainActivity, "com.fankes.tsbattery.Home"),
                if (b) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        notifyModuleInfoSwitch.setOnCheckedChangeListener { btn, b ->
            if (!btn.isPressed) return@setOnCheckedChangeListener
            putBoolean("_tip_run_info", b)
        }
        /*项目地址点击事件*/
        findViewById<View>(R.id.link_with_project_address).setOnClickListener {
            try {
                val intent = Intent()
                intent.action = "android.intent.action.VIEW"
                val content_url = Uri.parse("https://github.com/fankes/TSBattery")
                intent.data = content_url
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法启动系统默认浏览器", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 判断模块是否激活
     * 在 [HookMain] 中 Hook 掉此方法
     * @return 激活状态
     */
    private fun isHooked(): Boolean {
        Log.d("TSBattery", "isHooked: true")
        return isExpModuleActive()
    }

    /**
     * 新增太极判断方式
     * @return 是否激活
     */
    private fun isExpModuleActive(): Boolean {
        var isExp = false
        try {
            val uri = Uri.parse("content://me.weishu.exposed.CP/")
            var result: Bundle? = null
            try {
                result = contentResolver.call(uri, "active", null, null)
            } catch (e: RuntimeException) {
                // TaiChi is killed, try invoke
                try {
                    val intent = Intent("me.weishu.exp.ACTION_ACTIVE")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e1: Throwable) {
                    return false
                }
            }
            if (result == null) result = contentResolver.call(uri, "active", null, null)
            if (result == null) return false
            isExp = result.getBoolean("active", false)
        } catch (ignored: Throwable) {
        }
        return isExp
    }

    override fun onResume() {
        super.onResume()
        setWorldReadable()
    }

    override fun onRestart() {
        super.onRestart()
        setWorldReadable()
    }

    override fun onPause() {
        super.onPause()
        setWorldReadable()
    }

    /**
     * 获取保存的值
     * @param key 名称
     * @return 保存的值
     */
    private fun getBoolean(key: String) =
        getSharedPreferences(
            packageName + "_preferences",
            Context.MODE_PRIVATE
        ).getBoolean(key, false)

    /**
     * 保存值
     * @param key 名称
     * @param bool 值
     */
    private fun putBoolean(key: String, bool: Boolean) {
        getSharedPreferences(
            packageName + "_preferences",
            Context.MODE_PRIVATE
        ).edit().putBoolean(key, bool).apply()
        setWorldReadable()
        Handler().postDelayed({ setWorldReadable() }, 500)
        Handler().postDelayed({ setWorldReadable() }, 1000)
        Handler().postDelayed({ setWorldReadable() }, 1500)
    }

    /**
     * 强制设置 Sp 存储为全局可读可写
     * 以供模块使用
     */
    private fun setWorldReadable() {
        try {
            if (FileUtils.getDefaultPrefFile(this).exists()) {
                for (file in arrayOf<File>(
                    FileUtils.getDataDir(this),
                    FileUtils.getPrefDir(this),
                    FileUtils.getDefaultPrefFile(this)
                )) {
                    file.setReadable(true, false)
                    file.setExecutable(true, false)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法写入模块设置，请检查权限\n如果此提示一直显示，请不要双开模块", Toast.LENGTH_SHORT).show()
        }
    }
}