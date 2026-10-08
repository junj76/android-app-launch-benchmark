package com.junj.utils

data class ApplicationInfo(
    val name: String,
    val packageName: String,
    val componentName: String
)

enum class AppSetType {
    LIGHT,
    MIDDLE,
    HEAVY;

    val code: Int
        get() = ordinal
}

val appNameSet = arrayOf(
    // Light-10
    arrayOf(
        "wechat",
        "weibo",
        "zhihu",
        "qqmusic",
        "dianping",
        "xiecheng",
        "jd",
        "meituan",
        "netdisk",
        "kuake"
    ),

    // Middle-12
    arrayOf(
        "wechat",
        "weibo",
        "zhihu",
        "qqmusic",
        "dianping",
        "xiecheng",
        "meituan",
        "netdisk",
        "kuake",
        "wps",
        "doubao",
        "gdmap",
    ),
    // Heavy-13
    arrayOf(
        "wechat",
        "weibo",
        "zhihu",
        "qqmusic",
        "dianping",
        "xiecheng",
        "jd",
        "meituan",
        "netdisk",
        "kuake",
        "wps",
        "doubao",
        "gdmap",
//        "bilibili"
    ),
)

val globalAppInfos = arrayOf(
    ApplicationInfo(
        "douyin",
        "com.ss.android.ugc.aweme",
        "com.ss.android.ugc.aweme/.splash.SplashActivity"
    ),
    ApplicationInfo(
        "bilibili",
        "tv.danmaku.bili",
        "tv.danmaku.bili/.MainActivityV2"
    ),
    ApplicationInfo(
        "gdmap",
        "com.autonavi.minimap",
        "com.autonavi.minimap/com.autonavi.map.activity.SplashActivity"
    ),
    ApplicationInfo(
        "dianping",
        "com.dianping.v1",
        "com.dianping.v1/.NovaMainActivity"
    ),
    ApplicationInfo(
        "jd",
        "com.jingdong.app.mall",
        "com.jingdong.app.mall/.MainFrameActivity"
    ),
    ApplicationInfo(
        "meituan",
        "com.sankuai.meituan",
        "com.sankuai.meituan/com.meituan.android.pt.homepage.activity.MainActivity"
    ),
//    ApplicationInfo(
//        "wzry",
//        "com.tencent.tmgp.sgame/.SGameActivity"
//    ),
    ApplicationInfo(
        "xiecheng",
        "ctrip.android.view",
        "ctrip.android.view/ctrip.business.splash.CtripSplashActivity"
    ),
    ApplicationInfo(
        "wps",
        "cn.wps.moffice_eng",
        "cn.wps.moffice_eng/cn.wps.moffice.documentmanager.ab.proxy.main"
    ),
    ApplicationInfo(
        "doubao",
        "com.larus.nova",
        "com.larus.nova/com.larus.home.impl.alias.AliasActivity1"
    ),
    ApplicationInfo(
        "qqmusic",
        "com.tencent.qqmusic",
        "com.tencent.qqmusic/.activity.AppStarterActivity"
    ),
//    ApplicationInfo(
//        "chrome",
//        "com.android.chrome",
//        "com.android.chrome/com.google.android.apps.chrome.Main"
//    ),
    ApplicationInfo(
        "netdisk",
        "com.baidu.netdisk",
        "com.baidu.netdisk/.ui.DefaultMainActivity"
    ),
    ApplicationInfo(
        "weibo",
        "com.sina.weibo",
        "com.sina.weibo/.SplashActivity"
    ),
    ApplicationInfo(
        "wechat",
        "com.tencent.mm",
        "com.tencent.mm/.ui.LauncherUI"
    ),
    ApplicationInfo(
        "zhihu",
        "com.zhihu.android",
        "com.zhihu.android/.app.ui.activity.MainActivity"
    ),
    ApplicationInfo(
        "kuaishou",
        "com.smile.gifmaker",
        "com.smile.gifmaker/com.yxcorp.gifshow.HomeActivity"
    ),
    ApplicationInfo(
        "kuake",
        "com.quark.browser",
        "com.quark.browser/com.ucpro.MainActivity"
    )
)

data class LaunchApplicationItem(
    val amStartRound: Int,
    val amStartAppName: String,
    val amStartLaunchState: String,
    val amStartTotalTime: Long,
    val amStartWaitTime: Long,
    val amStartStatus: String,
    val dumpsysGfxInfoJankyFrames: Double,
    val dumpsysGfxInfoP50RenderLat: Int,
    val dumpsysGfxInfoP90RenderLat: Int,
    val dumpsysGfxInfoP95RenderLat: Int,
    val dumpsysGfxInfoP99RenderLat: Int,
    val dumpsysMemInfoTotalPss: Int,
    ) {
//    override fun toString(): String {
//        return "round: $round, " +
//            "appName: $appName, " +
//            "launchState: $launchState, " +
//            "totalTime: $totalTime, " +
//            "waitTime: $waitTime, " +
//            "status: $status"
//    }
}

val launchApplicationResult = ArrayList<LaunchApplicationItem>()
