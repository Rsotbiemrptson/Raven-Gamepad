#!/system/bin/sh
STATE=/data/local/ds4-bridge-app
HELPER=com.odin2.ds4bridgehelper
BRIDGE_PID=
HELPER_APK=
refresh_apk_path() {
    local package_paths
    package_paths="$(pm path "$HELPER" 2>/dev/null)" || return 1
    HELPER_APK="$(printf '%s\n' "$package_paths" | sed -n 's/^package://p' | head -n 1)"
    [ -n "$HELPER_APK" ]
}
read_setting() {
    if command -v content >/dev/null 2>&1; then
        content read --uri "content://$HELPER.targets/$1"
    else
        if [ "$1" = forwarding ] && [ "$(settings get system no_create_gamepad_button_layout 2>/dev/null)" = 1 ]; then echo off; return; fi
        [ -r "$HELPER_APK" ] || return 1
        CLASSPATH="$HELPER_APK" /system/bin/app_process /system/bin com.odin2.ds4bridgehelper.BridgeContentReader "$1"
    fi
}
cleanup() {
    [ -z "$BRIDGE_PID" ] || kill -TERM "$BRIDGE_PID" 2>/dev/null
    am broadcast -n "$HELPER/.ExclusionReceiver" --es action remove >/dev/null 2>&1
    rm -f "$STATE/supervisor.pid" "$STATE/bridge.pid"
}
trap 'exit 0' INT TERM
trap cleanup EXIT
echo "$$" >"$STATE/supervisor.pid"
while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done
am broadcast -n "$HELPER/.ExclusionReceiver" --es action add >/dev/null 2>&1
sleep 1
refresh_apk_path || exit 1
read_setting profile >"$STATE/profile.conf" 2>/dev/null
if [ -s "$STATE/profile.conf" ]; then
    "$STATE/odin_ds4_bridge" --profile "$STATE/profile.conf" --managed --seconds 0 &
else
    "$STATE/odin_ds4_bridge" --managed --seconds 0 &
fi
BRIDGE_PID=$!
echo "$BRIDGE_PID" >"$STATE/bridge.pid"
sleep 2
kill -0 "$BRIDGE_PID" 2>/dev/null || exit 1
dumpsys input 2>/dev/null | grep -q 'vendor=0x054c, product=0x09cc' || exit 1
missing_apk_checks=0
while kill -0 "$BRIDGE_PID" 2>/dev/null; do
    # Android can move base.apk when the app is updated in place.
    if ! refresh_apk_path; then
        kill -USR2 "$BRIDGE_PID" 2>/dev/null
        missing_apk_checks=$((missing_apk_checks + 1))
        [ "$missing_apk_checks" -lt 3 ] || exit 0
        sleep 1
        continue
    fi
    missing_apk_checks=0
    forwarding="$(read_setting forwarding 2>/dev/null)" || forwarding=off
    if [ "$forwarding" = off ]; then
        kill -USR2 "$BRIDGE_PID" 2>/dev/null
    elif [ "$forwarding" = all ]; then
        kill -USR1 "$BRIDGE_PID" 2>/dev/null
    elif [ "$forwarding" = selected ]; then
        top_package="$(dumpsys activity activities 2>/dev/null | grep -m 1 'topResumedActivity=' | sed -n 's/.* u[0-9][0-9]* \([^ /]*\)\/.*/\1/p')"
        targets="$(read_setting targets 2>/dev/null)"
        if [ -n "$top_package" ] && printf '%s\n' "$targets" | grep -Fqx "$top_package"; then
            kill -USR1 "$BRIDGE_PID" 2>/dev/null
        else
            kill -USR2 "$BRIDGE_PID" 2>/dev/null
        fi
    else
        kill -USR2 "$BRIDGE_PID" 2>/dev/null
    fi
    sleep 1
done
