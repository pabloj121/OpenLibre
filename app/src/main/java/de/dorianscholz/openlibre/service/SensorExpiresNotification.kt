package de.dorianscholz.openlibre.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import de.dorianscholz.openlibre.OpenLibre
import de.dorianscholz.openlibre.R
import de.dorianscholz.openlibre.model.AlgorithmUtil
import de.dorianscholz.openlibre.model.SensorData
import de.dorianscholz.openlibre.ui.MainActivity
import io.realm.Realm
import io.realm.Sort
import java.util.concurrent.TimeUnit

val NOTIFICATION_ID = 123
val CHANNEL_ID = "mychannel"

class SesnsorExpiresNotification : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val availableSensor = false
        val realmProcessedData = Realm.getInstance(OpenLibre.realmConfigProcessedData)
        val sensorDataResults = realmProcessedData.where(SensorData::class.java).findAllSorted(SensorData.START_DATE, Sort.DESCENDING)

        if (sensorDataResults.size == 0) {
            Toast.makeText(this, R.string.no_sensor_registered, Toast.LENGTH_SHORT).show()
        } else {

            val sensorData = sensorDataResults.first()
            // sendNotification(sensorData)

            val timeLeft = sensorData.timeLeft

            if (timeLeft >= TimeUnit.MINUTES.toMillis(1L)) {
                sendNotification(sensorData)
            } else {
                Toast.makeText(this, R.string.sensor_expired, Toast.LENGTH_SHORT).show()
            }
        }

        realmProcessedData.close()
    }

    fun getNotificationBuilder(sensorData: SensorData):NotificationCompat.Builder{
        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.sensor_expires_soon))
                .setContentText(R.string.sensor_expires_in.toString() +
                        AlgorithmUtil.getDurationBreakdown(resources, sensorData.timeLeft))
    }

    fun sendNotification(sensorData: SensorData) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = getNotificationBuilder(sensorData).setContentIntent(pendingIntent)

        // From API 26 you can create different channels to send notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            val name = "mychannel"
            val description = "This is my channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = description
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100,200,300,400,500,400,300,200,400)
            mChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(mChannel)
        }

        notificationManager.notify(220, notification.build())
    }
}