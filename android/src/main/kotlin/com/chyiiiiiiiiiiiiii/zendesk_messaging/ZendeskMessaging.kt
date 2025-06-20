package com.chyiiiiiiiiiiiiii.zendesk_messaging

import android.content.Intent
import io.flutter.plugin.common.MethodChannel
import zendesk.android.Zendesk
import zendesk.android.events.ZendeskEvent
import zendesk.android.events.ZendeskEventListener
import zendesk.messaging.android.DefaultMessagingFactory



class ZendeskMessaging(
    private val plugin: ZendeskMessagingPlugin,
    private val channel: MethodChannel
) {
    companion object {
        const val TAG = "[ZendeskMessaging]"

        // Method channel callback keys
        const val UNREAD_MESSAGES: String = "unread_messages"
    }

    // To create and use the event listener:
    private val zendeskEventListener = ZendeskEventListener { zendeskEvent ->
        when (zendeskEvent) {
            is ZendeskEvent.UnreadMessageCountChanged -> {

                channel.invokeMethod(
                    UNREAD_MESSAGES,
                    mapOf("messages_count" to zendeskEvent.currentUnreadCount)
                )
            }

            else -> {
                // Default branch for forward compatibility with Zendesk SDK and its `ZendeskEvent` expansion
            }
        }
    }

    fun initialize(channelKey: String, result: MethodChannel.Result) {
        println("$TAG - Channel Key - $channelKey")
        if (plugin.isInitialized) {
            println("$TAG - Já inicializado, ignorando nova chamada")
            result.success(null)
            return
        }

        Zendesk.initialize(
            plugin.activity!!,
            channelKey,
            successCallback = { value ->
                plugin.isInitialized = true
                println("$TAG - initialize success - $value")
                result.success(null)
            },
            failureCallback = { error ->
                plugin.isInitialized = false
                println("$TAG - initialize failure - $error")
                result.error("initialize_error", error.message, null)
            },
            messagingFactory = DefaultMessagingFactory()
        )
    }

    fun show() {
        println("$TAG - show() chamado")
        val currentActivity = plugin.activity
        if (currentActivity == null || currentActivity.isFinishing) {
            println("$TAG - activity está nula ou finalizando. Ignorando showMessaging.")
            return
        }
        Zendesk.instance.messaging.showMessaging(currentActivity, Intent.FLAG_ACTIVITY_NEW_TASK)
        println("$TAG - show")
    }

    fun loginUser(jwt: String, result: MethodChannel.Result) {
        Zendesk.instance.loginUser(
            jwt,
            { user ->
                plugin.isLoggedIn = true
                result.success(mapOf("id" to user.id, "externalId" to user.externalId))
            },
            { error ->
                println("$TAG - Login failure : ${error.message}")
                println(error)
                result.error("login_error", error.message, null)
            })
    }


    fun logoutUser(result: MethodChannel.Result) {
        Zendesk.instance.logoutUser(successCallback = {
            plugin.isLoggedIn = false
            result.success(null)
        }, failureCallback = { error ->
            println("$TAG - Logout failure : ${error.message}")
            result.error("logout_error", error.message, null)
        })
        Zendesk.instance.removeEventListener(zendeskEventListener)
    }

    fun listenMessageCountChanged() {
        Zendesk.instance.addEventListener(zendeskEventListener)
    }

    fun setConversationFields(fields: Map<String, String>) {
        Zendesk.instance.messaging.setConversationFields(fields)
    }

    fun clearConversationFields() {
        Zendesk.instance.messaging.clearConversationFields()
    }
}
