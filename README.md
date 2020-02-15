README
======

# Trackbook - Android Movement Recorder
<img src="https://raw.githubusercontent.com/y20k/trackbook/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="192" />

**Version 2.0.x ("Echoes")**

Trackbook is a bare bones app for recording your movements. Trackbook is great for hiking, vacation or workout. Once started it traces your movements on a map. The map data is provided by [OpenStreetMap (OSM)](https://www.openstreetmap.org/).

Trackbook is free software. It is published under the [MIT open source license](https://opensource.org/licenses/MIT). Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid) to display the map, which is also free software published under the [Apache License](https://github.com/osmdroid/osmdroid/blob/master/LICENSE). Want to help? Please check out the notes in [CONTRIBUTE.md](https://github.com/y20k/trackbook/blob/master/CONTRIBUTE.md) first.

## Install Trackbook
You can install it via Google Play and F-Droid - or you can go and grab the latest APK on [GitHub](https://github.com/y20k/trackbook/releases).

[<img src="https://play.google.com/intl/de_de/badges/images/generic/en_badge_web_generic.png" width="192">](https://play.google.com/store/apps/details?id=org.y20k.trackbook)

[<img src="https://cloud.githubusercontent.com/assets/9103935/14702535/45f6326a-07ab-11e6-9256-469c1dd51c22.png" width="192">](https://f-droid.org/repository/browse/?fdid=org.y20k.trackbook)

## Good To Know

### Start Recording via Quick Settings Tile
You can start a recording without opening Trackbook. Just pull down the System's Quick Settings and tap on the Start Recording tile. You need to manually add Trackbook's Recording tile to Quick Settings first. You can find information on customizing Quick Settings [here](https://support.google.com/android/answer/9083864?hl=en) and [here](https://www.xda-developers.com/get-custom-quick-settings-tiles/)

### Save Recordings as GPX
Recordings can be exported as GPX ([GPS Exchange Format](https://en.wikipedia.org/wiki/GPS_Exchange_Format)). Tap on the save button in the lower right corner when viewing a previosly recorded track.

### Copy GPX Files Manually
Trackbook automacally generates GPX files for every recording. You can find them in the folder `/Android/data/org.y20k.trackbook/files/gpx/` on your device's storage.

### How Does Trackbook Measure Distance?
Trackbook calculates the distance between markers and adds them up.

### How Does Transistor Measure Altitude?
Many devices have altitude sensors (of varying accuracy). Trackbook compares the altitude of each new marker with the previously stored altitude. The difference is added to either the uphill or downhill elevation value.

### What Does Accuracy Threshold Mean?
Every location fix, that Trackbook receives, is associated with an accuracy estimate. You can look up, how Android defines accuracy, in the [developer documentation](https://developer.android.com/reference/kotlin/android/location/Location.html#getaccuracy). `Accuracy Threshold` is the value, from which on location fixes are rejected. It can be adjusted in Trackbook's settings. You can increase the value, if your recordings tend to be incomplete. Trackbook will then also record less accurate location fixes.

## A Word on Privacy
Trackbook begins to store location data on device as soon a user presses the record button. Those recordings are stored in the directory `/Android/data/org.y20k.trackbook/files/`. They never leave the device. There is no web-service backing Trackbook.

Trackbook does not use Google Play Services to get its location data. It will however try to use data from the [NETWORK_PROVIDER](https://developer.android.com/reference/android/location/LocationManager#NETWORK_PROVIDER) on your device to augment the location data it received via GPS. The NETWORK_PROVIDER is a system-wide service, that Trackbook has no control over. This service will usually query an online database for the location of cell towers or Wi-Fi access points a device can see. You can prevent those kinds of requests on your device, if you set the location preferences system-wide to `Device Only`. Additionally Trackbook has a Setting `Restrict to GPS`

## Screenshots (v1.1)
[<img src="https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p1.png" width="240">](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p1.png)
[<img src="https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p2.png" width="240">](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p2.png)

[<img src="https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p3.png" width="240">](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p3.png)
[<img src="https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p4.png" width="240">](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/p4.png)
