package com.example.livelocationapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


open class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Log.e("TAG", "onNewToken: $s")
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", s).apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.e("TAG", "onMessageReceived:---- ${message.notification}")

        if (message.data.isNotEmpty()) {

            val notificationData = message.notification
            val pushData = message.data
            if (notificationData != null) {
                openNotification(
                    this,
                    pushData = pushData,
                    notificationData = notificationData
                )
            }
        }

    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun openNotification(
        context: Context,
        pushData: Map<String, String>,
        notificationData: RemoteMessage.Notification
    ) {
        val channelId = context.getString(R.string.default_notification_channel_id)
        val channelName = "Default"

//        val deepLinkIntent = createDeepLinkIntent(context, pushData)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationData.title)
            .setContentText(notificationData.body)
//            .setContentIntent(deepLinkIntent)
            .setSmallIcon(getSmallIconId())
            .setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManagerCompat.notify(createNotificationID(), builder.build())
    }

    private fun getSmallIconId(): Int {
        return R.drawable.ic_launcher_background
    }

    open fun createNotificationID(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

    var iUniqueId = (System.currentTimeMillis()).toInt()


    /*@SuppressLint("UnspecifiedImmutableFlag")
    private fun createDeepLinkIntent(
        context: Context,
        pushData: Map<String, String>
    ): PendingIntent? {

        val normalNotificationData = NotificationResponse()

        normalNotificationData.updatedAt = pushData["updatedAt"]?.toLong()
        normalNotificationData.createdAt = pushData["createdAt"]?.toLong()
        normalNotificationData.dueDate = pushData["due_date"]
        normalNotificationData.description = pushData["description"]
        normalNotificationData.notificationType = pushData["notification_type"]?.toInt()
        normalNotificationData.priority = pushData["priority"]?.toInt()
        normalNotificationData.status = pushData["status"]?.toInt()
        normalNotificationData.redFlag = pushData["red_flag"]?.toBoolean()
        normalNotificationData.userId = pushData["user_id"]?.toInt()

        when (pushData["notification_type"]?.toInt()) {
            1 -> {
                normalNotificationData.projectId = pushData["id"]?.toInt()
            }
            2, 3, 4 -> {
                normalNotificationData.taskId = pushData["id"]?.toInt()
                normalNotificationData.projectId = pushData["project_id"]?.toInt()
            }
            5, 7 -> {
                normalNotificationData.taskId = pushData["task_id"]?.toInt()
                normalNotificationData.projectId = pushData["project_id"]?.toInt()
            }
        }

        normalNotificationData.projectParticipantId =
            pushData["project_participant_id"]?.toInt()
        normalNotificationData.title = pushData["title"]
        normalNotificationData.isNote = pushData["is_note"]?.toBoolean()
        normalNotificationData.comment = pushData["comment"]
        normalNotificationData.parent_id = pushData["parent_id"]?.toInt()
        normalNotificationData.notificationId = pushData["notification_id"]?.toInt()
        normalNotificationData.assignColor = pushData["assign_color"]

        MainActivity.notificationPayload = null
        Log.e("normalNotificationData", normalNotificationData.toString())
        val actionIntent = Intent(context, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable("NotificationResponse", normalNotificationData)
        actionIntent.putExtra("NotificationBundle", bundle)
        actionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        actionIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        actionIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        actionIntent.action = System.currentTimeMillis().toString()

        val pendingIntent: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, iUniqueId, actionIntent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(
                this,
                iUniqueId,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        return pendingIntent
    }*/

}