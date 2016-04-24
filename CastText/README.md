## Cast Text

Передача текста с мобильного устройства на приемник(TV) используя ChromeCast.<br/>
Sending text from your mobile device to the receiver (TV) using ChromeCast.<br/>

#### Configure
Настраиваем обноружение Cast устройств. <br/>
Configure Cast device discovery. <br/>

```java
private MediaRouter          mediaRouter;
private MediaRouteSelector   mediaRouteSelector;
private MediaRouter.Callback mediaRouterCallback;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

	mediaRouter        = MediaRouter.getInstance(getApplicationContext());
	mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
	CastMediaControlIntent.categoryForCast(getResources().getString(R.string.app_id))).build();
	mediaRouterCallback = new MyMediaRouterCallback();
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
```

#### MediaRouter.Callback
Предоставляет методы для выполнения действий, когда устройство подключено или отключено.<br/>
Provides methods for performing actions when a route is selected or unselected.<br/>
```java
private class MyMediaRouterCallback extends MediaRouter.Callback {
    @Override
	public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
        // Получаем выбранное пользователем устройство
        // Handle the user route selection.
        castDevice = CastDevice.getFromBundle(info.getExtras());
        launchReceiver();
    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
        teardown(false);
        castDevice = null;
    }
}
```


