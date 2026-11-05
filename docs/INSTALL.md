# Install and update

## Fresh installation on a supported Odin 2 or Odin 3

1. Download the official `Raven-Gamepad-0.16.4.apk` from [Releases](https://github.com/Rsotbiemrptson/Raven-Gamepad/releases/tag/v0.16.4).
2. Open the APK and allow installation from that source when Android asks.
3. Open Raven Gamepad. Use the top-right three-dot menu to open **Settings**.
4. Select **Bridge controls**, expand **Advanced**, then tap **Install/Update Components**. Wait for the installation to finish.
5. Tap **Start** and wait for **Status: Running**.
6. Select **Apps**, check PS Remote Play or your other installed target apps, and tap **Save selected apps**. Search accepts names or package IDs. **Show system apps** is in the three-dot menu. Remote Play is not preselected or shown if it is absent.
7. Fully close and reopen the target app, connect to your console if needed, and test all buttons, both sticks, D-pad and triggers in a game.
8. Optional: under **Settings > Bridge controls**, enable **Start bridge after reboot**. Reboot, unlock once, and test without first opening Raven Gamepad.

Normal setup does not need USB debugging, a computer, a separate script kit or a firmware flash. The device must expose the supported OEM privileged interface described in the README; allowing APK installation does not itself grant that access.

## Update an existing official installation

Install the new APK over DS4 Bridge/Raven Gamepad. The package ID remains `com.odin2.ds4bridgehelper`, and the official signing identity is unchanged. Keep the old app installed so Android retains its app choices, mappings, theme and startup option.

**After updating:** open Settings > Bridge controls > Advanced > **Install/Update Components**, then Start. This refreshes the launcher and watcher for Odin 2/Android 13 and Odin 3/Android 15, including the fallback for firmware without Android's `content` command. Replacing the APK alone does not update installed runtime scripts. Saved app selections, mappings, theme and startup option are retained. No firmware flash or factory reset is needed. Quick Settings stock-mode switching remains Odin 2 only.

To add the tile, expand Android Quick Settings, tap its edit/pencil button, and add **Raven controller style** (shown as **Raven Gamepad** once active). Tap to cycle modes; see [Quick Settings controller style](USAGE.md#quick-settings-controller-style).

Community builds signed with another key cannot update the official app in place. Complete component removal and uninstall before switching signing identities; uninstalling loses app-private settings.

## Older firmware/script integrations

The component controls manage this standalone app's runtime. They do not remove an older firmware-baked bridge or another script installation. Do not run two bridges simultaneously. Firmware bridge detection disables standalone controls; migrate that integration separately before proceeding.
