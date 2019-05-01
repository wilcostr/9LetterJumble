package za.co.twinc.a9letterjumble;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.Calendar;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by wilco on 2018/05/07.
 * 9LetterJumble
 */

public class AlarmReceiver extends BroadcastReceiver {
    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgr;
    // The pending intent that is triggered when the alarm fires.
    private PendingIntent alarmIntent;

    NotificationManager mNotifyMgr;
    public static final String PRIMARY_NOTIF_CHANNEL = "default";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        SharedPreferences mainLog = context.getSharedPreferences(MainActivity.MAIN_PREFS, 0);

        // Get the current day, setting hours and minutes to zero
        Calendar cal = Calendar.getInstance();
        long timeNow = System.currentTimeMillis();
        cal.setTimeInMillis(timeNow);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);

        // Check if the Daily Challenge has already been completed
        if (mainLog.getLong("last_challenge", 0L) > cal.getTimeInMillis() - 10*60*1000)
            return;

        // Return if Notifications switched off in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notify = settingsPref.getBoolean(SettingsActivity.KEY_PREF_CHALLENGE, true);
        if (!notify)
            return;

        // Create notification channel. No problem if already created previously
        mNotifyMgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    PRIMARY_NOTIF_CHANNEL, PRIMARY_NOTIF_CHANNEL, NotificationManager.IMPORTANCE_LOW);
            mNotifyMgr.createNotificationChannel(channel);
        }

        // Create intent to open Main, load habit number in extras
        Intent openMainIntent = new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(openMainIntent);
        PendingIntent openMainPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_ONE_SHOT);

        // Give a notification here
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, PRIMARY_NOTIF_CHANNEL)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.challenge_reminder))
                .setContentIntent(openMainPendingIntent)
                .setSmallIcon(R.drawable.nine)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setVibrate(new long[]{1000, 200, 100, 200});

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            mBuilder.setSmallIcon(R.drawable.nine_png);

        Notification noti = mBuilder.build();

        // Issue notification
        if (mNotifyMgr != null)
            mNotifyMgr.notify(0, noti);

    }

    public void setAlarm(Context context) {
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);

        // Don't fire off immediately if notification time has passed already
        if (calendar.getTimeInMillis() < System.currentTimeMillis())
            calendar.add(Calendar.DAY_OF_MONTH, 1);

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    public void cancelAlarm() {
        // If the alarm has been set, cancel it.
        if (alarmMgr != null) {
            alarmMgr.cancel(alarmIntent);
        }
    }
}
