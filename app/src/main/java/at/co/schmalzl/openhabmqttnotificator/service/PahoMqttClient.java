package at.co.schmalzl.openhabmqttnotificator.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

import java.io.UnsupportedEncodingException;

/**
 * Created by Julian
 */

public class PahoMqttClient
{

    private static final String TAG = "PahoMqttClient";
    private MqttAndroidClient mqttAndroidClient;

    public MqttAndroidClient getMqttClient(Context context, @NonNull String brokerUrl,  @NonNull String clientId, String user,
                                           String password)
    {

        if (mqttAndroidClient != null && mqttAndroidClient.isConnected())
        {
            return mqttAndroidClient;
        }

        mqttAndroidClient = new MqttAndroidClient(context, brokerUrl, clientId);
        try
        {
            IMqttToken token = mqttAndroidClient.connect(getMqttConnectionOption(user, password));

            token.setActionCallback(new IMqttActionListener()
            {
                @Override
                public void onSuccess(IMqttToken asyncActionToken)
                {
                    mqttAndroidClient.setBufferOpts(getDisconnectedBufferOptions());
                    Log.d(TAG, "Success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception)
                {
                    Log.d(TAG, "Failure " + exception.toString());
                }
            });
        } catch (MqttException e)
        {
            e.printStackTrace();
        }

        return mqttAndroidClient;
    }

    public void disconnect() throws MqttException
    {
        IMqttToken mqttToken = mqttAndroidClient.disconnect();
        mqttToken.setActionCallback(new IMqttActionListener()
        {
            @Override
            public void onSuccess(IMqttToken iMqttToken)
            {
                Log.d(TAG, "Successfully disconnected");
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable)
            {
                Log.d(TAG, "Failed to disconnected " + throwable.toString());
            }
        });
    }

    @NonNull
    private DisconnectedBufferOptions getDisconnectedBufferOptions()
    {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(100);
        disconnectedBufferOptions.setPersistBuffer(false);
        disconnectedBufferOptions.setDeleteOldestMessages(false);
        return disconnectedBufferOptions;
    }

    @NonNull
    private MqttConnectOptions getMqttConnectionOption(String user, String password)
    {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setAutomaticReconnect(true);
        //mqttConnectOptions.setWill(Constants.PUBLISH_TOPIC, "I am going offline".getBytes(), 1, true);
        mqttConnectOptions.setUserName(user);
        mqttConnectOptions.setPassword(password.toCharArray());
        return mqttConnectOptions;
    }


    public void publishMessage(@NonNull MqttAndroidClient client, @NonNull String msg, int qos, @NonNull String topic)
            throws MqttException, UnsupportedEncodingException
    {
        byte[] encodedPayload = new byte[0];
        encodedPayload = msg.getBytes("UTF-8");
        MqttMessage message = new MqttMessage(encodedPayload);
        message.setId(320);
        message.setRetained(true);
        message.setQos(qos);
        client.publish(topic, message);
    }

    public void subscribe(@NonNull MqttAndroidClient client, @NonNull final String topic, int qos) throws MqttException
    {
        if (!client.isConnected())
        {
            return;
        }

        IMqttToken token = client.subscribe(topic, qos);
        token.setActionCallback(new IMqttActionListener()
        {
            @Override
            public void onSuccess(IMqttToken iMqttToken)
            {
                Log.d(TAG, "Subscribe Successfully " + topic);
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable)
            {
                Log.e(TAG, "Subscribe Failed " + topic);

            }
        });
    }

    public void unSubscribe(@NonNull MqttAndroidClient client, @NonNull final String topic) throws MqttException
    {

        IMqttToken token = client.unsubscribe(topic);

        token.setActionCallback(new IMqttActionListener()
        {
            @Override
            public void onSuccess(IMqttToken iMqttToken)
            {
                Log.d(TAG, "UnSubscribe Successfully " + topic);
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable)
            {
                Log.e(TAG, "UnSubscribe Failed " + topic);
            }
        });
    }

}


