package com.citypeople.project.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.citypeople.project.MyApplication
import com.citypeople.project.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private var map: java.util.HashMap<String, Int> = HashMap()

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/9bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            remoteMessage.data.keys.forEach {
                Log.d(TAG, "Message data payload Keys: $it" + remoteMessage.data[it])
            }

        }
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        //{description=New Notification, msg=New Notification, text=New Notification,
        // type=14, title=New Notification, admin_approved_status=1
        // }

//        if (remoteMessage.data.containsKey("chatId")) { //for chat notifications
//            val message = remoteMessage.data["message"]
//            val messageType = remoteMessage.data["messageType"]
//            if (messageType == MessageType.IMAGE) { // for chat image notification
//                sendChatNotification(
//                    remoteMessage.data["name"] + " sent image", "",
//                    remoteMessage.data, messageType
//                )
//            } else {  // for text message notifications
//                sendChatNotification(
//                    message, "Message from " + remoteMessage.data["name"],
//                    remoteMessage.data, messageType
//                )
//            }
        //    } else { // for other api notifications
        sendNotification(
            remoteMessage.data["title"],
            remoteMessage.data["description"], remoteMessage.data
        )
        //      }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(
        title: String?,
        messageBody: String?,
        data: MutableMap<String, String>
    ) {
        Handler(Looper.getMainLooper()).post {
            MyApplication.notificationArrived.postValue(true)
        }

        val notificationType = data["type"]?.toInt()

        val bundle = Bundle()
        val list = data.keys.toList()
        for (i in list.indices) {
            bundle.putString(list[i], data[list[i]])
        }

        var intent: Intent? = null

        /*     if (MyApplication.isAppBackground)
                 intent = Intent(this, SplashActivity::class.java)
                     .putExtra(Constants.NOTIFICATION_DATA, bundle)
             else {
                 if (notificationType!! >= Constants.NOTIFICATION_TYPE_BOOKING_PENDING && notificationType <= Constants.NOTIFICATION_TYPE_BOOKING_ARRIVED) {
                     val orderId = bundle.getString("order_id")?.toInt()
                     intent = Intent(this, BookingDetailsActivity::class.java)
                         .putExtra(Constants.NOTIFICATION_DATA, bundle)
                         .putExtra(Constants.ORDER_ID, orderId)
                 } else {
                     intent = Intent(this, HomeActivity::class.java)
                         .putExtra(Constants.NOTIFICATION_DATA, bundle)

                 }
             }*/

        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

   /*     val pendingIntent = PendingIntent.getActivity(
            this, 0 *//* Request code *//*, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )*/

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val attributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.citypeople_small_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
          //  .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notification from CityPeople",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.enableVibration(true);
            channel.setSound(defaultSoundUri, attributes); // This is IMPORTANT
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    /*   private fun sendChatNotification(
           title: String?,
           messageBody: String?,
           data: MutableMap<String, String>,
           messageType: String?
       ) {
           if (ChatActivity.isActive)
               return

           var notificationTitle = title
           var notificationDescription = messageBody

           var bitmap: Bitmap? = null
           val message = data["message"]

           if (messageType == MessageType.IMAGE)
               bitmap = Glide.with(applicationContext)
                   .asBitmap()
                   .load(message)
                   .submit()
                   .get()


           val bundle = Bundle()
           val list = data.keys.toList()

           for (i in list.indices) {
               when {
                   list[i] == "registrationTokens" -> {
                       Log.d("registrationTokens", data["registrationTokens"]!!)
                       var tokenList: ArrayList<String> = ArrayList()
                       val stringList = data["registrationTokens"]!!.drop(2).dropLast(2)
                       if (stringList.contains(",")) {
                           tokenList =
                               stringList.split(",").map { it.trim() }.toList() as ArrayList<String>
                       } else {
                           tokenList.add(stringList)
                       }
                       bundle.putStringArrayList("registrationTokens", tokenList)
                   }
                   else -> bundle.putString(list[i], data[list[i]])
               }
           }

           var intent: Intent? = null

           if (JustSayWhatApp.isAppBackground)
               intent = Intent(this, SplashActivity::class.java)
                   .putExtra(Constants.NOTIFICATION_CHAT_DATA, bundle)
           else {
               intent = Intent(this, ChatActivity::class.java)
                   .putExtra(Constants.NOTIFICATION_CHAT_DATA, bundle)
           }



           intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

           val pendingIntent = PendingIntent.getActivity(
               this, 0 *//* Request code *//*, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (messageBody.isNullOrEmpty()) {
            notificationDescription = title
            notificationTitle = getString(R.string.app_name)
        }

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_just_say_what)
            .setContentTitle(notificationTitle)
            .setContentText(notificationDescription)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        if (messageType == MessageType.IMAGE && bitmap != null)
            notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))


        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notification from JSW",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.enableVibration(true);
            channel.setSound(defaultSoundUri, attributes); // This is IMPORTANT
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 *//* ID of notification *//*, notificationBuilder.build())
    }*/

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
