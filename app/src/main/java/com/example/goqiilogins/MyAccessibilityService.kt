package com.example.goqiilogins

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isChecking = false
    private var goqiiDetected = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName != "com.betaout.GOQii") return

        //Log.d("MyService", "GOQii app detected")
        goqiiDetected = true

        // Start or restart checking whenever GoQii is detected
        startOrRestartChecking()
    }

    private fun startOrRestartChecking() {
        // Only check if we need to automate AND GoQii is detected
        if ((MyAccessibilityServiceController.shouldClickSignIn || MyAccessibilityServiceController.shouldClickBtnLogin) && goqiiDetected) {
            if (!isChecking) {
                isChecking = true
                startChecking()
            }
        } else {
            // Stop checking if not needed
            isChecking = false
        }
    }

    private fun startChecking() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isChecking) return

                // Check if we still need to automate AND GoQii is still detected
                if ((MyAccessibilityServiceController.shouldClickSignIn || MyAccessibilityServiceController.shouldClickBtnLogin) && goqiiDetected) {

                    checkAndClickButtons()

                    // Continue checking every 500ms
                    handler.postDelayed(this, 500)
                } else {
                    // Stop checking when done
                    isChecking = false
                    //Log.d("MyService", "Stopping checks")
                }
            }
        })
    }

    private fun checkAndClickButtons() {
        val rootNode = rootInActiveWindow ?: return

        //Log.d("MyService", "=== CHECKING BUTTONS ===")
        //Log.d("MyService", "Flags - SignIn: ${MyAccessibilityServiceController.shouldClickSignIn}, Google: ${MyAccessibilityServiceController.shouldClickBtnLogin}")

        // Step 1: Click SignIn button
        if (MyAccessibilityServiceController.shouldClickSignIn) {
            //Log.d("MyService", "Looking for SignIn button...")
            if (clickButton(rootNode, "Sign In") || clickButtonById(rootNode, "com.betaout.GOQii:id/signIn")) {
                //Log.d("MyService", "✓ SUCCESS: SignIn button clicked")
                MyAccessibilityServiceController.shouldClickSignIn = false
                MyAccessibilityServiceController.shouldClickBtnLogin = true
            } else {
                Log.d("MyService", "SignIn button not found yet")
            }
        }

        // Step 2: Click Google Login button
        if (MyAccessibilityServiceController.shouldClickBtnLogin) {
            //Log.d("MyService", "Looking for Google Login button...")
            if (clickButton(rootNode, "Sign in with Google") || clickButtonById(rootNode, "com.betaout.GOQii:id/btnLogin")) {
                //Log.d("MyService", "✓ SUCCESS: Google Login clicked")
                MyAccessibilityServiceController.shouldClickBtnLogin = false
            } else {
                Log.d("MyService", "Google Login button not found yet")
            }
        }

        rootNode.recycle()
    }

    // Add this function to manually trigger checking when flags change
    fun triggerCheck() {
        handler.post {
            startOrRestartChecking()
        }
    }

    private fun clickButton(rootNode: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable) {
                //Log.d("MyService", "Found clickable button by text: '$text'")
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    private fun clickButtonById(rootNode: AccessibilityNodeInfo, id: String): Boolean {
        return try {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (node.isClickable) {
                    //Log.d("MyService", "Found clickable button by ID: $id")
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    override fun onInterrupt() {
        isChecking = false
        goqiiDetected = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        isChecking = false
        goqiiDetected = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}