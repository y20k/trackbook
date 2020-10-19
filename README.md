# Trackbook - Android Movement Recorder _(trackbook)_

![](https://raw.githubusercontent.com/y20k/trackbook/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png)

![GitHub](https://img.shields.io/github/license/y20k/trackbook?style=plastic)
[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=plastic)](https://github.com/RichardLitt/standard-readme)

Trackbook is a bare-bones app for recording your movements.

## Table of Contents
* [Background](#background)
* [Install](#install)
* [Usage](#usage)
  * [Start Recording via Quick Settings Tile](#start-recording-via-quick-settings-tile)
  * [Save Recording as GPX](#save-recordings-as-gpx)
  * [Copy GPX Files Manually](#copy-gpx-files-manually)
  * [How does Trackbook measure distance?](#how-does-trackbook-measure-distance)
  * [How does Trackbook measure altitude](#how-does-trackbook-measure-altitude)
  * [What does "accuracy threshold" mean?](#what-does-accuracy-threshold-mean)
* [Where Are My Old Recordings?](#where-are-my-old-recordings)
* [A Word On Privacy](#a-word-on-privacy)
* [Contributing](#contributing)
  * [Report a Bug or Suggest a New Feature](#report-a-bug-or-suggest-a-new-feature)
  * [Help with Translations](#help-with-translations)
  * [Submit Your Own Solutions](#submit-your-own-solutions)
  * [Suggested Issues to Tackle](#suggested-issues-to-tackle)
* [License](#license)

## Background 
**Version 2.0.x ("Echoes")**

Trackbook is a bare-bones app for recording your movements.
Trackbook is great for hiking, vacationing, or working out.
Once started, it traces your movements on a map.
The map data is provided by [OpenStreetMap (OSM)](https://www.openstreetmap.org/).

Trackbook is free software.
It is published under the [MIT open-source license](https://opensource.org/licenses/MIT).
Trackbook uses [osmdroid](https://github.com/osmdroid/osmdroid) to display the map, which is also free software
published under the [Apache License](https://github.com/osmdroid/osmdroid/blob/master/LICENSE).
Want to help?
Please check out the notes in [CONTRIBUTE.md](https://github.com/y20k/trackbook/blob/master/CONTRIBUTE.md) first.

![](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/01-map-recording-active.png)
![](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/02-map-context-menu.png)

![](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/03-track-list.png)
![](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/04-track.png)

![](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/05-settings.png)
![](https://raw.githubusercontent.com/y20k/trackbook/master/metadata/en-US/phoneScreenshots/06-quick-settings-tile.png)

## Install
You can install it via Google Play and F-Droid - or you can go and grab the latest APK on
[GitHub](https://github.com/y20k/trackbook/releases).

[![](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=org.y20k.trackbook)
[![](https://fdroid.gitlab.io/artwork/badge/get-it-on.png)](https://f-droid.org/packages/org.y20k.trackbook/)

## Usage

### Start Recording via Quick Settings Tile
![](https://user-images.githubusercontent.com/9103935/74753187-09a75f00-5270-11ea-82de-18c5b8737e2b.png)

You can start a recording without opening Trackbook.
Just pull down the System's Quick Settings and tap on the "Start Recording" tile.
You'll need to manually add Trackbook's Recording tile to Quick Settings first.
You can find information on customizing Quick Settings [here](https://support.google.com/android/answer/9083864) and [here](https://www.xda-developers.com/get-custom-quick-settings-tiles/).

### Save Recordings as GPX
Recordings can be exported as GPX ([GPS Exchange Format](https://en.wikipedia.org/wiki/GPS_Exchange_Format)).
Tap on the save button in the lower-right corner of a previously recorded track.

### Copy GPX Files Manually
Trackbook automatically generates GPX files for every recording.
You can find them in the folder `/Android/data/org.y20k.trackbook/files/gpx/` on your device's storage.

### How does Trackbook measure distance?
Trackbook calculates the distance between markers and adds them up.

### How does Trackbook measure altitude?
Many devices have altitude sensors (of varying accuracy).
Trackbook compares the altitude of each new marker with the previously stored altitude.
The difference is added to either the uphill or downhill elevation value.

### What does "accuracy threshold" mean?
Every location fix that Trackbook receives is associated with an accuracy estimate.
You can look up how Android defines accuracy in the [developer documentation](https://developer.android.com/reference/kotlin/android/location/Location.html#getaccuracy).
`Accuracy Threshold` is the value from which location fixes are rejected.
It can be adjusted in Trackbook's settings.
You can increase the value if your recordings tend to be incomplete.
Trackbook will then also record less accurate location fixes.

## Where Are My Old Recordings?
The F-Droid version of Trackbook features an auto-importer for old recordings.
Sadly I was not able to implement the auto-importer for the Play Store version of Trackbook due to SDK requirements / restrictions.
That is partly my fault and I am very sorry.
There is a (quite complicated) solution to get back your old recordings.
Please head over to the [Wiki](https://github.com/y20k/trackbook/wiki) to find out how.

## A Word On Privacy
Trackbook begins to store location data on a device as soon as a user presses the record button.
Those recordings are stored in the directory `/Android/data/org.y20k.trackbook/files/`.
They never leave the device.
There is no web-service backing Trackbook.

Trackbook does not use Google Play Services to get its location data.
It will, however, try to use data from the [NETWORK_PROVIDER](https://developer.android.com/reference/android/location/LocationManager#NETWORK_PROVIDER) on your device to augment the location data it received via GPS.
The NETWORK_PROVIDER is a system-wide service that Trackbook has no control over.
This service will usually query an online database for the location of cell towers or Wi-Fi access points a device can see.
You can prevent those kinds of requests on your device if you set the location preferences system-wide to `Device Only`.
Additionally, Trackbook offers a `Restrict to GPS` setting that deactivates the NETWORK_PROVIDER just within the app.

## Thanks
Contributors - like the main translators for a certain language - are listed as co-authors of this project in
[AUTHORS.md](https://github.com/y20k/trackbook/blob/master/AUTHORS.md).
Bonus: If you are on this list, you are automatically eligible for a free German beverage (to be redeemed in Stuttgart).

## Contributing

### Report a Bug or Suggest a New Feature
Bugs and new features are being discussed on the GitHub [Issue Tracker](https://github.com/y20k/trackbook/issues).
The issue "[Previous discussions and feature requests](https://github.com/y20k/trackbook/issues/57)" lists some of the
features that were rejected - either because they did not fit conceptually or because I could not figure out how to
implement them.

### Help With Translations
The translations are managed on [Weblate](https://hosted.weblate.org/projects/trackbook/strings/).
Help is much appreciated.

### Submit Your Own Solutions
Help is very welcome, be it in the form of code, artwork, enhancements to the website, tutorial videos, or whatever.
**But please** suggest new features or enhancements in advance on the
[Issue Tracker](https://github.com/y20k/trackbook/issues) before implementing them.

### Suggested Issues to Tackle
[#19](https://github.com/y20k/trackbook/issues/19)

## License

The MIT License (MIT)

Copyright (c) 2016-20 - Y20K.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
