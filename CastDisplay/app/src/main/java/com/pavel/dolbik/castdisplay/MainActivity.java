package com.pavel.dolbik.castdisplay;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.IntegerRes;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MediaRouter           mediaRouter;
    private MediaRouteSelector    mediaRouteSelector;
    private MyMediaRouterCallback mediaRouterCallback;

    private LinearLayout container;
    private ImageView    image;
    private Button       changeImage;

    private ArrayList<Integer> images;
    private int imageIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        checkGooglePlayServices();

        // Настраиваем обноружение Cast устройств
        // Configure Cast device discovery
        mediaRouter        = MediaRouter.getInstance(getApplicationContext());
        mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(getResources().getString(R.string.app_id)))
                .build();
        mediaRouterCallback = new MyMediaRouterCallback();
    }


    private void initView() {
        container   = (LinearLayout) findViewById(R.id.container);
        image       = (ImageView)    findViewById(R.id.image);
        changeImage = (Button)       findViewById(R.id.change_img);
        changeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Pasha", "Change Image");
                changeImage();
            }
        });

        images = new ArrayList<>();
        images.add(R.drawable.image_1);
        images.add(R.drawable.image_2);
        images.add(R.drawable.image_3);
    }


    private void changeImage() {
        PresentationService presentationService = (PresentationService) CastRemoteDisplayLocalService.getInstance();
        if (presentationService != null) {
            if (imageIndex > 2) { imageIndex = 0; }
            presentationService.changeImage(images.get(imageIndex));
            image.setImageResource(images.get(imageIndex));
            imageIndex++;
        }
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
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            // Получаем выбранное пользователем устройство
            // Handle the user route selection.
            Log.d("Pasha", "onRouteSelected");
            CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            if (castDevice != null) {
                startCastService(castDevice);
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d("Pasha", "onRouteUnselected");
            if (CastRemoteDisplayLocalService.getInstance() != null) {
                CastRemoteDisplayLocalService.stopService();

                container.setVisibility(View.GONE);
            }
        }
    }


    private void startCastService(final CastDevice castDevice) {
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent notificationPendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);

        CastRemoteDisplayLocalService.NotificationSettings settings =
                new CastRemoteDisplayLocalService.NotificationSettings.Builder()
                        .setNotificationPendingIntent(notificationPendingIntent).build();

        CastRemoteDisplayLocalService.startService(
                MainActivity.this,
                PresentationService.class,
                getString(R.string.app_id),
                castDevice,
                settings,
                new CastRemoteDisplayLocalService.Callbacks() {

            @Override
            public void onServiceCreated(CastRemoteDisplayLocalService castRemoteDisplayLocalService) {
                Log.d("Pasha", "onServiceCreated");
            }

            @Override
            public void onRemoteDisplaySessionStarted(CastRemoteDisplayLocalService castRemoteDisplayLocalService) {
                Log.d("Pasha", "onRemoteDisplaySessionStarted");
                container.setVisibility(View.VISIBLE);
                image.setImageResource(images.get(imageIndex));
            }

            @Override
            public void onRemoteDisplaySessionError(Status status) {
                Log.d("Pasha", "onRemoteDisplaySessionError status "+status.toString());
                MainActivity.this.finish();

            }
        });


    }

    // Утилитный метод, который проверяет, доступен ли Google Play Service
    // Если не доступен - показываем диалоговое окно
    /**
     * A utility method to validate that the appropriate version of the Google Play Services is
     * available on the device. If not, it will open a dialog to address the issue. The dialog
     * displays a localized message about the error and upon user confirmation (by tapping on
     * dialog) will direct them to the Play Store if Google Play services is out of date or
     * missing, or to system settings if Google Play services is disabled on the device.
     */
    private boolean checkGooglePlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, 0).show();
            }
            return false;
        }
        return true;
    }


    @Override
    protected void onStop() {
        // Прекращаем пойск утройств
        // End media router discovery
        mediaRouter.removeCallback(mediaRouterCallback);
        super.onStop();
    }
}
