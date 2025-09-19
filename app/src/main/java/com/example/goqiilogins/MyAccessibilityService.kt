package com.example.goqiilogins

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private val maxRetries = 5

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only process when window content changes or state changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        // Step 1: Look for SignIn button
        if (MyAccessibilityServiceController.shouldClickSignIn) {
            if (findAndClickButton(rootNode, "com.betaout.GOQii:id/signIn", "Sign In")) {
                Log.d("MyService", "Clicked SignIn button")
                MyAccessibilityServiceController.shouldClickSignIn = false
                retryCount = 0

                // Delay for UI to load next screen
                handler.postDelayed({
                    MyAccessibilityServiceController.shouldClickBtnLogin = true
                }, 3000) // Increased to 3 seconds
            } else {
                retryCount++
                if (retryCount >= maxRetries) {
                    Log.d("MyService", "Max retries reached for SignIn")
                    MyAccessibilityServiceController.shouldClickSignIn = false
                    retryCount = 0
                }
            }
        }

        // Step 2: Look for Google Login button
        if (MyAccessibilityServiceController.shouldClickBtnLogin) {
            if (findAndClickButton(rootNode, "com.betaout.GOQii:id/btnLogin", "Sign in with Google")) {
                Log.d("MyService", "Clicked Google Login button")
                MyAccessibilityServiceController.shouldClickBtnLogin = false
                retryCount = 0
            } else {
                retryCount++
                if (retryCount >= maxRetries) {
                    Log.d("MyService", "Max retries reached for Google Login")
                    MyAccessibilityServiceController.shouldClickBtnLogin = false
                    retryCount = 0
                }
            }
        }

        rootNode.recycle()
    }

    override fun onInterrupt() {
        Log.d("MyService", "Service interrupted")
    }

    private fun findAndClickButton(rootNode: AccessibilityNodeInfo, viewId: String, text: String): Boolean {
        // Try by ID first (more reliable)
        val nodesById = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        for (node in nodesById) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }

        // Fallback to text search
        val nodesByText = rootNode.findAccessibilityNodeInfosByText(text)
        for (node in nodesByText) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            // Also check parent if node itself isn't clickable
            node.parent?.let { parent ->
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
        }

        return false
    }
}