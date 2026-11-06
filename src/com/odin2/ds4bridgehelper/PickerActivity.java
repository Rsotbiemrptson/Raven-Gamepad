package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PickerActivity extends ActivityGroup {
    private static final String TARGETS_FILE = "targets.conf";
    private final List<Choice> choices = new ArrayList<>(), visible = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private final Map<String, Drawable> icons = new HashMap<>();
    private PackageManager pm;
    private BaseAdapter adapter;
    private TextView status;
    private EditText search;
    private Switch systemApps;
    private boolean enabledOnly;
    private int foreground, secondary, background, accent;
    private int appliedTheme;
    private SettingsUi ui;
    private ListView list;
    private PopupMenu optionsPopup;
    private FrameLayout pane;
    private View appsPage,shellRoot;
    private TextView paneTitle;
    private Class<?> activeType=PickerActivity.class;
    private BridgeScreen activeChild;
    private Bundle pendingPaneState;

    private static final class Choice {
        final String pkg, label;
        final boolean system;
        final ApplicationInfo app;
        Choice(String pkg, String label, boolean system, ApplicationInfo app) {
            this.pkg = pkg; this.label = label; this.system = system; this.app = app;
        }
    }

    @Override protected void onCreate(Bundle state) {
        int mode = getPreferences(MODE_PRIVATE).getInt("theme", 0);
        appliedTheme=mode;
        boolean dark = mode == 2 || (mode == 0 && (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        setTheme(getResources().getIdentifier(dark?"RavenDark":"RavenLight","style",getPackageName()));
        super.onCreate(state);
        pendingPaneState=state==null?null:state.getBundle("pane_state");
        ui=new SettingsUi(this);foreground=ui.foreground;secondary=ui.secondary;background=ui.background;accent=ui.accent;
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        pm = getPackageManager();
        selected.addAll(readTargets());
        enabledOnly = state != null && state.getBoolean("enabled_only");
        buildScreen();
        loadChoices();
        if (state != null) search.setText(state.getString("query", ""));
        filterChoices();
        if(state!=null){String section=state.getString("pane","");for(Class<?> type:new Class<?>[]{AppearanceActivity.class,BridgeActivity.class,MappingActivity.class,DiagnosticsActivity.class})if(type.getName().equals(section)){openPane(type);break;}}
        else if(getIntent().getBooleanExtra("open_settings",false))openPane(AppearanceActivity.class);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("enabled_only", enabledOnly);
        outState.putString("query", search.getText().toString());
        outState.putString("pane",activeType.getName());
        if(activeChild!=null)outState.putBundle("pane_state",activeChild.savePaneState());
        super.onSaveInstanceState(outState);
    }
    @Override protected void onResume(){super.onResume();if(appliedTheme!=getPreferences(MODE_PRIVATE).getInt("theme",0)||ui.accent!=(getPreferences(MODE_PRIVATE).getInt("accentColor",0xff0070cc)|0xff000000)){recreate();return;}filterChoices();}

    private void buildScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(background);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(22), dp(10), dp(22), dp(4));header.setBackgroundColor(background);
        boolean wide=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        if(!wide)header.addView(ui.logo(),new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout names=ui.column();names.setPadding(wide?0:dp(10),0,0,0);if(!wide){TextView brand=ui.note("Raven Gamepad");brand.setPadding(0,0,0,0);names.addView(brand);}
        TextView title=ui.heading(enabledOnly ? "Enabled" : "Apps");paneTitle=title;title.setPadding(0,0,0,0);names.addView(title);header.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        Button more=ui.overflow();more.setOnClickListener(v->showOptions(more));header.addView(more,new LinearLayout.LayoutParams(dp(48),dp(44)));
        LinearLayout content=ui.column();content.setPadding(dp(22),dp(4),dp(22),dp(12));page.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(ui.note("Choose which apps receive your virtual controller input."));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(0,dp(4),0,dp(4));
        search = new EditText(this);
        search.setSingleLine(true); search.setHint("Search apps or package IDs");
        search.setTextColor(foreground); search.setHintTextColor(secondary);
        search.setTextSize(14);search.setBackground(ui.shape(ui.surface,24));search.setPadding(dp(16),dp(8),dp(16),dp(8));search.setHighlightColor(Color.argb(85,Color.red(accent),Color.green(accent),Color.blue(accent)));
        if(android.os.Build.VERSION.SDK_INT>=29){android.graphics.drawable.GradientDrawable cursor=ui.shape(ui.accentText,1);cursor.setSize(dp(2),dp(22));search.setTextCursorDrawable(cursor);}
        Drawable searchIcon=getDrawable(android.R.drawable.ic_menu_search).mutate();searchIcon.setTint(secondary);searchIcon.setBounds(0,0,dp(20),dp(20));search.setCompoundDrawables(searchIcon,null,null,null);search.setCompoundDrawablePadding(dp(8));
        controls.addView(search, new LinearLayout.LayoutParams(0, dp(44), 1));
        systemApps = accentSwitch();
        systemApps.setText("System apps");
        systemApps.setTextColor(foreground);
        systemApps.setTextSize(12);
        systemApps.setSwitchPadding(dp(6));
        systemApps.setContentDescription("Show system apps");
        systemApps.setChecked(getPreferences(MODE_PRIVATE).getBoolean("show_system", false));
        systemApps.setOnCheckedChangeListener((button, show) -> {
            getPreferences(MODE_PRIVATE).edit().putBoolean("show_system", show).apply();
            filterChoices();
        });
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(-2, dp(48));
        toggleParams.setMarginStart(dp(8));
        controls.addView(systemApps, toggleParams);
        content.addView(controls);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterChoices(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        status = new TextView(this);
        status.setTextColor(secondary); status.setTextSize(13);
        status.setPadding(0,dp(4),0,dp(8));
        content.addView(status);

        list = new ListView(this);
        list.setDivider(null);list.setDividerHeight(0);list.setCacheColorHint(background);list.setBackgroundColor(background);list.setSelector(android.R.color.transparent);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return visible.size(); }
            @Override public Object getItem(int position) { return visible.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                return makeRow(visible.get(position));
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = visible.get(position).pkg;
            setSelected(pkg, !selected.contains(pkg));
        });
        content.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        appsPage=page;pane=new FrameLayout(this);pane.setTag("raven-right-pane");pane.addView(page,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout right=ui.column();right.addView(header);right.addView(pane,new LinearLayout.LayoutParams(-1,0,1));
        shellRoot=RavenShell.wrap(this,ui,right,0,this::openPane);setContentView(shellRoot);
    }
    boolean isEnabledOnly() {
        return enabledOnly;
    }

    void openApps(boolean onlyEnabled) {
        if (activeChild != null) {
            activeChild.navigateAway(() -> showApps(onlyEnabled));
        } else {
            showApps(onlyEnabled);
        }
    }

    private void showApps(boolean onlyEnabled) {
        enabledOnly = onlyEnabled;
        if (activeType != PickerActivity.class) swapPane(PickerActivity.class);
        paneTitle.setText(enabledOnly ? "Enabled" : "Apps");
        filterChoices();
        list.setSelection(0);
        RavenShell.select(shellRoot, ui, PickerActivity.class);
    }

    void openPane(Class<?> type){
        if (type == PickerActivity.class) {
            openApps(false);
            return;
        }
        if(activeType==type)return;
        if(activeChild!=null)activeChild.navigateAway(()->swapPane(type));else swapPane(type);
    }
    private void swapPane(Class<?> type){
        ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);
        pane.removeAllViews();if(activeChild!=null)getLocalActivityManager().destroyActivity(activeType.getName(),true);
        activeChild=null;activeType=type;
        if(type==PickerActivity.class){pane.addView(appsPage,new FrameLayout.LayoutParams(-1,-1));paneTitle.setText(enabledOnly ? "Enabled" : "Apps");filterChoices();}
        else{
            Intent intent=new Intent(this,type);if(pendingPaneState!=null){intent.putExtra("pane_state",pendingPaneState);pendingPaneState=null;}
            android.view.Window window=getLocalActivityManager().startActivity(type.getName(),intent);
            activeChild=(BridgeScreen)getLocalActivityManager().getActivity(type.getName());View content=window.getDecorView();if(content.getParent() instanceof ViewGroup)((ViewGroup)content.getParent()).removeView(content);pane.addView(content,new FrameLayout.LayoutParams(-1,-1));
            paneTitle.setText(type==AppearanceActivity.class?"Appearance":type==BridgeActivity.class?"Bridge controls":type==MappingActivity.class?"Controller mapping":"Diagnostics");
        }
        RavenShell.select(shellRoot,ui,type);
    }
    @Override public void onBackPressed(){if(activeType!=PickerActivity.class)openPane(PickerActivity.class);else super.onBackPressed();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(activeChild!=null&&event.getKeyCode()!=KeyEvent.KEYCODE_BACK)return activeChild.dispatchKeyEvent(event);return super.dispatchKeyEvent(event);}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){return activeChild!=null?activeChild.dispatchGenericMotionEvent(event):super.dispatchGenericMotionEvent(event);}

    private View makeRow(Choice choice) {
        FrameLayout container=new FrameLayout(this);container.setPadding(0,0,0,dp(8));container.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(14), dp(8));
        row.setBackground(ui.shape(ui.surface,18));row.setMinimumHeight(dp(64));container.addView(row,new FrameLayout.LayoutParams(-1,-2));
        ImageView icon = new ImageView(this);
        Drawable drawable = icons.get(choice.pkg);
        if (drawable == null && choice.app != null) {
            drawable = choice.app.loadIcon(pm); icons.put(choice.pkg, drawable);
        }
        icon.setImageDrawable(drawable != null ? drawable : pm.getDefaultActivityIcon());
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.setMargins(dp(16), 0, dp(8), 0);
        TextView name = new TextView(this);
        name.setText(choice.label); name.setTextSize(16); name.setTextColor(foreground);
        name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(name);
        TextView pkg = new TextView(this);
        pkg.setText(choice.pkg); pkg.setTextSize(11); pkg.setTextColor(secondary);
        pkg.setSingleLine(true); pkg.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(pkg);
        row.addView(labels, labelParams);
        Switch toggle = accentSwitch();
        toggle.setChecked(selected.contains(choice.pkg));
        toggle.setContentDescription("Enable " + choice.label);
        toggle.setOnCheckedChangeListener((button, enabled) -> setSelected(choice.pkg, enabled));
        row.addView(toggle, new LinearLayout.LayoutParams(-2, dp(48)));
        container.setContentDescription(choice.label + ", " + choice.pkg
                + (selected.contains(choice.pkg) ? ", selected" : ", not selected"));
        return container;
    }

    private Switch accentSwitch() {
        Switch toggle = new Switch(this);
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new android.content.res.ColorStateList(
                states, new int[]{accent, secondary}));
        toggle.setTrackTintList(new android.content.res.ColorStateList(
                states, new int[]{accent, secondary}));
        return toggle;
    }

    private void showOptions(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        optionsPopup=popup;
        Menu menu = popup.getMenu();
        menu.add(0,6,1,"Settings");
        menu.add(0,7,2,"About");
        menu.add(0,8,3,"Report an issue");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId()==6)openPane(AppearanceActivity.class);
            else if (item.getItemId()==7)AboutDialog.show(this);
            else if (item.getItemId()==8)IssueReport.open(this, ui);
            return true;
        });
        popup.show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
    private File targetFile() {
        Context storage = createDeviceProtectedStorageContext();
        return new File(storage.getFilesDir(), TARGETS_FILE);
    }
    private Set<String> readTargets() {
        Set<String> targets = new HashSet<>();
        File file = targetFile();
        if (!file.exists()) return targets;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) if (validPackage(line)) targets.add(line);
        } catch (Exception e) {
            Toast.makeText(this, "Could not read saved apps", Toast.LENGTH_LONG).show();
        }
        return targets;
    }
    private static boolean validPackage(String pkg) {
        return pkg.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");
    }
    static boolean isSystemApp(ApplicationInfo app){
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0
            || app.packageName.equals("com.google.android.verifier")
            || app.packageName.equals("com.google.android.safetycore")
            || app.packageName.equals("com.google.android.contactkeys");
    }
    private void loadChoices() {
        Set<String> included = new HashSet<>();
        for (ApplicationInfo app : pm.getInstalledApplications(0)) {
            String pkg = app.packageName;
            if (!included.add(pkg) || pkg.equals(getPackageName())) continue;
            boolean system = isSystemApp(app);
            choices.add(new Choice(pkg, String.valueOf(app.loadLabel(pm)), system, app));
        }
        selected.retainAll(included);
        selected.remove(getPackageName());
        Collections.sort(choices, Comparator.comparing(c -> c.label.toLowerCase(Locale.ROOT)));
    }
    private void filterChoices() {
        if (adapter == null || search == null) return;
        String needle = search.getText().toString().toLowerCase(Locale.ROOT);
        boolean showSystem = getPreferences(MODE_PRIVATE).getBoolean("show_system", false);
        systemApps.setVisibility(enabledOnly ? View.GONE : View.VISIBLE);
        visible.clear();
        for (Choice c : choices) {
            if (enabledOnly ? !selected.contains(c.pkg) : !showSystem && c.system) continue;
            if (c.label.toLowerCase(Locale.ROOT).contains(needle)
                    || c.pkg.toLowerCase(Locale.ROOT).contains(needle)) visible.add(c);
        }
        adapter.notifyDataSetChanged(); updateStatus();
    }
    private void updateStatus() {
        if (visible.isEmpty() && search.getText().length() > 0) {
            status.setText("No matching apps | " + selected.size() + " selected");
        } else if (visible.isEmpty() && enabledOnly) {
            status.setText("No enabled apps | Select apps in the Apps tab");
        } else if (visible.isEmpty()
                && !getPreferences(MODE_PRIVATE).getBoolean("show_system", false)) {
            status.setText("No user apps installed | Turn on System apps");
        } else {
            status.setText(visible.size() + (visible.size()==1?" app shown | ":" apps shown | ") + selected.size() + " selected");
        }
    }
    private void setSelected(String pkg, boolean enabled) {
        Set<String> updated = new HashSet<>(selected);
        if (enabled) updated.add(pkg); else updated.remove(pkg);
        try {
            writeTargets(updated);
            selected.clear();
            selected.addAll(updated);
        } catch (IOException error) {
            Toast.makeText(this, "Could not update app selection", Toast.LENGTH_LONG).show();
        }
        filterChoices();
    }

    private void writeTargets(Set<String> targets) throws IOException {
        List<String> sorted = new ArrayList<>(targets);
        Collections.sort(sorted);
        File target = targetFile();
        File temporary = File.createTempFile(TARGETS_FILE, ".tmp", target.getParentFile());
        try {
            try (FileOutputStream stream = new FileOutputStream(temporary);
                 OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                for (String pkg : sorted) if (validPackage(pkg)) writer.write(pkg + "\n");
                writer.flush();
                stream.getFD().sync();
            }
            if (!temporary.renameTo(target)) throw new IOException("Could not replace targets file");
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }
}
