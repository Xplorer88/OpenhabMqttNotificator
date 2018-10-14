package at.co.schmalzl.openhabmqttnotificator.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import at.co.schmalzl.openhabmqttnotificator.service.MqttMessageService;

/**
 * Created by Julian
 */
public class MqttRestarterBroadcastReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i(MqttRestarterBroadcastReceiver.class.getSimpleName(), "Action: " + intent.getAction() + " -  Restarting!");
        SharedPreferences sharedPref = context.getSharedPreferences("openhabMqtt", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (!sharedPref.contains("serviceEnabled"))
        {
            Log.i(MqttRestarterBroadcastReceiver.class.getSimpleName(), "Preferences not found, ignoring");
            return;
        }

        if (sharedPref.getBoolean("disableService", true))
        {
            Log.i(MqttRestarterBroadcastReceiver.class.getSimpleName(), "Disabling Service...");
            context.stopService(new Intent(context, MqttMessageService.class));
            editor.putBoolean("serviceEnabled", false);
            editor.commit();
            return;
        }

        if (context.startService(new Intent(context, MqttMessageService.class)) != null)
        {
            editor.putBoolean("serviceEnabled", true);
        }
        else
        {
            editor.putBoolean("serviceEnabled", false);
        }
        editor.apply();

    }
}
