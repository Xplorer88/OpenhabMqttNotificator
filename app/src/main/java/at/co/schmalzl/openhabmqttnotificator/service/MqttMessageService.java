package at.co.schmalzl.openhabmqttnotificator.service;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;
import at.co.schmalzl.openhabmqttnotificator.MainActivity;
import at.co.schmalzl.openhabmqttnotificator.R;
import at.co.schmalzl.openhabmqttnotificator.receiver.MqttRestarterBroadcastReceiver;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Julian
 */
public class MqttMessageService extends Service
{

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    private String server, user, password, clientId, infoTopic, warnTopic, errorTopic;
    private boolean disableService;
    private int startId;

    public MqttMessageService()
    {

    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Log.d(TAG, "onStartCommand");
        if (intent == null) Log.d(TAG, "Intent is null");
        this.startId = startId;

        loadSharedPreference();

        pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), server, clientId, user, password);

        mqttAndroidClient.setCallback(new MqttCallbackExtended()
        {
            @Override
            public void connectComplete(boolean b, String s)
            {
                subscribeTopics(infoTopic, warnTopic, errorTopic);
            }

            @Override
            public void connectionLost(Throwable throwable)
            {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception
            {
                setMessageNotification(s, new String(mqttMessage.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
            {

            }
        });

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        loadSharedPreference();
        if (this.disableService)
        {
            try
            {
                unsubscribeTopics(infoTopic, warnTopic, errorTopic);
                mqttAndroidClient.disconnect();
                mqttAndroidClient.unregisterResources();
            } catch (MqttException e)
            {
                e.printStackTrace();
            }
            stopSelf(startId);
            return;
        }
        Intent broadcastIntent = new Intent("at.co.schmalzl.openhabmqttnotificator.MqttRestarter");
        sendBroadcast(broadcastIntent);
    }

    private void subscribeTopics(String infoTopic, String warnTopic, String errorTopic)
    {

        if (infoTopic != null && !infoTopic.matches(""))
        {
            try
            {
                mqttAndroidClient.subscribe(infoTopic, 1);
            } catch (MqttException e)
            {
                e.printStackTrace();
            }
        }

        if (warnTopic != null && !warnTopic.matches(""))
        {
            try
            {
                mqttAndroidClient.subscribe(warnTopic, 1);
            } catch (MqttException e)
            {
                e.printStackTrace();
            }
        }

        if (errorTopic != null && !errorTopic.matches(""))
        {
            try
            {
                mqttAndroidClient.subscribe(errorTopic, 1);
            } catch (MqttException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void unsubscribeTopics(String infoTopic, String warnTopic, String errorTopic)
    {
        if (infoTopic != null && !infoTopic.matches(""))
        {
            try
            {
                mqttAndroidClient.unsubscribe(infoTopic);
            } catch (MqttException e)
            {
                e.printStackTrace();
            }
        }

        if (warnTopic != null && !warnTopic.matches(""))
        {
            try
            {
                mqttAndroidClient.unsubscribe(warnTopic);
            } catch (MqttException e)
            {
                e.printStackTrace();
            }
        }

        if (errorTopic != null && !errorTopic.matches(""))
        {
            try
            {
                mqttAndroidClient.unsubscribe(errorTopic);
            } catch (MqttException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void loadSharedPreference()
    {
        Log.d(MqttRestarterBroadcastReceiver.class.getSimpleName(), "Trying to load settings from shared preferences");
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("openhabMqtt", Context.MODE_PRIVATE);

        this.server = sharedPref.getString("server", "");
        this.user = sharedPref.getString("user", "");
        this.password = sharedPref.getString("password", "");
        this.clientId = sharedPref.getString("clientId", "");
        this.infoTopic = sharedPref.getString("infoTopic", "");
        this.warnTopic = sharedPref.getString("warnTopic", "");
        this.errorTopic = sharedPref.getString("errorTopic", "");
        this.disableService = sharedPref.getBoolean("disableService", true);

    }

    private void setMessageNotification(@NonNull String topic, @NonNull String msg)
    {

        NotificationCompat.Builder mBuilder;
        RemoteViews collapsedView;
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (topic.matches(infoTopic))
        {
            collapsedView = new RemoteViews(getPackageName(), R.layout.notification_collapsed_info);
            collapsedView.setTextViewText(R.id.timestamp, DateUtils.formatDateTime(this, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME));
            collapsedView.setTextViewText(R.id.content_text, msg);
            collapsedView.setTextViewText(R.id.content_title, "OpenHAB info");
            mBuilder = new NotificationCompat.Builder(this, "mqtt")
                    .setSmallIcon(R.drawable.ic_info)
                    .setContentTitle("OpenHAB info")
                    .setContentText(msg)
                    .setAutoCancel(true)
                    .setCustomContentView(collapsedView)
                    .setSound(soundUri)
                    .setVibrate(new long[]{0, 250, 250, 250})
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        } else if (topic.matches(warnTopic))
        {
            collapsedView = new RemoteViews(getPackageName(), R.layout.notification_collapsed_warn);
            collapsedView.setTextViewText(R.id.timestamp, DateUtils.formatDateTime(this, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME));
            collapsedView.setTextViewText(R.id.content_text, msg);
            collapsedView.setTextViewText(R.id.content_title, "OpenHAB warning");
            mBuilder = new NotificationCompat.Builder(this, "mqtt")
                    .setSmallIcon(R.drawable.ic_warn)
                    .setContentTitle("OpenHAB warning")
                    .setContentText(msg)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setVibrate(new long[]{0, 500, 0, 500})
                    .setCustomContentView(collapsedView)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        } else
        {
            collapsedView = new RemoteViews(getPackageName(), R.layout.notification_collapsed_warn);
            collapsedView.setTextViewText(R.id.timestamp, DateUtils.formatDateTime(this, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME));
            collapsedView.setTextViewText(R.id.content_text, msg);
            collapsedView.setTextViewText(R.id.content_title, "OpenHAB error");
            mBuilder = new NotificationCompat.Builder(this, "mqtt")
                    .setSmallIcon(R.drawable.ic_error)
                    .setContentTitle("OpenHAB error")
                    .setContentText(msg)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setVibrate(new long[]{0, 500, 0, 500, 0, 500})
                    .setCustomContentView(collapsedView)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        }

        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(ThreadLocalRandom.current().nextInt(0, 1000 + 1), mBuilder.build());
    }


}
