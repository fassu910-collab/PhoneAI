package com.phoneai.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PhoneControlAccessibilityService : AccessibilityService() {

    companion object {
        var instance: PhoneControlAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        CommandLog.add(" AI Service       ")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun executeCommand(rawCommand: String) {
        val command = rawCommand.trim()
        CommandLog.add("  : \"$command\"")

        when {
            command.startsWith(" ", ignoreCase = true) || command.startsWith("open ", ignoreCase = true) -> {
                val appName = command.substringAfter(" ").trim()
                openApp(appName)
            }
            command.startsWith(" ", ignoreCase = true) || command.startsWith("type ", ignoreCase = true) -> {
                val text = command.substringAfter(" ").trim()
                typeInFocusedField(text)
            }
            command.startsWith("  ", ignoreCase = true) || command.startsWith("click ", ignoreCase = true) -> {
                val label = command.substringAfter(" ").trim()
                clickElementWithText(label)
            }
            command.equals("", ignoreCase = true) || command.equals("home", ignoreCase = true) -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
                CommandLog.add("    ")
            }
            command.equals("", ignoreCase = true) || command.equals("back", ignoreCase = true) -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
                CommandLog.add("  ")
            }
            command.equals("", ignoreCase = true) || command.equals("notifications", ignoreCase = true) -> {
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                CommandLog.add("  ")
            }
            else -> {
                CommandLog.add("        : ' WhatsApp', ' Hello', '  Send'")
            }
        }
    }

    private fun openApp(appName: String) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        }

        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                CommandLog.add(" : ${pm.getApplicationLabel(match)}")
                return
            }
        }
        CommandLog.add(" \"$appName\"         ")
    }

    private fun typeInFocusedField(text: String) {
        val focused = findFocusedEditableNode(rootInActiveWindow)
        if (focused == null) {
            CommandLog.add("            ,  ''  ")
            return
        }
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
        )
        val success = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        CommandLog.add(if (success) " : \"$text\"" else "    ")
    }

    private fun findFocusedEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) {
            val result = findFocusedEditableNode(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun clickElementWithText(label: String) {
        val root = rootInActiveWindow
        if (root == null) {
            CommandLog.add("    ")
            return
        }
        val nodes = root.findAccessibilityNodeInfosByText(label)
        if (nodes.isNullOrEmpty()) {
            CommandLog.add(" \"$label\"      ")
            return
        }
        var target: AccessibilityNodeInfo? = nodes[0]
        while (target != null && !target.isClickable) {
            target = target.parent
        }
        if (target != null) {
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            CommandLog.add("  : \"$label\"")
        } else {
            CommandLog.add(" \"$label\"     ")
        }
    }
}
