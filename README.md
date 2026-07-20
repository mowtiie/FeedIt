<div align="center">

<img width="" src="metadata/en-US/images/icon.png"  width=160 height=160  align="center">

# FeedIt

**Your feeds. Read your way.**

FeedIt is a simple, open-source RSS and Atom feed reader for Android. No account, no server of ours, no tracking — feeds are fetched directly from their own source, and everything else.

</div>

## Features

* **Free and Open Source:** Enjoy complete transparency and community-driven development.
* **No Account, No Tracking:** There's no login, no analytics, and nothing phoning home beyond fetching the feeds you actually subscribed to. Everything you read, star, and organize stays in a local database on your device.
* **Feed Management:** Add a feed by URL — with auto-discovery, so pasting a site's homepage usually finds its feed for you — organize with freeform tags, and set each feed to open articles in-app or in your browser.
* **Multiple Article Layouts:** List, Compact, Card, or Magazine — pick the density and style that actually fits how you read, from a quick headline scan to a full-image-forward view.
* **Search, Filter & Sort:** Full-text search, a Read/Unread checkbox filter that remembers your choice, and sorting by newest, oldest, unread-first, or by feed.
* **Background Sync, On Your Terms:** Choose a sync interval from 15 minutes to 12 hours, restrict it to Wi-Fi, or just pull to refresh manually — feeds are fetched concurrently, so one slow server doesn't hold up the rest.
* **Notifications:** Per-feed toggles for new-article alerts, with their own searchable and sortable management screen, plus a manual-sync summary and a single master switch that overrides everything if you just want quiet.
* **Quick Actions:** Long-press an article for a bottom sheet context menu — mark read/unread, star, share, or open in your browser — without leaving the list.
* **Appearance:** Choose System, Light, or Dark theme; boost contrast for readability; and opt into Material You dynamic color that matches your device's wallpaper.
* **OPML Import & Export:** Bring your subscriptions in from another reader, or take them with you if you ever want to leave.
* **Crash Reports:** If the app encounters an unexpected error, a dialog on the next launch lets you copy the technical details to your clipboard or save them to a file — nothing from your feeds or reading history is ever included, and nothing is sent automatically.

## Screenshots

<div align="center">
	<div>
	  <img src="metadata/en-US/images/screenshots/screenshot_1.jpg" width="30%" />
    <img src="metadata/en-US/images/screenshots/screenshot_2.jpg" width="30%" />
    <img src="metadata/en-US/images/screenshots/screenshot_3.jpg" width="30%" />
	  <img src="metadata/en-US/images/screenshots/screenshot_4.jpg" width="30%" />
    <img src="metadata/en-US/images/screenshots/screenshot_5.jpg" width="30%" />
    <img src="metadata/en-US/images/screenshots/screenshot_6.jpg" width="30%" />
	</div>
</div>

## Verification

APK releases on GitHub are signed using my key. They can
be verified using
[apksigner](https://developer.android.com/studio/command-line/apksigner.html#options-verify):

```
apksigner verify --print-certs --verbose feedit.apk
```

The output should look like:

```
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v3.1 scheme (APK Signature Scheme v3.1): false
Verified using v3.2 scheme (APK Signature Scheme v3.2): false
Verified using v4 scheme (APK Signature Scheme v4): false
```

The certificate fingerprints should correspond to the ones listed below:

```
Owner: CN=Mowtiie
Issuer: CN=Mowtiie
Serial number: 8a256fdcdde50069
Valid from: Wed Jun 10 22:57:23 PST 2026 until: Sun Oct 26 22:57:23 PST 2053
Certificate fingerprints:
         SHA1: 56:4E:2C:DB:E4:06:C9:EC:15:E6:BC:D9:0A:88:38:72:8B:FB:13:20
         SHA256: 8B:67:51:F3:C3:31:85:63:5F:98:95:30:B6:C0:73:A1:39:7B:3D:41:2B:EF:AE:69:06:A2:EB:58:45:D2:DE:63
```

**Warning:** Only install FeedIt APKs signed with the key above. Verifying the signature confirms you're running a genuine, unmodified build.

### PGP Signing

As an additional layer on top of the Android signature above, each release is also signed with my PGP key. While `apksigner` confirms the APK itself is intact, a PGP signature confirms that *I* am the one who published this specific file to GitHub — an independent check that doesn't rely on GitHub's account security alone.

**Public key fingerprint:**
```
9EA2 8F46 7802 5092 7643 1B69 42B5 FA42 AA63 90E1
```

Download and import the key from my website, or directly from this repo:

```
curl -O https://mowtiie.cc/PGP_PUBLIC_KEY.asc
gpg --import PGP_PUBLIC_KEY.asc
```

After importing, confirm the fingerprint printed by GPG matches the one listed above — that match is what actually establishes trust, not the import step itself. Fetching the key over HTTPS from a domain you already trust is arguably a stronger anchor than a keyserver, since keyservers accept uploads from anyone and don't vouch for identity.

Each release includes a detached `.asc` signature alongside the APK. Verify a downloaded release with:

```
gpg --verify feedit-vX.X.apk.asc feedit-vX.X.apk
```

A valid signature looks like:

```
Good signature from "Mowtiie <mowtiie.dev@gmail.com>"
```

**Note:** PGP signing is a supplementary trust measure, not a substitute for the `apksigner` check above — verify both if you want the highest confidence that a release is genuine and unmodified.

## Mapping Files

Each release on GitHub includes a `mapping-<version>.txt` file alongside the APK. This file is needed to deobfuscate stack traces from crash reports — match the file to the version shown in the crash report header and use it with `retrace` from the Android SDK.

## Contributing

Issues and pull requests are welcome. If you're filing a bug, please include your Android version and the steps to reproduce.

## License

This project is licensed under the GNU General Public License v3.0. See the
[LICENSE](LICENSE) file for details.
