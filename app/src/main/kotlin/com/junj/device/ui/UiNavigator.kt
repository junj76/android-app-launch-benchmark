package com.junj.device.ui

import com.junj.device.adb.runAdbRootShellCommand
import com.junj.domain.app.ApplicationInfo

private const val DEFAULT_MAX_BACK_PRESSES = 10
private const val PAGE_CHANGE_WAIT_MS = 1_000L

private val uiNodeRegex = Regex("""<node\b[^>]*>""")
private val englishHomeRegex = Regex("""\bhome\b""", RegexOption.IGNORE_CASE)

private val appSpecificHomeLabels = mapOf(
    "wechat" to listOf("微信"),
    "qqmusic" to listOf("音乐馆"),
)

fun simulateUserClickEvents(app: ApplicationInfo) {
    when(app.name) {
        "douyin" -> {

        }
        "bilibili" -> {

        }
        "gdmap" -> {

        }
        "dianping" -> {

        }
        "jd" -> {

        }
        "meituan" -> {

        }
        "xiecheng" -> {

        }
        "wps" -> {

        }
        "doubao" -> {

        }
        "qqmusic" -> {

        }
        "chrome" -> {

        }
        "netdisk" -> {

        }
        "weibo" -> {

        }
        "wechat" -> {

        }
        else -> {

        }
    }
}

/**
 * Returns [app] to its home page by inspecting the current UI XML and pressing
 * Back once per iteration.
 *
 * Android does not expose a universal "this is the app home page" flag. This
 * function first looks for a selected Home tab. If an app does not expose such
 * a node, it keeps going back until the app's root page exits, then launches the
 * app's main component again. [maxBackPresses] prevents an infinite loop when a
 * page intercepts the Back key.
 *
 * @return `true` when the home page is detected or the main component is
 * successfully relaunched; otherwise `false`.
 */
fun returnAppToHome(
    app: ApplicationInfo,
    maxBackPresses: Int = DEFAULT_MAX_BACK_PRESSES,
): Boolean {
    require(maxBackPresses > 0) { "maxBackPresses must be greater than 0" }

    var hierarchy = dumpCurrentUiHierarchy()

    repeat(maxBackPresses) {
        if (isAppHomeHierarchy(hierarchy, app)) return true

        val wasInTargetApp = hierarchyContainsPackage(hierarchy, app.packageName)
        val backResult = runAdbRootShellCommand("input keyevent KEYCODE_BACK")
        if (backResult.exitCode != 0) return false

        Thread.sleep(PAGE_CHANGE_WAIT_MS)
        val nextHierarchy = dumpCurrentUiHierarchy()
        val isInTargetApp = hierarchyContainsPackage(nextHierarchy, app.packageName)

        // We backed out of the app's root page. Relaunching its declared main
        // component leaves the test on the app home page instead of the launcher.
        if (wasInTargetApp && !isInTargetApp) {
            return relaunchAppHome(app)
        }

        hierarchy = nextHierarchy
    }

    return isAppHomeHierarchy(hierarchy, app)
}

private fun dumpCurrentUiHierarchy(): String =
    runAdbRootShellCommand("uiautomator dump /dev/tty 2>/dev/null").output

private fun isAppHomeHierarchy(hierarchy: String, app: ApplicationInfo): Boolean {
    if (!hierarchyContainsPackage(hierarchy, app.packageName)) return false

    val homeLabels = listOf("首页", "主页", "home") +
        appSpecificHomeLabels[app.name].orEmpty()

    return uiNodeRegex.findAll(hierarchy).any { match ->
        val node = match.value
        if (xmlAttribute(node, "package") != app.packageName) return@any false

        val text = xmlAttribute(node, "text")
        val contentDescription = xmlAttribute(node, "content-desc")
        val resourceId = xmlAttribute(node, "resource-id")
        val semanticText = "$text $contentDescription $resourceId"

        val hasHomeLabel = homeLabels.any { label ->
            if (label == "home") englishHomeRegex.containsMatchIn(semanticText)
            else semanticText.contains(label, ignoreCase = true)
        }
        val isSelected = xmlAttribute(node, "selected") == "true" ||
            xmlAttribute(node, "checked") == "true" ||
            contentDescription.contains("selected", ignoreCase = true) ||
            contentDescription.contains("已选择") ||
            contentDescription.contains("已选中")

        hasHomeLabel && isSelected
    }
}

private fun hierarchyContainsPackage(hierarchy: String, packageName: String): Boolean =
    uiNodeRegex.findAll(hierarchy).any { match ->
        xmlAttribute(match.value, "package") == packageName
    }

private fun relaunchAppHome(app: ApplicationInfo): Boolean {
    val result = runAdbRootShellCommand("am start -W -n ${app.componentName}")
    if (result.exitCode != 0) return false

    Thread.sleep(PAGE_CHANGE_WAIT_MS)
    return hierarchyContainsPackage(dumpCurrentUiHierarchy(), app.packageName)
}

private fun xmlAttribute(node: String, name: String): String =
    Regex("""\b${Regex.escape(name)}="([^"]*)"""")
        .find(node)
        ?.groupValues
        ?.get(1)
        .orEmpty()
