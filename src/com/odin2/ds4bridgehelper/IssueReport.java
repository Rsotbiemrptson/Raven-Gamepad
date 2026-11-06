package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

final class IssueReport {
    private static final String URL = "https://github.com/Rsotbiemrptson/Raven-Gamepad/issues/new";

    static void open(Activity activity, SettingsUi ui) {
        String version = "";
        try {
            version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}
        String body = "### Test environment\nRaven Gamepad version: " + version
                + "\nDevice model: \nAndroid and firmware version: \n"
                + "\n### Steps to reproduce\n\n### Expected result\n\n### Actual result or error message\n";
        Uri issue = Uri.parse(URL).buildUpon().appendQueryParameter("body", body).build();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, issue));
        } catch (ActivityNotFoundException unavailable) {
            ui.dialog().setTitle("Open GitHub Issues")
                    .setMessage("Open this address in a browser to report an issue:\n\n" + URL)
                    .setPositiveButton("OK", null).show();
        }
    }

    private IssueReport() {}
}
