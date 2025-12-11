package com.example.goqiilogins

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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
        //Log.d("MyService", "EVENT RECEIVED: ${event.eventType}, Package: ${event.packageName}")
        if (event.packageName != "com.betaout.GOQii" && event.packageName != "com.google.android.gms") return

        //Log.d("MyService", "App detected: ${event.packageName}")
        goqiiDetected = true

        // Start or restart checking whenever target app is detected
        startOrRestartChecking()
    }

    private fun startOrRestartChecking() {
        // Only check if we need to automate AND app is detected
        // Update the startOrRestartChecking() condition
        if ((MyAccessibilityServiceController.shouldClickSignIn ||
                    MyAccessibilityServiceController.shouldClickBtnLogin ||
                    MyAccessibilityServiceController.shouldSelectEmail ||
                    MyAccessibilityServiceController.shouldScrollToText ||
                    MyAccessibilityServiceController.shouldNavigateToArena ||
                    MyAccessibilityServiceController.shouldScrollAndLike) && goqiiDetected) {
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
                // Update the startOrRestartChecking() condition
                if ((MyAccessibilityServiceController.shouldClickSignIn ||
                            MyAccessibilityServiceController.shouldClickBtnLogin ||
                            MyAccessibilityServiceController.shouldSelectEmail ||
                            MyAccessibilityServiceController.shouldScrollToText ||
                            MyAccessibilityServiceController.shouldNavigateToArena ||
                            MyAccessibilityServiceController.shouldScrollAndLike) && goqiiDetected) {

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

        //Log.d("MyService", "=== checkAndClickButtons() CALLED - Step: ${MyAccessibilityServiceController.postLoginStep} ===")
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

                Thread.sleep(2000)
                MyAccessibilityServiceController.shouldNavigateToArena = true
                MyAccessibilityServiceController.postLoginStep = 1

                //rootNode.recycle()
                //return
            } else {
                Log.d("MyService", "Email account not found yet")
            }
        }

        //Log.d("Arena Flag value: ", MyAccessibilityServiceController.shouldNavigateToArena.toString())
        // Post-login navigation flow (Extra screens check logic)
        if (MyAccessibilityServiceController.shouldNavigateToArena) {
            when (MyAccessibilityServiceController.postLoginStep) {
                1 -> { // Check Home
                    if (isHomeScreenPresent(rootNode)) {
                        MyAccessibilityServiceController.postLoginStep = 2
                        Log.d("Home Check: ", "Going to step 2")
                    } else {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        handler.postDelayed({}, 2000) // Wait 2s
                    }
                }

                2 -> { // Click Arena
                    if (clickArenaButton(rootNode)) {
                        Log.d("Arena Check: ", "Processing step " + MyAccessibilityServiceController.postLoginStep)
                        MyAccessibilityServiceController.postLoginStep = 3
                        MyAccessibilityServiceController.arenaClickTime = System.currentTimeMillis()
                        handler.postDelayed({}, 4000) // Wait
                        Log.d("Arena Check 2: ", "Processing step " + MyAccessibilityServiceController.postLoginStep)
                    }
                }

                3 -> { // Verify Arena
                    Log.d("Arena Check 3: ", "Processing step " + MyAccessibilityServiceController.postLoginStep)
                    if (System.currentTimeMillis() - MyAccessibilityServiceController.arenaClickTime >= 4000) {
                        if (isArenaScreenPresent(rootNode)) {
                            MyAccessibilityServiceController.shouldNavigateToArena = false
                            MyAccessibilityServiceController.postLoginStep = 0
                            Log.d("Arena Check: ", "Completed")
                        } else {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            MyAccessibilityServiceController.postLoginStep = 2
                            Log.d("Arena Check 4: ", "BACK Action in progress")
                        }
                    }
                }
            }
            rootNode.recycle()
            return
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

        // Add the scroll and like logic to checkAndClickButtons() function:
        // Step 5: Scroll and like by date range
        if (MyAccessibilityServiceController.shouldScrollAndLike &&
            MyAccessibilityServiceController.startDateText != null &&
            MyAccessibilityServiceController.endDateText != null) {

            val startDate = MyAccessibilityServiceController.startDateText!!
            val endDate = MyAccessibilityServiceController.endDateText!!

            if (!MyAccessibilityServiceController.foundEndDate) {
                handleScrollAndLikeProcess(rootNode, startDate, endDate)
            } else {
                Log.d("MyService", "✓ SUCCESS: Completed scroll and like from '$startDate' to '$endDate'")

                // AFTER like process completes go back to GOQiilogins app
                //Log.d("MyService", "LIKE process completed - bringing app back")
                handler.postDelayed({
                    val intent = packageManager.getLaunchIntentForPackage("com.example.goqiilogins")
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                }, 2000)

                MyAccessibilityServiceController.shouldScrollAndLike = false
                MyAccessibilityServiceController.isProcessingRange = false
            }
        }

        rootNode.recycle()
    }

    private fun handleScrollAndLikeProcess(rootNode: AccessibilityNodeInfo, startDate: String, endDate: String) {
        // First, check if we've found the end date (stop condition)
        if (checkForText(rootNode, endDate) && MyAccessibilityServiceController.isProcessingRange) {
            Log.d("MyService", "✓ Found end date: '$endDate' - stopping process")
            MyAccessibilityServiceController.foundEndDate = true
            return
        }

        // Check if we're in the processing range
        if (MyAccessibilityServiceController.isProcessingRange) {
            // We're in the range - click like buttons and scroll
            clickLikeButtonsInRange(rootNode)
            performScrollDown(rootNode)
        } else {
            // We haven't found the start date yet - keep scrolling to find it
            if (checkForText(rootNode, startDate) && !MyAccessibilityServiceController.foundStartDate) {
                Log.d("MyService", "✓ Found start date: '$startDate' - starting like process")
                MyAccessibilityServiceController.foundStartDate = true
                MyAccessibilityServiceController.isProcessingRange = true
                clickLikeButtonsInRange(rootNode)
            } else {
                // Scroll to find the start date
                performScrollDown(rootNode)
            }
        }
    }

    private fun checkForText(rootNode: AccessibilityNodeInfo, targetText: String): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        return nodes.isNotEmpty()
    }

    // Add this to get context for saving
    private fun saveLikedPosts() {
        MyAccessibilityServiceController.saveState(this)
    }

    private fun clickLikeButtonsInRange(rootNode: AccessibilityNodeInfo) {
        try {
            val llLikeButtons = rootNode.findAccessibilityNodeInfosByViewId("com.betaout.GOQii:id/llLike")

            for (likeButton in llLikeButtons) {
                if (likeButton.isClickable) {
                    val postKey = getPostUniqueKey(likeButton, rootNode)

                    if (postKey != null && !MyAccessibilityServiceController.clickedPostKeys.contains(postKey)) {
                        //Log.d("MyService", "Clicking like - Element ID: $postKey")

                        if (likeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            MyAccessibilityServiceController.clickedPostKeys.add(postKey)
                            // SAVE IMMEDIATELY AFTER EACH SUCCESSFUL LIKE
                            saveLikedPosts()
                            //Log.d("MyService", "✓ Saved liked post: $postKey")
                        }

                        Thread.sleep(500)
                    } else if (postKey != null) {
                        Log.d("MyService", "Already liked: $postKey")
                    }
                }
            }

            llLikeButtons.forEach { it.recycle() }

        } catch (e: Exception) {
            Log.d("MyService", "Error: ${e.message}")
        }
    }

    private fun getPostUniqueKey(likeButton: AccessibilityNodeInfo, rootNode: AccessibilityNodeInfo): String? {
        try {
            // Find the post container by traversing up the hierarchy
            var postContainer: AccessibilityNodeInfo? = likeButton.parent
            var depth = 0

            // Traverse up to find the post container (limit depth to avoid infinite loops)
            while (postContainer != null && depth < 10) {
                // Check if this container has the post content we need
                val description = findChildTextById(postContainer, "com.betaout.GOQii:id/tv_description")
                val hashtag = findChildTextById(postContainer, "com.betaout.GOQii:id/tvHashtagOtherMessage")
                // Get the selected email account ID from Controller
                val emailId = MyAccessibilityServiceController.selectedAccount ?: "no_email"
                //Log.d("MyService", "getPostUniqueKey - Using email: $emailId")

                if (description != null || hashtag != null) {
                    // Found post content - create unique key
                    val postKey = "$emailId|${description ?: "no_desc"}|${hashtag ?: "no_hashtag"}"
                    //Log.d("MyService", "Post key: $postKey")
                    return postKey
                }

                // Move up to next parent
                postContainer = postContainer.parent
                depth++
            }

            // If hierarchy traversal failed, try searching in the entire root node
            Log.d("MyService", "Hierarchy traversal failed, searching in root node")
            val description = findChildTextById(rootNode, "com.betaout.GOQii:id/tv_description")
            val hashtag = findChildTextById(rootNode, "com.betaout.GOQii:id/tvHashtagOtherMessage")
            val emailId = MyAccessibilityServiceController.selectedAccount ?: "no_email"

            return if (description != null || hashtag != null) {
                "$emailId|${description ?: "no_desc"}|${hashtag ?: "no_hashtag"}"
            } else {
                null
            }

        } catch (e: Exception) {
            Log.d("MyService", "Error getting post key: ${e.message}")
            return null
        }
    }

    private fun findChildTextById(parent: AccessibilityNodeInfo, viewId: String): String? {
        return try {
            val nodes = parent.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes.isNotEmpty()) {
                val text = nodes[0].text?.toString()
                nodes.forEach { it.recycle() }
                text
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d("MyService", "Error finding child by ID $viewId: ${e.message}")
            null
        }
    }

    private fun performScrollDown(rootNode: AccessibilityNodeInfo): Boolean {
        val scrollableNodes = mutableListOf<AccessibilityNodeInfo>()
        findScrollableNodes(rootNode, scrollableNodes)

        for (node in scrollableNodes) {
            if (node.isScrollable) {
                Log.d("MyService", "Performing scroll down")
                if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                    return true
                }
            }
        }

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

    private fun isHomeScreenPresent(rootNode: AccessibilityNodeInfo): Boolean {
        return try {
            val homeNodes = rootNode.findAccessibilityNodeInfosByViewId("com.betaout.GOQii:id/ll_home")
            val found = homeNodes.isNotEmpty()
            Log.d("MyService", "Home check: found ${homeNodes.size} ll_home nodes = $found")
            found
        } catch (e: Exception) {
            false
        }
    }

    private fun clickArenaButton(rootNode: AccessibilityNodeInfo): Boolean {
        return try {
            val arenaNodes = rootNode.findAccessibilityNodeInfosByViewId("com.betaout.GOQii:id/ll_social")
            for (node in arenaNodes) {
                if (node.isClickable) {
                    Log.d("MyService", "Clicking Arena button")
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun isArenaScreenPresent(rootNode: AccessibilityNodeInfo): Boolean {
        return try {
            val arenaNodes = rootNode.findAccessibilityNodeInfosByViewId("com.betaout.GOQii:id/ll_social")
            // If we can still see Arena button, might mean we're still on home screen
            // OR check for Arena-specific elements
            //val arenaIndicator = rootNode.findAccessibilityNodeInfosByViewId("com.betaout.GOQii:id/some_arena_specific_id")
            //arenaIndicator.isNotEmpty()
            arenaNodes.isNotEmpty().also {
                if (it) Log.d("MyService", "Found ${arenaNodes.size} ll_social elements")
            }
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