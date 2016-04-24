package com.pavel.dolbik.casttext;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private MediaRouter          mediaRouter;
    private MediaRouteSelector   mediaRouteSelector;
    private MediaRouter.Callback mediaRouterCallback;
    private CastDevice           castDevice;

    private GoogleApiClient          apiClient;
    private Cast.Listener            castListener;
    private ConnectionCallbacks      connectionCallbacks;
    private ConnectionFailedListener connectionFailedListener;
    private boolean                  applicationStarted;
    private boolean                  waitingForReconnect;
    private String                   sessionId;
    private MessageChannel           messageChannel;

    private EditText messageEt;
    private Button   send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Настраиваем обноружение Cast устройств
        // Configure Cast device discovery
        mediaRouter        = MediaRouter.getInstance(getApplicationContext());
        mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(getResources().getString(R.string.app_id)))
                .build();
        mediaRouterCallback = new MyMediaRouterCallback();

        messageEt = (EditText) findViewById(R.id.message);
        send      = (Button)   findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(messageEt.getText().toString());
                messageEt.setText("");
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        // Устанавливаем селектор MediaRouteActionProvider для обнаружения устройств.
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
        return true;
    }


    @Override
    protected void onStart() {
        // Запускаем обнаружение устройства
        // Start media router discovery
        super.onStart();
        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }


    // Предоставляет методы для выполнения действий, когда устройство подключено или отключено.
    // Provides methods for performing actions when a route is selected or unselected.
    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d("Pasha", "onRouteSelected "+router.toString()+" "+info.toString());
            // Получаем выбранное пользователем устройство
            // Handle the user route selection.
            castDevice = CastDevice.getFromBundle(info.getExtras());
            launchReceiver();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d("Pasha", "onRouteUnselected");
            teardown(false);
            castDevice = null;
        }
    }


    // Запускаем приложение на приенике (TV, монитор, др)
    // Start the receiver app (TV, monitor, other)
    private void launchReceiver() {
        castListener = new Cast.Listener() {
            @Override
            public void onApplicationDisconnected(int statusCode) {
                Log.d("Pasha", "Application has stopped "+statusCode);
                teardown(true);
            }
        };


        // Подключаемся к сервису Google Play
        // Connect to Google Play services
        connectionCallbacks      = new ConnectionCallbacks();
        connectionFailedListener = new ConnectionFailedListener();
        Cast.CastOptions.Builder apiOptionsBuilder = new Cast.CastOptions.Builder(castDevice, castListener);
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
        apiClient.connect();
    }


    // Google Play services callbacks
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d("Pasha", "ConnectionCallbacks -> onConnected");

            if (apiClient == null) { return; }

            if (waitingForReconnect) {
                waitingForReconnect = false;

                // Проверяем, если приемное приложение все еще запущено
                // Check if the receiver app is still running
                if ((bundle != null)
                        && bundle.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                    Log.d("Pasha", "App  is no longer running");
                    teardown(true);
                } else {
                    // Пересоздаем канал для передачи сообщения
                    // Re-create the custom message channel
                    try {
                        Cast.CastApi.setMessageReceivedCallbacks(
                                apiClient,
                                messageChannel.getNamespace(),
                                messageChannel);
                    } catch (IOException e) {
                        Log.e("Pasha", "Exception while creating channel", e);
                    }
                }
            }

            // Запускаем приложение на приемнике
            // Launch the receiver app
            else {
                Cast.CastApi.launchApplication(apiClient, getString(R.string.app_id))
                        .setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                    @Override
                    public void onResult(@NonNull Cast.ApplicationConnectionResult result) {
                        Status status = result.getStatus();
                        Log.d("Pasha", "ApplicationConnectionResultCallback.onResult:" + status.getStatusCode());

                        if (status.isSuccess()) {
                            ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                            sessionId = result.getSessionId();
                            String applicationStatus = result.getApplicationStatus();
                            boolean wasLaunched = result.getWasLaunched();
                            applicationStarted = true;
                            Log.d("Pasha", "application name: "
                                    + applicationMetadata.getName() + ", status: " + applicationStatus
                                    + ", sessionId: " + sessionId + ", wasLaunched: " + wasLaunched);

                            // Создаем канал для передачи сообщений
                            // Create the custom message channel
                            messageChannel = new MessageChannel();
                            try {
                                Cast.CastApi.setMessageReceivedCallbacks(
                                        apiClient, messageChannel.getNamespace(), messageChannel);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            // Передаем сообщени по-умолчанию на приемное устройство
                            // Set the initial instructions on the receiver
                            sendMessage(getString(R.string.instructions));

                            messageEt.setVisibility(View.VISIBLE);
                            send.setVisibility(View.VISIBLE);
                        } else {
                            teardown(true);
                        }
                    }
                });
            }

        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d("Pasha", "ConnectionCallbacks -> onConnectionSuspended "+i);
            waitingForReconnect = true;
        }
    }


    // Google Play services callbacks
    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d("Pasha", "ConnectionFailedListener -> onConnectionFailed");
            teardown(false);
        }
    }


    // Custom канал для передачи сообщений
    // Custom message channel
    class MessageChannel implements Cast.MessageReceivedCallback {

        public String getNamespace() {
            return getString(R.string.namespace);
        }

        // Принемаем сообщения от принемающего app (TV, monitor, other)
        // Receive message from the receiver app (TV, monitor, other)
        @Override
        public void onMessageReceived(CastDevice castDevice, String s, String s1) {
            Log.d("Pasha", "onMessageReceived: " + s +" "+s1);
        }
    }


    // Передаем текстовое сообщение на приемник (TV, monitor, other)
    // Send a text message to the receiver (TV, monitor, other)
    private void sendMessage(String message) {
        Log.d("Pasha", "sendMessage "+message);
        if (apiClient != null && messageChannel != null) {
            try {
                Cast.CastApi.sendMessage(apiClient, messageChannel.getNamespace(), message).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status result) {
                                if (!result.isSuccess()) {
                                    Log.e("Pasha", "Sending message failed");
                                }
                            }
                        });
            } catch (Exception e) {
                Log.e("Pasha", "Exception while sending message", e);
            }
        } else {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    // Разрываем соединение с приемником
    // Tear down the connection to the receiver
    private void teardown(boolean selectDefaultRoute) {
        Log.d("Pasha", "Teardown");
        if (apiClient != null) {
            if (applicationStarted) {
                if (apiClient.isConnected() || apiClient.isConnecting()) {
                    try {
                        Cast.CastApi.stopApplication(apiClient, sessionId);
                        if (messageChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(apiClient, messageChannel.getNamespace());
                            messageChannel = null;

                            messageEt.setVisibility(View.GONE);
                            send.setVisibility(View.GONE);
                        }
                    } catch (IOException e) {
                        Log.e("Pasha", "Exception while removing channel", e);
                    }
                    apiClient.disconnect();
                }
                applicationStarted = false;
            }
            apiClient = null;
        }
        if (selectDefaultRoute) {
            mediaRouter.selectRoute(mediaRouter.getDefaultRoute());
        }
        castDevice = null;
        waitingForReconnect = false;
        sessionId = null;
    }


    @Override
    protected void onStop() {
        // Прекращаем пойск утройств
        // End media router discovery
        mediaRouter.removeCallback(mediaRouterCallback);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        teardown(true);
        super.onDestroy();
    }
}
