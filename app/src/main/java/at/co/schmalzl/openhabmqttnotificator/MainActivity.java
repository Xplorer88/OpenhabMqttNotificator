package at.co.schmalzl.openhabmqttnotificator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.UUID;

public class MainActivity extends AppCompatActivity
{

    private String TAG = "MainActivity";
    private String clientId;
    private boolean serviceEnabled, disableService;

    private EditText etServer, etUser, etPassword, etInfoTopic, etWarnTopic, etErrorTopic;
    private TextView twMessages;
    private Button btEnableService;

    private SharedPreferences sharedPref;

    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getApplicationContext().getSharedPreferences("openhabMqtt", Context.MODE_PRIVATE);

        etServer = (EditText) findViewById(R.id.edServer);
        etUser = (EditText) findViewById(R.id.edUser);
        etPassword = (EditText) findViewById(R.id.edPassword);

        etInfoTopic = (EditText) findViewById(R.id.edInfoTopic);
        etWarnTopic = (EditText) findViewById(R.id.edWarnTopic);
        etErrorTopic = (EditText) findViewById(R.id.edErrorTopic);

        btEnableService = (Button) findViewById(R.id.btSubscribe);

        twMessages = (TextView) findViewById(R.id.twMessages);

        loadPreferences();

        btEnableService.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loadPreferences();

                if (serviceEnabled)
                {
                    disableService = true;
                    if (serviceIntent == null)
                    {
                        serviceIntent = new Intent("at.co.schmalzl.openhabmqttnotificator.MqttRestarter");
                    }
                    twMessages.setText("Service disabled");
                    btEnableService.setText("Enable service");
                    writePreferences();
                    sendBroadcast(serviceIntent);
                    return;
                }

                String server = etServer.getText().toString();
                String infoTopic = etInfoTopic.getText().toString();
                String warnTopic = etWarnTopic.getText().toString();
                String errorTopic = etErrorTopic.getText().toString();

                if (infoTopic.isEmpty() && warnTopic.isEmpty() && errorTopic.isEmpty() && server.isEmpty())
                {
                    twMessages.setText("Missing parameter");
                    return;
                }

                serviceIntent = new Intent("at.co.schmalzl.openhabmqttnotificator.MqttRestarter");

                serviceEnabled = true;
                disableService = false;
                twMessages.setText("Service enabled");
                btEnableService.setText("Disable service");
                writePreferences();

                sendBroadcast(serviceIntent);

            }
        });

    }

    /*private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }*/


    @Override
    protected void onDestroy()
    {
        if (serviceIntent != null)
        {
            sendBroadcast(serviceIntent);
        }
        Log.i(TAG, "onDestroy!");
        super.onDestroy();

    }

    private void loadPreferences()
    {
        // The following three parameters are needed at the first service start
        serviceEnabled = sharedPref.getBoolean("serviceEnabled", false);
        disableService = sharedPref.getBoolean("disableService", false);
        clientId = sharedPref.getString("clientId", "ANDROID_CLIENT_" + UUID.randomUUID().toString());

        // Prevent to overwrite the params on first start
        if (!sharedPref.contains("server"))
        {
            return;
        }

        etServer.setText(sharedPref.getString("server", ""));
        etUser.setText(sharedPref.getString("user", ""));
        etPassword.setText(sharedPref.getString("password", ""));
        etInfoTopic.setText(sharedPref.getString("infoTopic", ""));
        etWarnTopic.setText(sharedPref.getString("warnTopic", ""));
        etErrorTopic.setText(sharedPref.getString("errorTopic", ""));

        if (serviceEnabled)
        {
            twMessages.setText("Service enabled");
            btEnableService.setText("Disable service");
        } else
        {
            twMessages.setText("Service disabled");
            btEnableService.setText("Enable service");
        }
    }

    private void writePreferences()
    {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("serviceEnabled", this.serviceEnabled);
        editor.putString("server", etServer.getText().toString());
        editor.putString("user", etUser.getText().toString());
        editor.putString("password", etPassword.getText().toString());
        editor.putString("clientId", clientId);
        editor.putString("infoTopic", etInfoTopic.getText().toString());
        editor.putString("warnTopic", etWarnTopic.getText().toString());
        editor.putString("errorTopic", etErrorTopic.getText().toString());
        editor.putBoolean("disableService", disableService);
        editor.commit();
    }
}
