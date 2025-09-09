package com.example.goqiilogins

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    private var prefs: SharedPreferences? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("coords", MODE_PRIVATE)
        Log.d("MyService", "Accessibility Service connected")
    }

    private fun logNodeTree(node: AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) return
        val prefix = " ".repeat(depth * 2)
        Log.d(
            "NodeTree",
            "$prefix Class: ${node.className}, Text: ${node.text}, ID: ${node.viewIdResourceName}, Clickable: ${node.isClickable}"
        )
        for (i in 0 until node.childCount) {
            logNodeTree(node.getChild(i), depth + 1)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val rootNode = rootInActiveWindow ?: return

        // Capture clicks + save coordinates
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            event.source?.let { node ->
                val rect = Rect()
                node.getBoundsInScreen(rect)
                saveCoordinates(rect)
            }
        }

        // Handle screen changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val className = event.className?.toString()
            if (className?.contains("HomeBasedTabActivity") == true) {
                Log.d("MyService", "WelcomeScreen is active")
                //logNodeTree(rootNode)
            }

            // Step 1: Tap "I already have an account"
            if (findAndClickByText(rootNode, "Sign In, I already have an account")) {
                Log.d("MyAccessibilityService", "Clicked: I already have an account")
                return
            }

            // Step 2: Tap "Sign in with Google"
            if (findAndClickByText(rootNode, "Sign in with Google")) {
                Log.d("MyAccessibilityService", "Clicked: Sign in with Google")
                return
            }
        }
    }

    override fun onInterrupt() {
        Log.d("MyAccessibilityService", "Service interrupted")
    }

    private fun findAndClickByText(node: AccessibilityNodeInfo?, text: String): Boolean {
        node ?: return false
        val nodes = node.findAccessibilityNodeInfosByText(text)
        for (n in nodes) {
            if (n.isClickable) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                n.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        for (i in 0 until node.childCount) {
            if (findAndClickByText(node.getChild(i), text)) return true
        }
        return false
    }

    private fun saveCoordinates(rect: Rect) {
        prefs?.edit()
            ?.putInt("x", rect.centerX())
            ?.putInt("y", rect.centerY())
            ?.apply()

        Log.d("MyService", "Saved center coordinates: (${rect.centerX()}, ${rect.centerY()})")
    }

    fun tapSavedCoordinates() {
        val x = prefs?.getInt("x", -1) ?: -1
        val y = prefs?.getInt("y", -1) ?: -1

        if (x != -1 && y != -1) {
            val gestureBuilder = GestureDescription.Builder()
            val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            dispatchGesture(gestureBuilder.build(), null, null)
            Log.d("MyService", "Tapped at saved coordinates: ($x, $y)")
        } else {
            Log.d("MyService", "No coordinates saved yet!")
        }
    }
}
