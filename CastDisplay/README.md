## Cast Display

Передача изображений и музыки с мобильного устройства на приемник(TV) используя ChromeCast.<br/>
Sending images and sound from your mobile device to the receiver (TV) using ChromeCast.<br/>

#### Configure
Для того что бы запустить приложение, нужно указать имя пакета - **com.pavel.dolbik.castdisplay, app_id - 00A9CB1D**<br/>
For run the application, you need to specify the name of the package - **com.pavel.dolbik.castdisplay, app_id - 00A9CB1D** <br/>

Или создайте свой собственный app_id.<br/>
Or create your own app_id. <br/>
https://cast.google.com/publish/#/overview<br/>

<img src="/screenshots/screen.jpg" alt="screen" title="screen" width="1000" height="1000" />

1. Add new application. (Add new application.)<br/>
2. Выберете Remote Display Receiver. (Select Remote Display Receiver)<br/>
3. Отредактируйте свое приложение нажав Edit. (Edit your application by clicking Edit)<br/>
4. Укажите Package Name вашего приложенияю (Set Package Name your applications.)<br/>
5. Опубликуйте приложение нажав Publish. (Publish the app by clicking Publish.)<br/>
6. Приблизительно через часов 12, ваше приложение будет доступно. (After about 12 hours, your application will be available.)<br/>

#### Check Google Play Services
Утилитный метод, который проверяет, доступен ли Google Play Service.
A utility method to validate that the appropriate version of the Google Play Services is available on the device.
```java
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
```

#### Initialization
Настраиваем обноружение Cast устройств. <br/>
Configure Cast device discovery. <br/>
```java
private MediaRouter           mediaRouter;
private MediaRouteSelector    mediaRouteSelector;
private MyMediaRouterCallback mediaRouterCallback;
	
mediaRouter = MediaRouter.getInstance(getApplicationContext());
mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
        CastMediaControlIntent.categoryForCast(getResources().getString(R.string.app_id)))
        .build();
mediaRouterCallback = new MyMediaRouterCallback();

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
```

#### MediaRouter.Callback
Предоставляет методы для выполнения действий, когда устройство подключено или отключено.<br/>
Provides methods for performing actions when a route is selected or unselected.<br/>
```java
private class MyMediaRouterCallback extends MediaRouter.Callback {
	@Override
    public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
		// Получаем выбранное пользователем устройство
        // Handle the user route selection.
        CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
        if (castDevice != null) {
            startCastService(castDevice);
        }
    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
        if (CastRemoteDisplayLocalService.getInstance() != null) {
            CastRemoteDisplayLocalService.stopService();
            container.setVisibility(View.GONE);
        }
    }
}
```

#### Lifecycle
```java
@Override
protected void onStart() {
    // Запускаем обнаружение устройства
    // Start media router discovery
    super.onStart();
    mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
}

@Override
protected void onStop() {
    // Прекращаем пойск утройств
    // End media router discovery
    mediaRouter.removeCallback(mediaRouterCallback);
    super.onStop();
}
```

#### Start Cast Service
```java
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
```

#### Manifest
```java
<service
    android:name=".PresentationService"
    android:exported="false"
    />
```

#### PresentationService
```java
private MyPresentation myPresentation;

@Override
public void onCreatePresentation(Display display) {
    Log.d("Pasha", "onCreatePresentation");
    dismissPresentation();
    myPresentation = new MyPresentation(this, display);

    try {
        myPresentation.show();
    } catch (WindowManager.InvalidDisplayException ex) {
        Log.e("Pasha", "Unable to show presentation, display was removed.", ex);
        dismissPresentation();
    }
}
```

#### Presentation
```java
private class MyPresentation extends CastPresentation {

    public MyPresentation(Context serviceContext, Display display) {
        super(serviceContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentation);
    }
}
```

#### Dismiss Presentation
```java
@Override
public void onDismissPresentation() {
    dismissPresentation();
}

private void dismissPresentation() {
    if (myPresentation != null) {
        myPresentation.dismiss();
        myPresentation = null;
    }
}
```





