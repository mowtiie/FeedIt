# FeedIt

Android RSS/Atom reader. Local-only: no account, no backend, no analytics. Feeds are fetched directly from their sources. Data lives in on-device SQLite. License: GPL-3.0.

## Tech stack and constraints
- Java 11 only. Never add Kotlin source files.
- Package `com.mowtiie.feedit`. minSdk 29, targetSdk/compileSdk 36, versionName 1.1.
- XML layouts + ViewBinding (`buildConfig` also enabled).
- SQLite via hand-written DAOs (`DatabaseHelper`, `*Dao`). No Room. Schema changes go through `DatabaseHelper`.
- WorkManager for sync, Glide for images, Material 3 (DayNight, NoActionBar).
- Pure Activities. Never introduce Fragments. All Activities extend `FeedItActivity`.
- MVVM: `AndroidViewModel` + `LiveData`. No Kotlin coroutines/Flow.
- Dependencies are in `gradle/libs.versions.toml`. Add new ones there, not inline.

## Build and run
Use the wrapper (`./gradlew` on bash, `.\gradlew.bat` on PowerShell):
- `assembleDebug`: debug APK. applicationId is `com.mowtiie.feedit.debug` (versionName suffix `-debug`, separate launcher icon).
- `testDebugUnitTest`: unit tests (only the template `ExampleUnitTest` exists; no real tests yet).
- `lintDebug` / `lint`: Android lint.
- `assembleRelease`: needs `keystore.properties` in the repo root (gitignored; keys `keyAlias`, `keyPassword`, `storeFile`, `storePassword`). Without it the release signing config is skipped, so debug builds still work. Release has R8 minify + shrinkResources on. Do not commit the file.
- Debug logs: `adb logcat -s FeedItSync` (sync pipeline).

## Architecture map (`app/src/main/java/com/mowtiie/feedit/`)
```
FeedItApp          Application: crash handler, notification channel, schedules sync
data/              DatabaseHelper, ArticleDao, FeedDao, TagDao, FeedRepository, RepositoryCallback
model/             Article, Feed, Tag, FeedTags (feed + its tags)
net/               FeedFetcher, FetchResult, FeedAutoDiscovery
parser/            FeedParser (RSS/Atom), OPML parser/writer, DateUtils, Parsed* DTOs
sync/              SyncWorker, SyncScheduler, SyncLog, NotificationHelper, NotificationActionReceiver
ui/activities/     Main, FeedArticles, ArticleDetail, AddEditFeed, FeedManagement, TagManagement,
                   FeedNotificationSettings, Settings, About, base FeedItActivity
ui/adapters/       ArticleAdapter (list/card/magazine holders), Feed/Tag/FeedNotification adapters
ui/viewmodel/      One ViewModel per screen
util/              ArticleActionsBottomSheet, ArticleUiState, InsetsUtil, PrefsKeys, ThemeManager,
                   BatteryOptimizationHelper
crash/             CrashHandler, CrashReporter
```
- `FeedRepository` is a singleton with a single-thread executor. This is intentional: it gives FIFO ordering of reads and writes. Never swap it for a pool.
- Data access goes through `FeedRepository` (activities/ViewModels only reference DAO types for things like `ArticleDao.SortOrder`).

## Conventions and gotchas

### Parsing (`parser/FeedParser`)
- Parsing loops track `depth` over `XmlPullParser`.
- `readText()` calls `parser.nextTag()` and consumes the element's closing tag. Every branch that calls `readText()` MUST `depth--` afterward. Missing it desyncs the loop.
- Never call `parser.next()` directly inside loops. Use `nextEvent()`, which throws on `END_DOCUMENT` instead of spinning forever on truncated XML.
- YouTube Atom entries nest `media:thumbnail`/`media:content` inside `media:group`; the Atom entry parser handles `group` with its own inner loop.
- `decodePlainTextEntities` applies to titles only. Never apply it to summary/content: those are stored as raw HTML.

### Sync (`sync/`)
- `SyncWorker` fetches feeds on a 4-thread pool inside one `doWork()`. Optional input `KEY_FEED_ID` scopes a run to one feed.
- Unique work names (in `SyncScheduler`): periodic (`feedit_periodic_sync`), manual all-feeds (`feedit_manual_sync`), per-feed (`feedSyncWorkName(feedId)`).
- Log via `SyncLog` (tag `FeedItSync`), not raw `Log`.

### Notifications
- One notification per new article (title = feed name, body = article title), plus a group summary.
- Actions (Mark read, Open) are handled by `NotificationActionReceiver`. Open respects the feed's open mode (in-app `ArticleDetailActivity` vs browser `ACTION_VIEW`).
- PendingIntent request codes: `articleId*2` (mark read) and `articleId*2+1` (open). Keep them distinct or intents collide.
- Notification icons must be monochrome. Action icons need their own drawables (`ic_action_mark_read`, `ic_action_open`); don't reuse UI icons.
- `ic_stat_feedit` (anydpi) currently has `android:tint="#FFFFFF"`. Leave it unless you've verified rendering; don't add tint to other notification icons.

### Article list
- `MainViewModel.matchesCurrentFilter()` MUST mirror the filter logic in `ArticleDao.getArticles()` (read/unread, starred). Optimistic updates rebuild from a cached list, so a mismatch makes items appear/disappear until the next reload. Search is handled in the DAO query only.
- Read/unread filter (`PrefsKeys.SHOW_READ` / `SHOW_UNREAD`) and sort order persist in SharedPreferences. Sort order key is `"sort_order"` (`PrefsKeys.SORT_ORDER` exists, but `MainViewModel` uses its own identical constant).
- No ActionMode or multi-select. Long-press opens `ArticleActionsBottomSheet`.

### Layout and insets
- Every `AppBarLayout` needs `android:fitsSystemWindows="true"`. `CollapsingToolbarLayout` (ArticleDetailActivity) needs it on itself too.
- Call `EdgeToEdge.enable(this)` in each Activity's `onCreate`, then `InsetsUtil.applyEdgeToEdgeInsets(appBar, content)`. Do not add manual window-inset listeners on the app bar.
- Toolbars use `Widget.FeedIt.Toolbar` (title font via `TextAppearance.FeedIt.ToolbarTitle`). `CollapsingToolbarLayout` needs its own `expandedTitleTextAppearance` and `collapsedTitleTextAppearance`.

### Thumbnails (`ArticleAdapter.bindThumbnail`)
- Three states: image URL present → Glide with `.placeholder()` (loading) / `.error()` / `.centerCrop()`; no URL → clear drawable and set `android:background` placeholder.
- `android:background` stretches; Glide `.placeholder()` goes through centerCrop. Check both paths before changing placeholder drawables.

### Resources
- All user-visible strings go in `res/values/strings.xml` (links in `links.xml`). After edits, check for duplicate resource names.

### ProGuard (`app/proguard-rules.pro`)
- `ArticleDao.SortOrder` is persisted by `name()`, so it's `@Keep`. Never rename or remove enum constants without a migration.
- `SyncWorker` has a keep rule. `SourceFile`/`LineNumberTable` are kept so crash reports stay readable.

### Crash reporting
- `crash/` writes `last_crash.txt` and shows a dialog on next launch. Nothing is sent over the network. Keep it that way.

### Network and backup
- Cleartext HTTP is blocked (no network security config); `FeedFetcher` returns a clear error for http redirects. Feed URLs default to `https://`.
- Backup rules (`data_extraction_rules.xml`, `backup_rules.xml`) include only `feedit.db` and shared prefs. `last_crash.txt` is not backed up.
- No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission (Play policy); the battery setting opens the system optimization list.

### Tags
- The UI no longer uses tag colors, but the `color` column (`DatabaseHelper`) and `Tag.color` still exist. Don't drop them without a DB migration.

## Workflow rules
- After editing XML, validate it is well-formed.
- After Java edits, grep for stale references to anything removed or renamed.
- When changing a method signature, grep all call sites.
- Never claim something compiles or passes without running the build (`assembleDebug`) when a build is possible.
- Keep comment density and naming consistent with surrounding code.

## Known TODOs and placeholders
- `TagManagementActivity` (~line 122): TODO to surface a Snackbar/Toast on error.
- `res/values/links.xml`: `url_support` is `https://ko-fi/mowtiie`, which is not a valid ko-fi URL (likely missing `.com`).
- `strings.xml` `preference_app_source_code_summary` mentions "the README", but the repo has no README.
- `app_author` = `mowtiie`; `url_source_code` = `https://github.com/mowtiie/feedit`. Verify these are final.
- `assets/license.txt` and `LICENSE` are the full GPLv3 text with no project-specific copyright line; `app_license` string says "GNU General Public License V3.0".
- Tests: only template `ExampleUnitTest` / `ExampleInstrumentedTest`.
