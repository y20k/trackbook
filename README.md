README
======

Trackbook - Movement Recorder for Android
-----------------------------------------

**Version 0.1.x ("The Great Gig in the Sky")**

Trackbook is a bare bones app for recording your movements. Trackbook is great for hiking, vacation or workout. Once started it displays your movements on a map. 

Trackbook is free software. It is published under the [MIT open source license](https://opensource.org/licenses/MIT). Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid) to display the map, which is also free software published under the [Apache License](https://github.com/osmdroid/osmdroid/blob/master/LICENSE). Want to help? Please check out the notes in [CONTRIBUTE.md](https://github.com/y20k/transistor/blob/master/CONTRIBUTE.md) first.

Install Trackbook
------------------
Do not install Trackbook. Trackbook is not finished yet. See the Install Canary below? Wait until it flies. 

            .---.
           /   6_6
           \_  (__\
           //   \\
          ((     ))
    =======""===""===============
             |||
             |||
             '|'

Developement screenshot(s)
--------------------------
[<img src="https://cloud.githubusercontent.com/assets/9103935/18307128/4ca8e8a8-74ef-11e6-9f47-33f31bdff36e.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18307128/4ca8e8a8-74ef-11e6-9f47-33f31bdff36e.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18307129/4ca963be-74ef-11e6-8b5b-f4ea6793a1d1.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18307129/4ca963be-74ef-11e6-8b5b-f4ea6793a1d1.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18307132/4cabc816-74ef-11e6-9752-237e3f1611fc.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18307132/4cabc816-74ef-11e6-9752-237e3f1611fc.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18307131/4caa247a-74ef-11e6-8059-41abfe11f71c.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18307131/4caa247a-74ef-11e6-8059-41abfe11f71c.png)
[<img src="https://cloud.githubusercontent.com/assets/9103935/18307130/4ca9f270-74ef-11e6-96e7-d918e807e9a2.png" width="240">](https://cloud.githubusercontent.com/assets/9103935/18307130/4ca9f270-74ef-11e6-96e7-d918e807e9a2.png)

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
---------------------------------------
### Permission "INTERNET"
Trackbook needs to download map data from Open Street Map servers and therefore needs access to the internet.

### Permission "ACCESS_NETWORK_STATE" and "ACCESS_WIFI_STATE"
Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid/) to draw its main map. osmdroid needs to know the current state of your device’s connectivity - see [Prerequisites](https://github.com/osmdroid/osmdroid/wiki/Prerequisites). I am not sure why though. On the other hand: These permissions are not harmful in any way.

### Permission "ACCESS_COARSE_LOCATION" and "ACCESS_FINE_LOCATION"
Trackbook needs accurate GPS location data to be able to record your movements. If the GPS data is not available or not accurate enough Trackbook uses location data from cell tower and WiFi triangulation.

### Permission "WRITE_EXTERNAL_STORAGE"
Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid), which caches map tiles on Android's external storage. You can find the map cache in the `osmdroid` folder on the top level of the user-facing file system.
