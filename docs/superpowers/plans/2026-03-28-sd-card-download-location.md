# SD Card Download Location Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a download storage location preference (internal vs. SD card) to Findroid's settings UI and wire it into the download path resolution, fixing external storage download failures (issue #778) in the process.

**Architecture:** Store the user's preferred storage index as a `String?` preference (SharedPreferences, key `pref_downloads_storage_index`, null = index 0). Because the options are dynamic (storage volume labels + free space computed at runtime), add a lightweight `PreferenceDynamicSelect` model that holds a `List<Pair<String?, String>>` instead of `@ArrayRes` IDs, plus a matching composable. The `SettingsViewModel` is given `@ApplicationContext Context` to enumerate volumes via `getExternalFilesDirs(null)` + `StatFs`. `DownloaderViewModel` gains `AppPreferences` injection and reads the preference before calling `downloader.downloadItem`. `DownloaderImpl` gains a safe bounds-checked fallback so a removed SD card silently falls back to internal storage.

**Tech Stack:** Kotlin, Hilt/DI, Jetpack Compose, Android SharedPreferences, Android DownloadManager, `StatFs`, `Formatter.formatFileSize`

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| **Modify** | `settings/build.gradle.kts` | Add Hilt Gradle plugin (required for `@ApplicationContext` injection in `SettingsViewModel`) |
| **Modify** | `settings/src/main/java/dev/jdtech/jellyfin/settings/domain/AppPreferences.kt` | Add `downloadStorageIndex` preference key |
| **Create** | `settings/src/main/java/dev/jdtech/jellyfin/settings/presentation/models/PreferenceDynamicSelect.kt` | New UI model for dynamic (runtime-computed) option lists |
| **Modify** | `settings/src/main/java/dev/jdtech/jellyfin/settings/presentation/settings/SettingsViewModel.kt` | Inject Context, add storage preference UI, handle in `loadPreferences` + `onAction` |
| **Create** | `app/phone/src/main/java/dev/jdtech/jellyfin/presentation/settings/components/SettingsDynamicSelectCard.kt` | Composable for `PreferenceDynamicSelect` (reuses `SettingsSelectDialog`) |
| **Modify** | `app/phone/src/main/java/dev/jdtech/jellyfin/presentation/settings/components/SettingsGroupCard.kt` | Add `is PreferenceDynamicSelect` branch |
| **Modify** | `core/src/main/java/dev/jdtech/jellyfin/core/presentation/downloader/DownloaderViewModel.kt` | Inject `AppPreferences`, use preference as default storage index (only when no explicit index provided) |
| **Modify** | `core/src/main/java/dev/jdtech/jellyfin/utils/DownloaderImpl.kt` | Add bounds-checked fallback when building `storageLocation` |
| **Modify** | `settings/src/main/res/values/strings.xml` | Add `pref_download_storage_location`, `pref_download_internal_storage`, `pref_download_sd_card` strings |

---

## Task 0: Add Hilt Gradle plugin to `settings` module

**Files:**
- Modify: `settings/build.gradle.kts`

The `settings` module currently only has `alias(libs.plugins.ksp)`. Injecting `@ApplicationContext Context` into `SettingsViewModel` requires the Hilt Gradle plugin (which generates the `HiltViewModel` factory wiring). Without it, KSP will fail with an unresolved binding error at compile time.

- [ ] **Step 1: Add plugin**

  Open `settings/build.gradle.kts`. In the `plugins { }` block, add:
  ```kotlin
  alias(libs.plugins.hilt)
  ```
  Place it alongside the existing `ksp` plugin entry. The exact key name can be confirmed with:
  ```bash
  grep "hilt" gradle/libs.versions.toml | head -5
  ```
  It will be something like `libs.plugins.hilt` or `libs.plugins.dagger.hilt.android`.

- [ ] **Step 2: Commit**

  ```bash
  git add settings/build.gradle.kts
  git commit -m "build: add Hilt plugin to settings module for @ApplicationContext injection"
  ```

---

## Task 1: Add `downloadStorageIndex` to AppPreferences

**Files:**
- Modify: `settings/src/main/java/dev/jdtech/jellyfin/settings/domain/AppPreferences.kt:81-83`

- [ ] **Step 1: Add the preference field**

  In `AppPreferences.kt`, after the existing download preferences (after line 83 – `downloadWhenRoaming`):

  ```kotlin
  // Downloads
  val downloadOverMobileData = Preference("pref_downloads_mobile_data", false)
  val downloadWhenRoaming = Preference("pref_downloads_roaming", false)
  val downloadStorageIndex = Preference<String?>("pref_downloads_storage_index", null)
  ```

  `null` means "use index 0" (internal app-specific external storage). Any string like `"1"`, `"2"` points to that index in `getExternalFilesDirs(null)`.

- [ ] **Step 2: Commit**

  ```bash
  git add settings/src/main/java/dev/jdtech/jellyfin/settings/domain/AppPreferences.kt
  git commit -m "feat: add downloadStorageIndex preference key"
  ```

---

## Task 2: Create `PreferenceDynamicSelect` model

**Files:**
- Create: `settings/src/main/java/dev/jdtech/jellyfin/settings/presentation/models/PreferenceDynamicSelect.kt`

- [ ] **Step 1: Create the file**

  ```kotlin
  package dev.jdtech.jellyfin.settings.presentation.models

  import androidx.annotation.DrawableRes
  import androidx.annotation.StringRes
  import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend
  import dev.jdtech.jellyfin.settings.presentation.enums.DeviceType

  /**
   * Like [PreferenceSelect] but options are provided as a runtime list instead of @ArrayRes
   * IDs. Use this when the option labels must be computed at runtime (e.g. storage volumes
   * with free-space annotations).
   *
   * [dynamicOptions] is a list of (storedValue, displayLabel) pairs. The stored value is what
   * gets written to [backendPreference]; null is allowed (maps to "not set").
   */
  data class PreferenceDynamicSelect(
      @param:StringRes override val nameStringResource: Int,
      @param:StringRes override val descriptionStringRes: Int? = null,
      @param:DrawableRes override val iconDrawableId: Int? = null,
      override val enabled: Boolean = true,
      override val dependencies: List<PreferenceBackend<Boolean>> = emptyList(),
      override val supportedDeviceTypes: List<DeviceType> = listOf(DeviceType.PHONE, DeviceType.TV),
      val onUpdate: (String?) -> Unit = {},
      val backendPreference: PreferenceBackend<String?>,
      val dynamicOptions: List<Pair<String?, String>> = emptyList(),
      val value: String? = null,
  ) : Preference
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add settings/src/main/java/dev/jdtech/jellyfin/settings/presentation/models/PreferenceDynamicSelect.kt
  git commit -m "feat: add PreferenceDynamicSelect model for runtime option lists"
  ```

---

## Task 3: Add string resources

**Files:**
- Modify: `settings/src/main/res/values/strings.xml`

- [ ] **Step 1: Add strings**

  Open `settings/src/main/res/values/strings.xml` and add after the existing download strings (near `download_mobile_data` / `download_roaming`):

  ```xml
  <string name="pref_download_storage_location">Download storage location</string>
  <string name="pref_download_internal_storage">Internal storage (%s free)</string>
  <string name="pref_download_sd_card">SD card %1$d (%2$s free)</string>
  ```

  `%s` is filled with a formatted byte size from `Formatter.formatFileSize`.
  `%1$d` in the SD card label is the 1-based card number (index + 1 in `getStorageOptions()`), so users can distinguish multiple cards.

- [ ] **Step 2: Commit**

  ```bash
  git add settings/src/main/res/values/strings.xml
  git commit -m "feat: add storage location preference string resources"
  ```

---

## Task 4: Create `SettingsDynamicSelectCard` composable

**Files:**
- Create: `app/phone/src/main/java/dev/jdtech/jellyfin/presentation/settings/components/SettingsDynamicSelectCard.kt`

- [ ] **Step 1: Create the composable**

  Model it on `SettingsSelectCard.kt` but use `preference.dynamicOptions` directly (no `stringArrayResource` calls):

  ```kotlin
  package dev.jdtech.jellyfin.presentation.settings.components

  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.Spacer
  import androidx.compose.foundation.layout.height
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.layout.width
  import androidx.compose.material3.Icon
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.mutableStateOf
  import androidx.compose.runtime.remember
  import androidx.compose.runtime.setValue
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.res.painterResource
  import androidx.compose.ui.res.stringResource
  import dev.jdtech.jellyfin.core.R as CoreR
  import dev.jdtech.jellyfin.presentation.theme.spacings
  import dev.jdtech.jellyfin.settings.presentation.models.PreferenceDynamicSelect

  @Composable
  fun SettingsDynamicSelectCard(
      preference: PreferenceDynamicSelect,
      onUpdate: (value: String?) -> Unit,
      modifier: Modifier = Modifier,
  ) {
      val notSetString = stringResource(CoreR.string.not_set)
      val optionsMap = remember(preference.dynamicOptions) { preference.dynamicOptions.toMap() }
      // preference.value == null means "not yet set" → treat as "0" (internal storage)
      val displayValue = preference.value ?: "0"
      var showDialog by remember { mutableStateOf(false) }

      SettingsBaseCard(
          preference = preference,
          onClick = { showDialog = true },
          modifier = modifier,
      ) {
          Row(
              modifier = Modifier.padding(MaterialTheme.spacings.medium),
              verticalAlignment = Alignment.CenterVertically,
          ) {
              if (preference.iconDrawableId != null) {
                  Icon(
                      painter = painterResource(preference.iconDrawableId!!),
                      contentDescription = null,
                  )
                  Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
              }
              Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = stringResource(preference.nameStringResource),
                      style = MaterialTheme.typography.titleMedium,
                  )
                  Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                  Text(
                      text = optionsMap.getOrDefault(displayValue, notSetString),
                      style = MaterialTheme.typography.bodyMedium,
                  )
              }
          }
      }

      if (showDialog) {
          SettingsSelectDialog(
              // SettingsSelectDialog accepts a PreferenceSelect; we build a synthetic one
              // using the existing SettingsSelectDialog overload that takes options directly.
              preference = dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect(
                  nameStringResource = preference.nameStringResource,
                  backendPreference = preference.backendPreference,
                  options = 0,        // unused — dialog receives the list directly
                  optionValues = 0,   // unused
                  // Normalize null to "0" so the internal-storage radio button is
                  // highlighted when the preference has never been explicitly set.
                  value = preference.value ?: "0",
              ),
              options = preference.dynamicOptions,
              onUpdate = { value ->
                  showDialog = false
                  onUpdate(value)
              },
              onDismissRequest = { showDialog = false },
          )
      }
  }
  ```

  **Note:** `SettingsSelectDialog` already accepts `options: List<Pair<String?, String>>` as a separate parameter (it builds the list outside the dialog in `SettingsSelectCard`). We pass our `dynamicOptions` list directly. The `PreferenceSelect` wrapper is only needed for `SettingsSelectDialog`'s title and selected-value highlight.

- [ ] **Step 2: Compile-check approach**

  The `SettingsSelectDialog` signature is:
  ```kotlin
  fun SettingsSelectDialog(
      preference: PreferenceSelect,
      options: List<Pair<String?, String>>,
      onUpdate: (value: String?) -> Unit,
      onDismissRequest: () -> Unit,
  )
  ```
  The `options` and `optionValues` fields on the synthetic `PreferenceSelect` (set to `0`) are never read by `SettingsSelectDialog` itself — only by `SettingsSelectCard` which calls `stringArrayResource()`. So passing `0` is safe as long as we never call `stringArrayResource(0)`.

  **Alternative if 0 causes issues at compile/lint time:** extract the dialog content to a separate internal helper that takes just the `List<Pair<String?,String>>` parameter. This is a lint-safe refactor that touches only `SettingsSelectDialog.kt` — add an overload or extract the `LazyColumn` content.

- [ ] **Step 3: Commit**

  ```bash
  git add app/phone/src/main/java/dev/jdtech/jellyfin/presentation/settings/components/SettingsDynamicSelectCard.kt
  git commit -m "feat: add SettingsDynamicSelectCard composable for runtime option lists"
  ```

---

## Task 5: Wire `PreferenceDynamicSelect` into `SettingsGroupCard`

**Files:**
- Modify: `app/phone/src/main/java/dev/jdtech/jellyfin/presentation/settings/components/SettingsGroupCard.kt`

- [ ] **Step 1: Add import and branch**

  1. Add import at the top (after the `PreferenceSelect` import):
     ```kotlin
     import dev.jdtech.jellyfin.settings.presentation.models.PreferenceDynamicSelect
     ```

  2. In the `when (preference)` block inside `SettingsGroupCard` (after the `is PreferenceSelect ->` branch, around line 81), add:
     ```kotlin
     is PreferenceDynamicSelect ->
         SettingsDynamicSelectCard(
             preference = preference,
             onUpdate = { value ->
                 onAction(SettingsAction.OnUpdate(preference.copy(value = value)))
                 preference.onUpdate(value)
             },
             modifier = Modifier.fillMaxWidth(),
         )
     ```

- [ ] **Step 2: Commit**

  ```bash
  git add app/phone/src/main/java/dev/jdtech/jellyfin/presentation/settings/components/SettingsGroupCard.kt
  git commit -m "feat: handle PreferenceDynamicSelect in SettingsGroupCard"
  ```

---

## Task 6: Update `SettingsViewModel` — add Context, storage helpers, preference UI, and handlers

**Files:**
- Modify: `settings/src/main/java/dev/jdtech/jellyfin/settings/presentation/settings/SettingsViewModel.kt`

This is the largest task. Break it into sub-steps.

- [ ] **Step 1: Inject `@ApplicationContext Context`**

  Change the class declaration from:
  ```kotlin
  @HiltViewModel
  class SettingsViewModel @Inject constructor(private val appPreferences: AppPreferences) :
      ViewModel() {
  ```
  to:
  ```kotlin
  @HiltViewModel
  class SettingsViewModel @Inject constructor(
      @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
      private val appPreferences: AppPreferences,
  ) : ViewModel() {
  ```

  Also add import at top of file:
  ```kotlin
  import android.content.Context
  import android.os.StatFs
  import android.text.format.Formatter
  import dagger.hilt.android.qualifiers.ApplicationContext
  ```

- [ ] **Step 2: Add `getStorageOptions()` helper function**

  Add a private function after the `topLevelPreferences` block (before `loadPreferences`):

  ```kotlin
  private fun getStorageOptions(): List<Pair<String?, String>> {
      val dirs = context.getExternalFilesDirs(null)
      return dirs.mapIndexedNotNull { index, dir ->
          if (dir == null) return@mapIndexedNotNull null
          val free = try {
              Formatter.formatFileSize(context, StatFs(dir.path).availableBytes)
          } catch (_: Exception) {
              return@mapIndexedNotNull null
          }
          val label = if (index == 0) {
              context.getString(R.string.pref_download_internal_storage, free)
          } else {
              // index + 1 for 1-based display ("SD card 1", "SD card 2", ...)
              context.getString(R.string.pref_download_sd_card, index + 1, free)
          }
          Pair(index.toString(), label)
      }
  }
  ```

- [ ] **Step 3: Add `PreferenceDynamicSelect` to the Downloads section in `topLevelPreferences`**

  In `topLevelPreferences`, find the Downloads `PreferenceCategory`'s `nestedPreferenceGroups`. It currently has one `PreferenceGroup` with two `PreferenceSwitch` items. Add a second `PreferenceGroup` before or after with the storage select:

  ```kotlin
  nestedPreferenceGroups =
      listOf(
          PreferenceGroup(
              preferences =
                  listOf(
                      PreferenceSwitch(
                          nameStringResource = R.string.download_mobile_data,
                          supportedDeviceTypes = listOf(DeviceType.PHONE),
                          backendPreference = appPreferences.downloadOverMobileData,
                      ),
                      PreferenceSwitch(
                          nameStringResource = R.string.download_roaming,
                          dependencies = listOf(appPreferences.downloadOverMobileData),
                          supportedDeviceTypes = listOf(DeviceType.PHONE),
                          backendPreference = appPreferences.downloadWhenRoaming,
                      ),
                  )
          ),
          PreferenceGroup(
              preferences =
                  listOf(
                      PreferenceDynamicSelect(
                          nameStringResource = R.string.pref_download_storage_location,
                          iconDrawableId = R.drawable.ic_hard_drive,
                          supportedDeviceTypes = listOf(DeviceType.PHONE),
                          backendPreference = appPreferences.downloadStorageIndex,
                          dynamicOptions = emptyList(), // filled in loadPreferences
                      ),
                  )
          ),
      ),
  ```

  **Note on icon:** `R.drawable.ic_hard_drive` exists in the `settings` module (confirmed — used by Cache settings) and is the closest semantic match.

  Add import at top of `SettingsViewModel.kt`:
  ```kotlin
  import dev.jdtech.jellyfin.settings.presentation.models.PreferenceDynamicSelect
  ```

- [ ] **Step 4: Handle `PreferenceDynamicSelect` in `loadPreferences`**

  In the `when (preference)` map block inside `loadPreferences`, add after the `is PreferenceLongInput -> { ... }` branch (before `else -> preference`):

  ```kotlin
  is PreferenceDynamicSelect -> {
      preference.copy(
          enabled = preference.enabled &&
              preference.dependencies.all { appPreferences.getValue(it) },
          dynamicOptions = getStorageOptions(),
          value = appPreferences.getValue(preference.backendPreference),
      )
  }
  ```

- [ ] **Step 5: Handle `PreferenceDynamicSelect` in `onAction`**

  In `onAction`, in the `when (action.preference)` block, add after `is PreferenceLongInput -> ...`:

  ```kotlin
  is PreferenceDynamicSelect ->
      appPreferences.setValue(
          action.preference.backendPreference,
          action.preference.value,
      )
  ```

- [ ] **Step 6: Commit**

  ```bash
  git add settings/src/main/java/dev/jdtech/jellyfin/settings/presentation/settings/SettingsViewModel.kt
  git commit -m "feat: add download storage location preference to settings UI"
  ```

---

## Task 7: Update `DownloaderViewModel` to read storage preference

**Files:**
- Modify: `core/src/main/java/dev/jdtech/jellyfin/core/presentation/downloader/DownloaderViewModel.kt`

- [ ] **Step 1: Inject `AppPreferences`**

  Change:
  ```kotlin
  @HiltViewModel
  class DownloaderViewModel @Inject constructor(private val downloader: Downloader) : ViewModel() {
  ```
  to:
  ```kotlin
  @HiltViewModel
  class DownloaderViewModel @Inject constructor(
      private val downloader: Downloader,
      private val appPreferences: dev.jdtech.jellyfin.settings.domain.AppPreferences,
  ) : ViewModel() {
  ```

  Add import at top:
  ```kotlin
  import dev.jdtech.jellyfin.settings.domain.AppPreferences
  ```

- [ ] **Step 2: Read preference in `download()`**

  The preference acts as a **global default** — it only applies when the caller hasn't explicitly specified a storage index (i.e., `storageIndex == 0`, which is the default). This preserves any per-download storage selection dialogs in the UI.

  Change the private `download` function body:
  ```kotlin
  private fun download(item: FindroidItem, storageIndex: Int = 0) {
      // Use the per-call storageIndex if explicitly non-zero; otherwise fall back to the
      // global preference (which itself defaults to 0 = internal storage).
      val resolvedIndex = storageIndex.takeIf { it != 0 }
          ?: appPreferences.getValue(appPreferences.downloadStorageIndex)?.toIntOrNull()
          ?: 0
  ```

  Then pass `resolvedIndex` to `downloader.downloadItem`:
  ```kotlin
  val (downloadId, uiText) =
      downloader.downloadItem(
          item = item,
          sourceId = item.sources.first().id,
          storageIndex = resolvedIndex,
      )
  ```

  This means:
  - If an explicit non-zero storageIndex was passed (e.g., from a per-download dialog), that wins.
  - Otherwise, use the global settings preference (default: 0 = internal storage).

- [ ] **Step 3: Commit**

  ```bash
  git add core/src/main/java/dev/jdtech/jellyfin/core/presentation/downloader/DownloaderViewModel.kt
  git commit -m "feat: DownloaderViewModel reads storage index preference before download"
  ```

---

## Task 8: Harden `DownloaderImpl` — safe fallback for removed SD card

**Files:**
- Modify: `core/src/main/java/dev/jdtech/jellyfin/utils/DownloaderImpl.kt`

- [ ] **Step 1: Replace storage resolution in `downloadItem`**

  In `downloadItem`, replace lines 69–78 (the current `storageLocation` assignment + null/state check) with:

  ```kotlin
  val dirs = context.getExternalFilesDirs(null)
  val storageLocation = run {
      // Try requested index first; silently fall back to index 0 if unavailable.
      val requested = dirs.getOrNull(storageIndex)
      if (
          requested != null &&
          Environment.getExternalStorageState(requested) == Environment.MEDIA_MOUNTED
      ) {
          requested
      } else {
          dirs.getOrNull(0)?.takeIf {
              Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
          }
      }
  } ?: return@coroutineScope Pair(
      -1,
      UiText.StringResource(CoreR.string.storage_unavailable),
  )
  ```

  This silently falls back from SD card to internal storage when the SD card is removed, and only returns an error when even internal storage is unavailable.

- [ ] **Step 2: Apply same fix to `downloadExternalMediaStreams`**

  In `downloadExternalMediaStreams` (around line 255), replace:
  ```kotlin
  val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
  ```
  with:
  ```kotlin
  val dirs = context.getExternalFilesDirs(null)
  val storageLocation = dirs.getOrNull(storageIndex)
      ?.takeIf { Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED }
      ?: dirs.getOrNull(0)
      ?: return
  ```

  Add required import if not already present (it should already be there from existing code):
  ```kotlin
  import android.os.Environment
  ```

- [ ] **Step 3: Commit**

  ```bash
  git add core/src/main/java/dev/jdtech/jellyfin/utils/DownloaderImpl.kt
  git commit -m "fix: DownloaderImpl falls back to internal storage if SD card unavailable (#778)"
  ```

---

## Task 9: Verify `SettingsDynamicSelectCard` compiles cleanly

- [ ] **Step 1: Confirm `SettingsSelectDialog` doesn't read `options`/`optionValues` from the preference object**

  The synthetic `PreferenceSelect` in `SettingsDynamicSelectCard` passes `options = 0` and `optionValues = 0`. `PreferenceSelect.options` and `.optionValues` are plain `Int` fields with no `@ArrayRes` annotation, so no lint errors. `SettingsSelectDialog` receives the list as its own `options: List<Pair<String?,String>>` parameter and never calls `stringArrayResource(preference.options)`.

  Sanity check:
  ```bash
  grep "stringArrayResource" app/phone/src/main/java/dev/jdtech/jellyfin/presentation/settings/components/SettingsSelectDialog.kt
  ```
  Expected: no matches.

- [ ] **Step 2: If the grep shows unexpected `stringArrayResource` usage inside `SettingsSelectDialog`**

  Add an internal overload of `SettingsSelectDialog` that takes `title: String` and `options: List<Pair<String?,String>>` directly (no `PreferenceSelect` wrapper), and call that from `SettingsDynamicSelectCard` instead.

---

## Task 10: Build debug APK

- [ ] **Step 1: Set up Android SDK path**

  Check for SDK:
  ```bash
  ls ~/Android/Sdk/platforms 2>/dev/null || echo "SDK not found at ~/Android/Sdk"
  ```

  If not found, the Android SDK must be installed before building. If running in a CI environment with the SDK available, create `local.properties`:
  ```bash
  echo "sdk.dir=/path/to/your/android/sdk" > local.properties
  ```
  Common SDK locations:
  - Linux: `~/Android/Sdk`
  - WSL: `/mnt/c/Users/<user>/AppData/Local/Android/Sdk` (may not work; prefer native Linux SDK)
  - CI: `$ANDROID_HOME`

- [ ] **Step 2: Run the build**

  ```bash
  ./gradlew :app:phone:assembleDebug 2>&1 | tee build.log
  ```

  (The multi-module structure means the app module is at `app/phone`.)

- [ ] **Step 3: Handle common build failures**

  - **`sdk.dir` not set**: Create `local.properties` with correct path.
  - **`@ArrayRes` lint error on `options = 0`**: See Task 9, Step 2.
  - **`@StringRes` error on new strings not found**: Ensure `strings.xml` additions from Task 3 are correct.
  - **Unresolved reference `PreferenceDynamicSelect` in `SettingsGroupCard`**: Ensure import added in Task 5.
  - **Hilt error about missing binding for `AppPreferences` in `DownloaderViewModel`**: `AppPreferences` is already provided via `AppPreferencesModule` in `core` — no new DI module needed.

- [ ] **Step 4: Report APK path**

  On success the APK is at:
  ```
  app/phone/build/outputs/apk/debug/phone-debug.apk
  ```
  (Confirm with `ls app/phone/build/outputs/apk/debug/`)

---

## Task 11: Final commit summary

- [ ] **Step 1: Verify all changes are committed**

  ```bash
  git status
  git log --oneline -10
  ```

- [ ] **Step 2: Create summary commit if any stragglers**

  ```bash
  git add -A
  git commit -m "feat: add SD card download location selection, fix external storage downloads"
  ```

---

## Edge Cases & Constraints Checklist

- [ ] No new permissions added to `AndroidManifest.xml` — `getExternalFilesDirs()` paths are pre-granted
- [ ] Preference UI shows only internal storage when no SD card present (single entry in `getStorageOptions()`)
- [ ] Removed SD card falls back silently to internal storage (`DownloaderImpl` Task 8)
- [ ] Download file naming/UUID scheme unchanged — only `storageLocation` (base dir) changes
- [ ] TV device type excluded (`supportedDeviceTypes = listOf(DeviceType.PHONE)`) so TV UI is unaffected
