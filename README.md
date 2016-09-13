README
======

Trackbook - Movement Recorder for Android
-----------------------------------------

**Version 0.9.x ("The Great Gig in the Sky")**

Trackbook is a bare bones app for recording your movements. Trackbook is great for hiking, vacation or workout. Once started it traces your movements on a map. The map data is provided by [OpenStreetMap (OSM)](https://www.openstreetmap.org/).

Trackbook is free software. It is published under the [MIT open source license](https://opensource.org/licenses/MIT). Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid) to display the map, which is also free software published under the [Apache License](https://github.com/osmdroid/osmdroid/blob/master/LICENSE). Want to help? Please check out the notes in [CONTRIBUTE.md](https://github.com/y20k/transistor/blob/master/CONTRIBUTE.md) first.

Install Trackbook
-----------------
Version 0.9 is the first release of Trackbook. It is not completely finished yet, but you can install it. Watch the install canary... It finally flies(*). For now you can install it via Google Play or grab a Release APK here on [GitHub](https://github.com/y20k/trackbook/releases). Stay tuned for an [F-Droid release](https://f-droid.org/forums/topic/trackbook-movement-recorder-for-android/).

[<img src="https://play.google.com/intl/de_de/badges/images/generic/en_badge_web_generic.png" width="192">](https://play.google.com/store/apps/details?id=org.y20k.trackbook)

                     .--.
                    /   6\_
                    \   ___\
    ________________/   \_______________
    \_____         /     \        _____/
       \______     |     |     ______/
           \_____  \     / ______/
               \___/\   /\___/
                    /A A\
                   /     \
                   `"==="`

(*) before it just sat [here](https://github.com/y20k/trackbook/blob/42ce5a3e764cd84365afaf0bb58929482b9e0890/README.md)

How to use Trackbook
--------------------
### Start recording movements
Press the big blue button to start recording your movements. Trackbook will continuously drop markers onto the map.

### Stop recording movements
To stop your recording press the big blue button again or use the stop button in the Trackbook's notification. You can look at the recorded movements on the map afterwards.

### Distance and duration
Peek into Trackbook's notification to see the distance and duration of your current recording.

### Clear the map
You can clear the map by either long-pressing the big blue button or dismissing the notification.

Which Permissions does Trackbook need?
--------------------------------------
### Permission "INTERNET"
Trackbook needs to download map data from Open Street Map servers and therefore needs access to the internet.

### Permission "ACCESS_NETWORK_STATE" and "ACCESS_WIFI_STATE"
Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid/) to draw its main map. osmdroid needs to know the current state of your device’s connectivity - see [Prerequisites](https://github.com/osmdroid/osmdroid/wiki/Prerequisites). I am not sure why though. On the other hand: These permissions are not harmful in any way.

### Permission "ACCESS_COARSE_LOCATION" and "ACCESS_FINE_LOCATION"
Trackbook needs accurate GPS location data to be able to record your movements. If the GPS data is not available or not accurate enough Trackbook uses location data from cell tower and WiFi triangulation.

### Permission "WRITE_EXTERNAL_STORAGE"
Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid), which caches map tiles on Android's external storage. You can find the map cache in the `osmdroid` folder on the top level of the user-facing file system.

Developement screenshots
------------------------
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436615/9e6d973e-78f9-11e6-8d3d-21f655127579.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436615/9e6d973e-78f9-11e6-8d3d-21f655127579.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436620/a1d5b71c-78f9-11e6-8770-5b7955a7d762.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436620/a1d5b71c-78f9-11e6-8770-5b7955a7d762.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436623/a42cb416-78f9-11e6-9f34-e7b3203f1ea9.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436623/a42cb416-78f9-11e6-9f34-e7b3203f1ea9.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436627/a6822cfa-78f9-11e6-9662-0e7f245312e9.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436627/a6822cfa-78f9-11e6-9662-0e7f245312e9.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436628/a8b22692-78f9-11e6-9498-a48464285e6c.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436628/a8b22692-78f9-11e6-9498-a48464285e6c.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436629/aad5ac78-78f9-11e6-8e3d-915d46f76765.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436629/aad5ac78-78f9-11e6-8e3d-915d46f76765.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436631/ad2cf63e-78f9-11e6-9ea6-68bbfee0f7d4.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436631/ad2cf63e-78f9-11e6-9ea6-68bbfee0f7d4.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18436633/afe55aba-78f9-11e6-9720-0554fd5b4107.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18436633/afe55aba-78f9-11e6-9720-0554fd5b4107.png)

