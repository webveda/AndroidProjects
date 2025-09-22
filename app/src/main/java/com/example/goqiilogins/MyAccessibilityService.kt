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
        if (event.packageName != "com.betaout.GOQii" && event.packageName != "com.google.android.gms") return

        //Log.d("MyService", "App detected: ${event.packageName}")
        goqiiDetected = true

        // Start or restart checking whenever target app is detected
        startOrRestartChecking()
    }

    private fun startOrRestartChecking() {
        // Only check if we need to automate AND app is detected
        if ((MyAccessibilityServiceController.shouldClickSignIn ||
                    MyAccessibilityServiceController.shouldClickBtnLogin ||
                    MyAccessibilityServiceController.shouldSelectEmail ||
                    MyAccessibilityServiceController.shouldScrollToText) && goqiiDetected) {
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

                // Check if we still need to automate AND app is still detected
                if ((MyAccessibilityServiceController.shouldClickSignIn ||
                            MyAccessibilityServiceController.shouldClickBtnLogin ||
                            MyAccessibilityServiceController.shouldSelectEmail ||
                            MyAccessibilityServiceController.shouldScrollToText) && goqiiDetected) {

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
        //Log.d("MyService", "Flags - SignIn: ${MyAccessibilityServiceController.shouldClickSignIn}, " +
        //        "Google: ${MyAccessibilityServiceController.shouldClickBtnLogin}, " +
        //        "Email: ${MyAccessibilityServiceController.shouldSelectEmail}")

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
                MyAccessibilityServiceController.shouldSelectEmail = true
            } else {
                Log.d("MyService", "Google Login button not found yet")
            }
        }

        // Step 3: Select email account
        if (MyAccessibilityServiceController.shouldSelectEmail && MyAccessibilityServiceController.selectedAccount != null) {
            //Log.d("MyService", "Looking for email account: ${MyAccessibilityServiceController.selectedAccount}")
            if (selectEmailAccount(rootNode, MyAccessibilityServiceController.selectedAccount!!)) {
                //Log.d("MyService", "✓ SUCCESS: Email account selected")
                MyAccessibilityServiceController.shouldSelectEmail = false
                MyAccessibilityServiceController.selectedAccount = null
            } else {
                Log.d("MyService", "Email account not found yet")
            }
        }

        // Step 4: Scroll to specific text
        if (MyAccessibilityServiceController.shouldScrollToText &&
            MyAccessibilityServiceController.textToScrollTo != null) {

            val targetText = MyAccessibilityServiceController.textToScrollTo!!
            //Log.d("MyService", "Looking for text to scroll to: '$targetText'")

            if (scrollToText(rootNode, targetText)) {
                //Log.d("MyService", "✓ SUCCESS: Found and scrolled to text: '$targetText'")
                MyAccessibilityServiceController.shouldScrollToText = false
                MyAccessibilityServiceController.textToScrollTo = null
            } else {
                //Log.d("MyService", "Text '$targetText' not found yet, continuing to scroll...")
                // Keep scrolling until text is found
                performScrollDown(rootNode)
            }
        }

        rootNode.recycle()
    }

    private fun scrollToText(rootNode: AccessibilityNodeInfo, targetText: String): Boolean {
        // First try to find the exact text view
        val targetNodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        if (targetNodes.isNotEmpty()) {
            for (node in targetNodes) {
                if (node.viewIdResourceName == "com.betaout.GOQii:id/tvHashtagOtherMessage" ||
                    node.text?.toString()?.contains(targetText, ignoreCase = true) == true) {
                    //Log.d("MyService", "Found target text: $targetText")
                    return true  // Text is already visible
                }
            }
        }

        // Also check by ID if available
        try {
            val nodesById = rootNode.findAccessibilityNodeInfosByViewId("com.betaout.GOQii:id/tvHashtagOtherMessage")
            for (node in nodesById) {
                val nodeText = node.text?.toString() ?: ""
                if (nodeText.contains(targetText, ignoreCase = true)) {
                    //Log.d("MyService", "Found target text by ID: $targetText")
                    return true  // Text is visible
                }
            }
        } catch (e: Exception) {
            Log.d("MyService", "Error finding by ID: ${e.message}")
        }

        return false
    }

    private fun performScrollDown(rootNode: AccessibilityNodeInfo): Boolean {
        // Find scrollable containers and perform scroll down
        val scrollableNodes = mutableListOf<AccessibilityNodeInfo>()
        findScrollableNodes(rootNode, scrollableNodes)

        for (node in scrollableNodes) {
            if (node.isScrollable) {
                //Log.d("MyService", "Performing scroll down")
                if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                    return true
                }
            }
        }

        // If no scrollable nodes found, try scrolling the root
        //Log.d("MyService", "Scrolling root node")
        return rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private fun findScrollableNodes(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isScrollable) {
            result.add(AccessibilityNodeInfo.obtain(node))
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findScrollableNodes(child, result)
                child.recycle()
            }
        }
    }

    // Select email account from Google account chooser
    private fun selectEmailAccount(rootNode: AccessibilityNodeInfo, targetEmail: String): Boolean {
        // Try by ID first
        try {
            val nodesById = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.gms:id/account_name")
            for (node in nodesById) {
                val emailText = node.text?.toString() ?: ""
                if (emailText.equals(targetEmail, ignoreCase = true) && node.isClickable) {
                    //Log.d("MyService", "Found email account by ID: $emailText")
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        } catch (e: Exception) {
            Log.d("MyService", "Error finding email by ID: ${e.message}")
        }

        // Fallback to text search (case insensitive)
        val nodesByText = rootNode.findAccessibilityNodeInfosByText(targetEmail)
        for (node in nodesByText) {
            if (node.isClickable) {
                //Log.d("MyService", "Found email account by text: $targetEmail")
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            // Also check parent if node itself isn't clickable
            node.parent?.let { parent ->
                if (parent.isClickable) {
                    //Log.d("MyService", "Found clickable parent for email: $targetEmail")
                    return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }

        return false
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