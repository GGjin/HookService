package com.gg.hookservice

import android.content.Context
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.lang.reflect.Method

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val binderHooker = SystemServiceBinderHooker(Context.WIFI_SERVICE, "android.net.wifi.IWifiManager", object : SystemServiceBinderHooker
        .HookCallback {
            override fun onServiceMethodInvoke(method: Method?, args: Array<out Any>?) {
                Log.w("TAG", "method 111--->$method")
            }

            override fun onServiceMethodIntercept(receiver: Any?, method: Method?, args: Array<out Any>?): Any {
                TODO("Not yet implemented")
            }

        })
        binderHooker.hook()

        // 监控系统的 wifi 扫描，gps 定位
        val mWifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // 希望在任何地方都能监控这个系统方法调用：同事写的代码，第三方的代码
        // hook：asm 字节码，动态代理，native hook
        // asm 字节码：改自己写好的代码和第三方引入，改不了系统代码
        // 动态代理：1.单例 2.接口
        mWifiManager.startScan()
    }
}