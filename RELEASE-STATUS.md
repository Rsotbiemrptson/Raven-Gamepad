# Release status

Current final: **Raven Gamepad 0.17.2**, version code **56**, dated 2026-11-07.

Odin 2 / Android 13 / Touch Mapping 2.0(20240724_155618) and Odin 3 / Android 15 / Touch Mapping 3.0(20260429_153600) passed Remote Play gameplay and automatic startup after reboot/first unlock. The user confirmed controls before and after reboot on both for 0.16.4. In 0.16.5, the updated app picker passed device tests on Odin 3.

Install the APK over the official app, then use **Install/Update Components** and Start. Saved selections, mappings, appearance and startup preferences remain. No firmware flash or factory reset is needed.

Quick Settings stock-style switching remains Odin 2/API33 only. Older Odin 3 mapping 3.0(20251128_112846) is inspected but not hardware-tested. Unknown versions are blocked. Retroid Pocket 6 is read-only diagnostics; bridging is disabled and untested. Battery impact is not measured. Touchpad experiment is retained separately for later and is not included.

Package/signing identity and Sony controller IDs are unchanged. No private keys, device serials/logs or OEM binaries are published.

On Odin 3, the v0.16.6 preview passed an in-place APK update and Remote Play gameplay on the same bridge process and boot. Install the updated components once when upgrading from v0.16.5 or earlier.

The shared About layout was reviewed on Odin 3. The user confirmed that the dialog stays still when swiped.

Odin 3 on-demand registration passed gameplay and reboot testing. Dead Cells and Super Meat Boy worked with their switches off, and Remote Play worked with its switch on.
