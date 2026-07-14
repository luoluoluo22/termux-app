package com.termux.app;



import android.annotation.SuppressLint;

import android.app.AlertDialog;

import android.content.ActivityNotFoundException;

import android.content.BroadcastReceiver;

import android.content.ComponentName;

import android.content.Context;

import android.content.Intent;

import android.content.IntentFilter;

import android.content.ServiceConnection;

import android.net.Uri;

import android.os.Bundle;

import android.os.IBinder;

import android.view.ContextMenu;

import android.view.ContextMenu.ContextMenuInfo;

import android.view.Gravity;

import android.view.Menu;

import android.view.MenuItem;

import android.view.View;

import android.view.ViewGroup;

import android.view.WindowManager;

import android.widget.EditText;

import android.widget.ImageButton;

import android.widget.ListView;

import android.widget.RelativeLayout;

import android.widget.Toast;



import com.termux.R;

import com.termux.app.api.file.FileReceiverActivity;

import com.termux.app.terminal.TermuxActivityRootView;

import com.termux.app.terminal.TermuxTerminalSessionActivityClient;

import com.termux.app.terminal.io.TermuxTerminalExtraKeys;

import com.termux.shared.activities.ReportActivity;

import com.termux.shared.activity.ActivityUtils;

import com.termux.shared.activity.media.AppCompatActivityUtils;

import com.termux.shared.data.IntentUtils;

import com.termux.shared.android.PermissionUtils;

import com.termux.shared.data.DataUtils;

import com.termux.shared.termux.TermuxConstants;

import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY;

import com.termux.app.activities.HelpActivity;

import com.termux.app.activities.SettingsActivity;

import com.termux.shared.termux.crash.TermuxCrashUtils;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import com.termux.app.terminal.TermuxSessionsListViewController;

import com.termux.app.terminal.io.TerminalToolbarViewPager;

import com.termux.app.terminal.TermuxTerminalViewClient;

import com.termux.shared.termux.extrakeys.ExtraKeysView;

import com.termux.shared.termux.interact.TextInputDialogUtils;

import com.termux.shared.logger.Logger;

import com.termux.shared.termux.TermuxUtils;

import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;

import com.termux.shared.termux.theme.TermuxThemeUtils;

import com.termux.shared.theme.NightMode;

import com.termux.shared.view.ViewUtils;

import com.termux.terminal.TerminalSession;

import com.termux.terminal.TerminalSessionClient;

import com.termux.view.TerminalView;

import com.termux.view.TerminalViewClient;



import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.viewpager.widget.ViewPager;

import java.util.Arrays;

import java.util.List;

import java.util.ArrayList;

import java.util.Collections;

import java.io.File;

import java.io.FileReader;

import java.io.BufferedReader;

import android.graphics.Typeface;

import android.graphics.Color;

import android.graphics.Paint;

import android.text.SpannableString;

import android.text.Spanned;

import android.text.style.StyleSpan;

import android.widget.ArrayAdapter;

import android.widget.TextView;

import android.widget.ListView;

import android.view.View;

import android.view.ViewGroup;

import android.view.LayoutInflater;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.termux.shared.theme.ThemeUtils;

import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;

import androidx.viewpager.widget.ViewPager;

import java.util.Arrays;

import java.util.List;

import java.util.ArrayList;

import java.util.Collections;

import java.io.File;

import java.io.FileReader;

import java.io.BufferedReader;

import android.graphics.Typeface;

import android.graphics.Color;

import android.graphics.Paint;

import android.text.SpannableString;

import android.text.Spanned;

import android.text.style.StyleSpan;

import android.widget.ArrayAdapter;

import android.widget.TextView;

import android.widget.ListView;

import android.view.View;

import android.view.ViewGroup;

import android.view.LayoutInflater;

import android.content.Context;

import androidx.core.content.ContextCompat;





/**

 * A terminal emulator activity.

 * <p/>

 * See

 * <ul>

 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>

 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>

 * </ul>

 * about memory leaks.

 */

public final class TermuxActivity extends AppCompatActivity implements ServiceConnection {



    /**

     * The connection to the {@link TermuxService}. Requested in {@link #onCreate(Bundle)} with a call to

     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in

     * {@link #onServiceConnected(ComponentName, IBinder)}.

     */

    TermuxService mTermuxService;



    /**

     * The {@link TerminalView} shown in  {@link TermuxActivity} that displays the terminal.

     */

    TerminalView mTerminalView;



    /**

     *  The {@link TerminalViewClient} interface implementation to allow for communication between

     *  {@link TerminalView} and {@link TermuxActivity}.

     */

    TermuxTerminalViewClient mTermuxTerminalViewClient;



    /**

     *  The {@link TerminalSessionClient} interface implementation to allow for communication between

     *  {@link TerminalSession} and {@link TermuxActivity}.

     */

    TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;



    /**

     * Termux app shared preferences manager.

     */

    private TermuxAppSharedPreferences mPreferences;



    /**

     * Termux app SharedProperties loaded from termux.properties

     */

    private TermuxAppSharedProperties mProperties;



    /**

     * The root view of the {@link TermuxActivity}.

     */

    TermuxActivityRootView mTermuxActivityRootView;



    /**

     * The space at the bottom of {@link @mTermuxActivityRootView} of the {@link TermuxActivity}.

     */

    View mTermuxActivityBottomSpaceView;



    /**

     * The terminal extra keys view.

     */

    ExtraKeysView mExtraKeysView;



    /**

     * The client for the {@link #mExtraKeysView}.

     */

    TermuxTerminalExtraKeys mTermuxTerminalExtraKeys;



    /**

     * The termux sessions list controller.

     */

    TermuxSessionsListViewController mTermuxSessionListViewController;



    /**

     * The {@link TermuxActivity} broadcast receiver for various things like terminal style configuration changes.

     */

    private final BroadcastReceiver mTermuxActivityBroadcastReceiver = new TermuxActivityBroadcastReceiver();



    /**

     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.

     */

    Toast mLastToast;



    /**

     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the

     * time, so if the session causing a change is not in the foreground it should probably be treated as background.

     */

    private boolean mIsVisible;



    /**

     * If onResume() was called after onCreate().

     */

    private boolean mIsOnResumeAfterOnCreate = false;



    /**

     * If activity was restarted like due to call to {@link #recreate()} after receiving

     * {@link TERMUX_ACTIVITY#ACTION_RELOAD_STYLE}, system dark night mode was changed or activity

     * was killed by android.

     */

    private boolean mIsActivityRecreated = false;



    /**

     * The {@link TermuxActivity} is in an invalid state and must not be run.

     */

    private boolean mIsInvalidState;



    private int mNavBarHeight;



    private float mTerminalToolbarDefaultHeight;





    private static final int CONTEXT_MENU_SELECT_URL_ID = 0;

    private static final int CONTEXT_MENU_SHARE_TRANSCRIPT_ID = 1;

    private static final int CONTEXT_MENU_SHARE_SELECTED_TEXT = 10;

    private static final int CONTEXT_MENU_AUTOFILL_USERNAME = 11;

    private static final int CONTEXT_MENU_AUTOFILL_PASSWORD = 2;

    private static final int CONTEXT_MENU_RESET_TERMINAL_ID = 3;

    private static final int CONTEXT_MENU_KILL_PROCESS_ID = 4;

    private static final int CONTEXT_MENU_STYLING_ID = 5;

    private static final int CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON = 6;

    private static final int CONTEXT_MENU_HELP_ID = 7;

    private static final int CONTEXT_MENU_SETTINGS_ID = 8;

    private static final int CONTEXT_MENU_REPORT_ID = 9;



    private static final String ARG_TERMINAL_TOOLBAR_TEXT_INPUT = "terminal_toolbar_text_input";

    private static final String ARG_ACTIVITY_RECREATED = "activity_recreated";



    private static final String LOG_TAG = "TermuxActivity";



    @Override

    public void onCreate(Bundle savedInstanceState) {

        Logger.logDebug(LOG_TAG, "onCreate");

        mIsOnResumeAfterOnCreate = true;



        if (savedInstanceState != null)

            mIsActivityRecreated = savedInstanceState.getBoolean(ARG_ACTIVITY_RECREATED, false);



        // Delete ReportInfo serialized object files from cache older than 14 days

        ReportActivity.deleteReportInfoFilesOlderThanXDays(this, 14, false);



        // Load Termux app SharedProperties from disk

        mProperties = TermuxAppSharedProperties.getProperties();

        reloadProperties();



        setActivityTheme();



        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_termux);



        // Load termux shared preferences

        // This will also fail if TermuxConstants.TERMUX_PACKAGE_NAME does not equal applicationId

        mPreferences = TermuxAppSharedPreferences.build(this, true);

        if (mPreferences == null) {

            // An AlertDialog should have shown to kill the app, so we don't continue running activity code

            mIsInvalidState = true;

            return;

        }



        setMargins();



        mTermuxActivityRootView = findViewById(R.id.activity_termux_root_view);

        mTermuxActivityRootView.setActivity(this);

        mTermuxActivityBottomSpaceView = findViewById(R.id.activity_termux_bottom_space_view);

        mTermuxActivityRootView.setOnApplyWindowInsetsListener(new TermuxActivityRootView.WindowInsetsListener());



        View content = findViewById(android.R.id.content);

        content.setOnApplyWindowInsetsListener((v, insets) -> {

            mNavBarHeight = insets.getSystemWindowInsetBottom();

            return insets;

        });



        if (mProperties.isUsingFullScreen()) {

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        }



        setTermuxTerminalViewAndClients();



        setTerminalToolbarView(savedInstanceState);



        setSettingsButtonView();



        setNewSessionButtonView();



        setToggleKeyboardView();



        registerForContextMenu(mTerminalView);



        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);



        try {

            // Start the {@link TermuxService} and make it run regardless of who is bound to it

            Intent serviceIntent = new Intent(this, TermuxService.class);

            startService(serviceIntent);



            // Attempt to bind to the service, this will call the {@link #onServiceConnected(ComponentName, IBinder)}

            // callback if it succeeds.

            if (!bindService(serviceIntent, this, 0))

                throw new RuntimeException("bindService() failed");

        } catch (Exception e) {

            Logger.logStackTraceWithMessage(LOG_TAG,"TermuxActivity failed to start TermuxService", e);

            Logger.showToast(this,

                getString(e.getMessage() != null && e.getMessage().contains("app is in background") ?

                    R.string.error_termux_service_start_failed_bg : R.string.error_termux_service_start_failed_general),

                true);

            mIsInvalidState = true;

            return;

        }



        // Send the {@link TermuxConstants#BROADCAST_TERMUX_OPENED} broadcast to notify apps that Termux

        // app has been opened.

        TermuxUtils.sendTermuxOpenedBroadcast(this);

        writeGitHubConfig();

    }



    @Override

    public void onStart() {

        super.onStart();



        Logger.logDebug(LOG_TAG, "onStart");



        if (mIsInvalidState) return;



        mIsVisible = true;



        if (mTermuxTerminalSessionActivityClient != null)

            mTermuxTerminalSessionActivityClient.onStart();



        if (mTermuxTerminalViewClient != null)

            mTermuxTerminalViewClient.onStart();



        if (mPreferences.isTerminalMarginAdjustmentEnabled())

            addTermuxActivityRootViewGlobalLayoutListener();



        registerTermuxActivityBroadcastReceiver();

    }



    @Override

    public void onResume() {

        super.onResume();



        Logger.logVerbose(LOG_TAG, "onResume");



        if (mIsInvalidState) return;



        if (mTermuxTerminalSessionActivityClient != null)

            mTermuxTerminalSessionActivityClient.onResume();



        if (mTermuxTerminalViewClient != null)

            mTermuxTerminalViewClient.onResume();



        // Check if a crash happened on last run of the app or if a plugin crashed and show a

        // notification with the crash details if it did

        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(this, LOG_TAG);



        mIsOnResumeAfterOnCreate = false;

    }



    @Override

    protected void onStop() {

        super.onStop();



        Logger.logDebug(LOG_TAG, "onStop");



        if (mIsInvalidState) return;



        mIsVisible = false;



        if (mTermuxTerminalSessionActivityClient != null)

            mTermuxTerminalSessionActivityClient.onStop();



        if (mTermuxTerminalViewClient != null)

            mTermuxTerminalViewClient.onStop();



        removeTermuxActivityRootViewGlobalLayoutListener();



        unregisterTermuxActivityBroadcastReceiver();

        getDrawer().closeDrawers();

    }



    @Override

    public void onDestroy() {

        super.onDestroy();



        Logger.logDebug(LOG_TAG, "onDestroy");



        if (mIsInvalidState) return;



        if (mTermuxService != null) {

            // Do not leave service and session clients with references to activity.

            mTermuxService.unsetTermuxTerminalSessionClient();

            mTermuxService = null;

        }



        try {

            unbindService(this);

        } catch (Exception e) {

            // ignore.

        }

    }



    @Override

    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {

        Logger.logVerbose(LOG_TAG, "onSaveInstanceState");



        super.onSaveInstanceState(savedInstanceState);

        saveTerminalToolbarTextInput(savedInstanceState);

        savedInstanceState.putBoolean(ARG_ACTIVITY_RECREATED, true);

    }











    /**

     * Part of the {@link ServiceConnection} interface. The service is bound with

     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this

     * callback method.

     */

    @Override

    public void onServiceConnected(ComponentName componentName, IBinder service) {

        Logger.logDebug(LOG_TAG, "onServiceConnected");



        mTermuxService = ((TermuxService.LocalBinder) service).service;



        setTermuxSessionsListView();



        final Intent intent = getIntent();

        setIntent(null);



        if (mTermuxService.isTermuxSessionsEmpty()) {

            if (mIsVisible) {

                TermuxInstaller.setupBootstrapIfNeeded(TermuxActivity.this, () -> {

                    if (mTermuxService == null) return; // Activity might have been destroyed.

                    try {

                        boolean launchFailsafe = false;

                        if (intent != null && intent.getExtras() != null) {

                            launchFailsafe = intent.getExtras().getBoolean(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);

                        }

                        mTermuxTerminalSessionActivityClient.addNewSession(launchFailsafe, null);

                    } catch (WindowManager.BadTokenException e) {

                        // Activity finished - ignore.

                    }

                });

            } else {

                // The service connected while not in foreground - just bail out.

                finishActivityIfNotFinishing();

            }

        } else {

            // If termux was started from launcher "New session" shortcut and activity is recreated,

            // then the original intent will be re-delivered, resulting in a new session being re-added

            // each time.

            if (!mIsActivityRecreated && intent != null && Intent.ACTION_RUN.equals(intent.getAction())) {

                // Android 7.1 app shortcut from res/xml/shortcuts.xml.

                boolean isFailSafe = intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);

                mTermuxTerminalSessionActivityClient.addNewSession(isFailSafe, null);

            } else {

                mTermuxTerminalSessionActivityClient.setCurrentSession(mTermuxTerminalSessionActivityClient.getCurrentStoredSessionOrLast());

            }

        }



        // Update the {@link TerminalSession} and {@link TerminalEmulator} clients.

        mTermuxService.setTermuxTerminalSessionClient(mTermuxTerminalSessionActivityClient);

    }



    @Override

    public void onServiceDisconnected(ComponentName name) {

        Logger.logDebug(LOG_TAG, "onServiceDisconnected");



        // Respect being stopped from the {@link TermuxService} notification action.

        finishActivityIfNotFinishing();

    }













    private void reloadProperties() {

        mProperties.loadTermuxPropertiesFromDisk();



        if (mTermuxTerminalViewClient != null)

            mTermuxTerminalViewClient.onReloadProperties();

    }







    private void setActivityTheme() {

        // Update NightMode.APP_NIGHT_MODE

        TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());



        // Set activity night mode. If NightMode.SYSTEM is set, then android will automatically

        // trigger recreation of activity when uiMode/dark mode configuration is changed so that

        // day or night theme takes affect.

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

    }



    private void setMargins() {

        RelativeLayout relativeLayout = findViewById(R.id.activity_termux_root_relative_layout);

        int marginHorizontal = mProperties.getTerminalMarginHorizontal();

        int marginVertical = mProperties.getTerminalMarginVertical();

        ViewUtils.setLayoutMarginsInDp(relativeLayout, marginHorizontal, marginVertical, marginHorizontal, marginVertical);

    }







    public void addTermuxActivityRootViewGlobalLayoutListener() {

        getTermuxActivityRootView().getViewTreeObserver().addOnGlobalLayoutListener(getTermuxActivityRootView());

    }



    public void removeTermuxActivityRootViewGlobalLayoutListener() {

        if (getTermuxActivityRootView() != null)

            getTermuxActivityRootView().getViewTreeObserver().removeOnGlobalLayoutListener(getTermuxActivityRootView());

    }







    private void setTermuxTerminalViewAndClients() {

        // Set termux terminal view and session clients

        mTermuxTerminalSessionActivityClient = new TermuxTerminalSessionActivityClient(this);

        mTermuxTerminalViewClient = new TermuxTerminalViewClient(this, mTermuxTerminalSessionActivityClient);



        // Set termux terminal view

        mTerminalView = findViewById(R.id.terminal_view);

        mTerminalView.setTerminalViewClient(mTermuxTerminalViewClient);



        com.google.android.material.floatingactionbutton.FloatingActionButton drawerToggleButton = findViewById(R.id.drawer_toggle_button);

        if (drawerToggleButton != null) {

            drawerToggleButton.setOnClickListener(v -> {

                DrawerLayout drawerLayout = getDrawer();

                if (drawerLayout != null) {

                    if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {

                        drawerLayout.closeDrawer(Gravity.LEFT);

                    } else {

                        drawerLayout.openDrawer(Gravity.LEFT);

                    }

                }

            });

            DrawerLayout drawerLayout = getDrawer();

            if (drawerLayout != null) {

                drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {

                    @Override

                    public void onDrawerStateChanged(int newState) {

                        if (newState == DrawerLayout.STATE_SETTLING || newState == DrawerLayout.STATE_DRAGGING) {

                            reloadAIChatHistory();

                        }

                    }

                });

            }

        }



        if (mTermuxTerminalViewClient != null)

            mTermuxTerminalViewClient.onCreate();



        if (mTermuxTerminalSessionActivityClient != null)

            mTermuxTerminalSessionActivityClient.onCreate();

    }



    private void setTermuxSessionsListView() {

        ListView termuxSessionsListView = findViewById(R.id.terminal_sessions_list);

        mTermuxSessionListViewController = new TermuxSessionsListViewController(this, mTermuxService.getTermuxSessions());

        termuxSessionsListView.setAdapter(mTermuxSessionListViewController);

        termuxSessionsListView.setOnItemClickListener(mTermuxSessionListViewController);

        termuxSessionsListView.setOnItemLongClickListener(mTermuxSessionListViewController);

    }



    private static class AIChatItem {

        String uuid;

        String topic;

        long lastModified;



        AIChatItem(String uuid, String topic, long lastModified) {

            this.uuid = uuid;

            this.topic = topic;

            this.lastModified = lastModified;

        }

    }



    private class AIChatHistoryAdapter extends ArrayAdapter<AIChatItem> {

        private final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

        private final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);



        public AIChatHistoryAdapter(Context context, List<AIChatItem> items) {

            super(context, R.layout.item_terminal_sessions_list, items);

        }



        @NonNull

        @Override

        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            View rowView = convertView;

            if (rowView == null) {

                LayoutInflater inflater = getLayoutInflater();

                rowView = inflater.inflate(R.layout.item_terminal_sessions_list, parent, false);

            }



            TextView titleView = rowView.findViewById(R.id.session_title);

            AIChatItem item = getItem(position);

            if (item == null) {

                titleView.setText("null chat");

                return rowView;

            }



            boolean shouldEnableDarkTheme = com.termux.shared.theme.ThemeUtils.shouldEnableDarkTheme(

                TermuxActivity.this, NightMode.getAppNightMode().getName()

            );



            if (shouldEnableDarkTheme) {

                titleView.setBackground(

                    ContextCompat.getDrawable(TermuxActivity.this, R.drawable.session_background_black_selected)

                );

            }



            String prefix = "💬 ";

            String topic = item.topic;

            SpannableString styled = new SpannableString(prefix + topic);

            styled.setSpan(boldSpan, 0, prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            styled.setSpan(italicSpan, prefix.length(), styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);



            titleView.setText(styled);

            titleView.setPaintFlags(titleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

            titleView.setTextColor(shouldEnableDarkTheme ? Color.WHITE : Color.BLACK);



            return rowView;

        }

    }



    public void reloadAIChatHistory() {

        ListView aiListView = findViewById(R.id.ai_chat_history_list);

        if (aiListView == null) return;



        List<AIChatItem> items = new ArrayList<>();

        String AGY_BRAIN_DIR = "/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/.gemini/antigravity-cli/brain";

        File brainDir = new File(AGY_BRAIN_DIR);

        if (brainDir.exists() && brainDir.isDirectory()) {

            File[] files = brainDir.listFiles();

            if (files != null) {

                for (File dir : files) {

                    if (dir.isDirectory() && dir.getName().length() == 36) { // It is a UUID

                        File customTitleFile = new File(dir, "custom_title.txt");

                        String topic = null;

                        if (customTitleFile.exists()) {

                            topic = readCustomTitle(customTitleFile);

                        }

                        if (topic == null || topic.isEmpty()) {

                            File logsDir = new File(dir, ".system_generated/logs");

                            File transcript = new File(logsDir, "transcript.jsonl");

                            if (transcript.exists()) {

                                topic = extractTopicFromTranscript(transcript);

                            }

                        }

                        if (topic == null || topic.isEmpty()) {

                            topic = "新会话 (" + dir.getName().substring(0, 6) + ")";

                        }

                        items.add(new AIChatItem(dir.getName(), topic, dir.lastModified()));

                    }

                }

            }

        }



        Collections.sort(items, (a, b) -> Long.compare(b.lastModified, a.lastModified));



        AIChatHistoryAdapter adapter = new AIChatHistoryAdapter(this, items);

        aiListView.setAdapter(adapter);

        aiListView.setOnItemClickListener((parent, view, position, id) -> {

            AIChatItem clickedItem = items.get(position);

            startAIChatSession(clickedItem.uuid, clickedItem.topic);

        });

        aiListView.setOnItemLongClickListener((parent, view, position, id) -> {

            AIChatItem clickedItem = items.get(position);

            showAIChatMenuDialog(clickedItem);

            return true;

        });

    }



    private String readCustomTitle(File file) {

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {

            return reader.readLine();

        } catch (Exception e) {

            return null;

        }

    }



    private void showAIChatMenuDialog(AIChatItem item) {

        String[] options = {"重命名话题", "删除话题"};

        new AlertDialog.Builder(this)

            .setTitle("管理 AI 话题")

            .setItems(options, (dialog, which) -> {

                if (which == 0) {

                    showRenameAIChatDialog(item);

                } else if (which == 1) {

                    showDeleteAIChatConfirmDialog(item);

                }

            })

            .show();

    }



    private void showRenameAIChatDialog(AIChatItem item) {

        final android.widget.EditText input = new android.widget.EditText(this);

        input.setText(item.topic);

        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(this)

            .setTitle("重命名话题")

            .setView(input)

            .setPositiveButton("确定", (dialog, which) -> {

                String newTitle = input.getText().toString().trim();

                if (!newTitle.isEmpty()) {

                    saveCustomTitle(item.uuid, newTitle);

                    reloadAIChatHistory();

                }

            })

            .setNegativeButton("取消", null)

            .show();

    }



    private void saveCustomTitle(String uuid, String title) {

        String AGY_BRAIN_DIR = "/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/.gemini/antigravity-cli/brain";

        File convDir = new File(AGY_BRAIN_DIR, uuid);

        if (convDir.exists() && convDir.isDirectory()) {

            File titleFile = new File(convDir, "custom_title.txt");

            try (java.io.FileWriter writer = new java.io.FileWriter(titleFile)) {

                writer.write(title);

            } catch (Exception e) {

                // Ignore

            }

        }

    }



    private void showDeleteAIChatConfirmDialog(AIChatItem item) {

        new AlertDialog.Builder(this)

            .setTitle("确认删除")

            .setMessage("确定要删除话题 \"" + item.topic + "\" 吗？此操作无法撤销。")

            .setPositiveButton("确定", (dialog, which) -> {

                deleteAIChat(item.uuid);

                reloadAIChatHistory();

            })

            .setNegativeButton("取消", null)

            .show();

    }



    private void deleteAIChat(String uuid) {

        TermuxService service = getTermuxService();

        if (service != null) {

            List<TermuxSession> sessionsToRemove = new ArrayList<>();

            for (TermuxSession session : service.getTermuxSessions()) {

                String[] args = session.getExecutionCommand().arguments;

                if (args != null && args.length > 0) {

                    for (String arg : args) {

                        if (arg != null && arg.contains(uuid)) {

                            sessionsToRemove.add(session);

                            break;

                        }

                    }

                }

            }

            for (TermuxSession session : sessionsToRemove) {

                session.getTerminalSession().finishIfRunning();

                service.removeTermuxSession(session.getTerminalSession());

            }

        }



        String AGY_BRAIN_DIR = "/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/.gemini/antigravity-cli/brain";

        File convDir = new File(AGY_BRAIN_DIR, uuid);

        if (convDir.exists()) {

            deleteFolderRecursively(convDir);

        }

    }



    private void deleteFolderRecursively(File file) {

        if (file.isDirectory()) {

            File[] children = file.listFiles();

            if (children != null) {

                for (File child : children) {

                    deleteFolderRecursively(child);

                }

            }

        }

        file.delete();

    }



    private String extractTopicFromTranscript(File transcriptFile) {

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(transcriptFile))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.contains("\"type\":\"USER_INPUT\"")) {

                    org.json.JSONObject obj = new org.json.JSONObject(line);

                    if ("USER_INPUT".equals(obj.optString("type"))) {

                        String content = obj.optString("content");

                        if (content != null) {

                            content = content.trim();

                            if (content.length() > 18) {

                                return content.substring(0, 18) + "...";

                            }

                            return content;

                        }

                    }

                }

            }

        } catch (Exception e) {

            // Ignore

        }

        return null;

    }



    public void startAIChatSession(String uuid, String topic) {

        TermuxService service = getTermuxService();

        if (service == null) return;



        for (TermuxSession session : service.getTermuxSessions()) {

            String[] args = session.getExecutionCommand().arguments;

            if (args != null && args.length > 0) {

                for (String arg : args) {

                    if (arg != null && arg.contains(uuid)) {

                        mTermuxTerminalSessionActivityClient.setCurrentSession(session.getTerminalSession());

                        getDrawer().closeDrawers();

                        return;

                    }

                }

            }

        }



        String workingDirectory = getProperties().getDefaultWorkingDirectory();

        String execPath = "/data/data/com.termux/files/usr/bin/agy";

        String[] arguments = new String[]{"--conversation=" + uuid};



        TermuxSession newTermuxSession = service.createTermuxSession(

            execPath, arguments, null, workingDirectory, false, topic

        );

        if (newTermuxSession == null) return;



        mTermuxTerminalSessionActivityClient.setCurrentSession(newTermuxSession.getTerminalSession());

        getDrawer().closeDrawers();

    }







    private void setTerminalToolbarView(Bundle savedInstanceState) {

        mTermuxTerminalExtraKeys = new TermuxTerminalExtraKeys(this, mTerminalView,

            mTermuxTerminalViewClient, mTermuxTerminalSessionActivityClient);



        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();

        if (mPreferences.shouldShowTerminalToolbar()) terminalToolbarViewPager.setVisibility(View.VISIBLE);



        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();

        mTerminalToolbarDefaultHeight = layoutParams.height;



        setTerminalToolbarHeight();



        String savedTextInput = null;

        if (savedInstanceState != null)

            savedTextInput = savedInstanceState.getString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT);



        terminalToolbarViewPager.setAdapter(new TerminalToolbarViewPager.PageAdapter(this, savedTextInput));

        terminalToolbarViewPager.addOnPageChangeListener(new TerminalToolbarViewPager.OnPageChangeListener(this, terminalToolbarViewPager));

    }



    private void setTerminalToolbarHeight() {

        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();

        if (terminalToolbarViewPager == null) return;



        ViewGroup.LayoutParams layoutParams = terminalToolbarViewPager.getLayoutParams();

        layoutParams.height = Math.round(mTerminalToolbarDefaultHeight *

            (mTermuxTerminalExtraKeys.getExtraKeysInfo() == null ? 0 : mTermuxTerminalExtraKeys.getExtraKeysInfo().getMatrix().length) *

            mProperties.getTerminalToolbarHeightScaleFactor());

        terminalToolbarViewPager.setLayoutParams(layoutParams);

    }



    public void toggleTerminalToolbar() {

        final ViewPager terminalToolbarViewPager = getTerminalToolbarViewPager();

        if (terminalToolbarViewPager == null) return;



        final boolean showNow = mPreferences.toogleShowTerminalToolbar();

        Logger.showToast(this, (showNow ? getString(R.string.msg_enabling_terminal_toolbar) : getString(R.string.msg_disabling_terminal_toolbar)), true);

        terminalToolbarViewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);

        if (showNow && isTerminalToolbarTextInputViewSelected()) {

            // Focus the text input view if just revealed.

            findViewById(R.id.terminal_toolbar_text_input).requestFocus();

        }

    }



    private void saveTerminalToolbarTextInput(Bundle savedInstanceState) {

        if (savedInstanceState == null) return;



        final EditText textInputView = findViewById(R.id.terminal_toolbar_text_input);

        if (textInputView != null) {

            String textInput = textInputView.getText().toString();

            if (!textInput.isEmpty()) savedInstanceState.putString(ARG_TERMINAL_TOOLBAR_TEXT_INPUT, textInput);

        }

    }







    private void setSettingsButtonView() {

        ImageButton settingsButton = findViewById(R.id.settings_button);

        settingsButton.setOnClickListener(v -> {

            ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));

        });

    }



    private void setNewSessionButtonView() {

        View newSessionButton = findViewById(R.id.new_session_button);

        newSessionButton.setOnClickListener(v -> mTermuxTerminalSessionActivityClient.addNewSession(false, null));

        newSessionButton.setOnLongClickListener(v -> {

            TextInputDialogUtils.textInput(TermuxActivity.this, R.string.title_create_named_session, null,

                R.string.action_create_named_session_confirm, text -> mTermuxTerminalSessionActivityClient.addNewSession(false, text),

                R.string.action_new_session_failsafe, text -> mTermuxTerminalSessionActivityClient.addNewSession(true, text),

                -1, null, null);

            return true;

        });

    }



    private void setToggleKeyboardView() {

        findViewById(R.id.toggle_keyboard_button).setOnClickListener(v -> {

            mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();

            getDrawer().closeDrawers();

        });



        findViewById(R.id.toggle_keyboard_button).setOnLongClickListener(v -> {

            toggleTerminalToolbar();

            return true;

        });

    }











    @SuppressLint("RtlHardcoded")

    @Override

    public void onBackPressed() {

        if (getDrawer().isDrawerOpen(Gravity.LEFT)) {

            getDrawer().closeDrawers();

        } else {

            finishActivityIfNotFinishing();

        }

    }



    public void finishActivityIfNotFinishing() {

        // prevent duplicate calls to finish() if called from multiple places

        if (!TermuxActivity.this.isFinishing()) {

            finish();

        }

    }



    /** Show a toast and dismiss the last one if still visible. */

    public void showToast(String text, boolean longDuration) {

        if (text == null || text.isEmpty()) return;

        if (mLastToast != null) mLastToast.cancel();

        mLastToast = Toast.makeText(TermuxActivity.this, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);

        mLastToast.setGravity(Gravity.TOP, 0, 0);

        mLastToast.show();

    }







    @Override

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        TerminalSession currentSession = getCurrentSession();

        if (currentSession == null) return;



        boolean autoFillEnabled = mTerminalView.isAutoFillEnabled();



        menu.add(Menu.NONE, CONTEXT_MENU_SELECT_URL_ID, Menu.NONE, R.string.action_select_url);

        menu.add(Menu.NONE, CONTEXT_MENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.action_share_transcript);

        if (!DataUtils.isNullOrEmpty(mTerminalView.getStoredSelectedText()))

            menu.add(Menu.NONE, CONTEXT_MENU_SHARE_SELECTED_TEXT, Menu.NONE, R.string.action_share_selected_text);

        if (autoFillEnabled)

            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_USERNAME, Menu.NONE, R.string.action_autofill_username);

        if (autoFillEnabled)

            menu.add(Menu.NONE, CONTEXT_MENU_AUTOFILL_PASSWORD, Menu.NONE, R.string.action_autofill_password);

        menu.add(Menu.NONE, CONTEXT_MENU_RESET_TERMINAL_ID, Menu.NONE, R.string.action_reset_terminal);

        menu.add(Menu.NONE, CONTEXT_MENU_KILL_PROCESS_ID, Menu.NONE, getResources().getString(R.string.action_kill_process, getCurrentSession().getPid())).setEnabled(currentSession.isRunning());

        menu.add(Menu.NONE, CONTEXT_MENU_STYLING_ID, Menu.NONE, R.string.action_style_terminal);

        menu.add(Menu.NONE, CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON, Menu.NONE, R.string.action_toggle_keep_screen_on).setCheckable(true).setChecked(mPreferences.shouldKeepScreenOn());

        menu.add(Menu.NONE, CONTEXT_MENU_HELP_ID, Menu.NONE, R.string.action_open_help);

        menu.add(Menu.NONE, CONTEXT_MENU_SETTINGS_ID, Menu.NONE, R.string.action_open_settings);

        menu.add(Menu.NONE, CONTEXT_MENU_REPORT_ID, Menu.NONE, R.string.action_report_issue);

    }



    /** Hook system menu to show context menu instead. */

    @Override

    public boolean onCreateOptionsMenu(Menu menu) {

        mTerminalView.showContextMenu();

        return false;

    }



    @Override

    public boolean onContextItemSelected(MenuItem item) {

        TerminalSession session = getCurrentSession();



        switch (item.getItemId()) {

            case CONTEXT_MENU_SELECT_URL_ID:

                mTermuxTerminalViewClient.showUrlSelection();

                return true;

            case CONTEXT_MENU_SHARE_TRANSCRIPT_ID:

                mTermuxTerminalViewClient.shareSessionTranscript();

                return true;

            case CONTEXT_MENU_SHARE_SELECTED_TEXT:

                mTermuxTerminalViewClient.shareSelectedText();

                return true;

            case CONTEXT_MENU_AUTOFILL_USERNAME:

                mTerminalView.requestAutoFillUsername();

                return true;

            case CONTEXT_MENU_AUTOFILL_PASSWORD:

                mTerminalView.requestAutoFillPassword();

                return true;

            case CONTEXT_MENU_RESET_TERMINAL_ID:

                onResetTerminalSession(session);

                return true;

            case CONTEXT_MENU_KILL_PROCESS_ID:

                showKillSessionDialog(session);

                return true;

            case CONTEXT_MENU_STYLING_ID:

                showStylingDialog();

                return true;

            case CONTEXT_MENU_TOGGLE_KEEP_SCREEN_ON:

                toggleKeepScreenOn();

                return true;

            case CONTEXT_MENU_HELP_ID:

                ActivityUtils.startActivity(this, new Intent(this, HelpActivity.class));

                return true;

            case CONTEXT_MENU_SETTINGS_ID:

                ActivityUtils.startActivity(this, new Intent(this, SettingsActivity.class));

                return true;

            case CONTEXT_MENU_REPORT_ID:

                mTermuxTerminalViewClient.reportIssueFromTranscript();

                return true;

            default:

                return super.onContextItemSelected(item);

        }

    }



    @Override

    public void onContextMenuClosed(Menu menu) {

        super.onContextMenuClosed(menu);

        // onContextMenuClosed() is triggered twice if back button is pressed to dismiss instead of tap for some reason

        mTerminalView.onContextMenuClosed(menu);

    }



    private void showKillSessionDialog(TerminalSession session) {

        if (session == null) return;



        final AlertDialog.Builder b = new AlertDialog.Builder(this);

        b.setIcon(android.R.drawable.ic_dialog_alert);

        b.setMessage(R.string.title_confirm_kill_process);

        b.setPositiveButton(android.R.string.yes, (dialog, id) -> {

            dialog.dismiss();

            session.finishIfRunning();

        });

        b.setNegativeButton(android.R.string.no, null);

        b.show();

    }



    private void onResetTerminalSession(TerminalSession session) {

        if (session != null) {

            session.reset();

            showToast(getResources().getString(R.string.msg_terminal_reset), true);



            if (mTermuxTerminalSessionActivityClient != null)

                mTermuxTerminalSessionActivityClient.onResetTerminalSession();

        }

    }



    private void showStylingDialog() {

        Intent stylingIntent = new Intent();

        stylingIntent.setClassName(TermuxConstants.TERMUX_STYLING_PACKAGE_NAME, TermuxConstants.TERMUX_STYLING_APP.TERMUX_STYLING_ACTIVITY_NAME);

        try {

            startActivity(stylingIntent);

        } catch (ActivityNotFoundException | IllegalArgumentException e) {

            // The startActivity() call is not documented to throw IllegalArgumentException.

            // However, crash reporting shows that it sometimes does, so catch it here.

            new AlertDialog.Builder(this).setMessage(getString(R.string.error_styling_not_installed))

                .setPositiveButton(R.string.action_styling_install,

                    (dialog, which) -> ActivityUtils.startActivity(this, new Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_STYLING_FDROID_PACKAGE_URL))))

                .setNegativeButton(android.R.string.cancel, null).show();

        }

    }

    private void toggleKeepScreenOn() {

        if (mTerminalView.getKeepScreenOn()) {

            mTerminalView.setKeepScreenOn(false);

            mPreferences.setKeepScreenOn(false);

        } else {

            mTerminalView.setKeepScreenOn(true);

            mPreferences.setKeepScreenOn(true);

        }

    }







    /**

     * For processes to access primary external storage (/sdcard, /storage/emulated/0, ~/storage/shared),

     * termux needs to be granted legacy WRITE_EXTERNAL_STORAGE or MANAGE_EXTERNAL_STORAGE permissions

     * if targeting targetSdkVersion 30 (android 11) and running on sdk 30 (android 11) and higher.

     */

    public void requestStoragePermission(boolean isPermissionCallback) {

        new Thread() {

            @Override

            public void run() {

                // Do not ask for permission again

                int requestCode = isPermissionCallback ? -1 : PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION;



                // If permission is granted, then also setup storage symlinks.

                if(PermissionUtils.checkAndRequestLegacyOrManageExternalStoragePermission(

                    TermuxActivity.this, requestCode, !isPermissionCallback)) {

                    if (isPermissionCallback)

                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,

                            getString(com.termux.shared.R.string.msg_storage_permission_granted_on_request));



                    TermuxInstaller.setupStorageSymlinks(TermuxActivity.this);

                } else {

                    if (isPermissionCallback)

                        Logger.logInfoAndShowToast(TermuxActivity.this, LOG_TAG,

                            getString(com.termux.shared.R.string.msg_storage_permission_not_granted_on_request));

                }

            }

        }.start();

    }



    @Override

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        Logger.logVerbose(LOG_TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: "  + resultCode + ", data: "  + IntentUtils.getIntentString(data));

        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {

            requestStoragePermission(true);

        } else if (requestCode == 1001 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            handleSelectedFile(data.getData());

        }

    }



    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Logger.logVerbose(LOG_TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", permissions: "  + Arrays.toString(permissions) + ", grantResults: "  + Arrays.toString(grantResults));

        if (requestCode == PermissionUtils.REQUEST_GRANT_STORAGE_PERMISSION) {

            requestStoragePermission(true);

        }

    }







    public int getNavBarHeight() {

        return mNavBarHeight;

    }



    public TermuxActivityRootView getTermuxActivityRootView() {

        return mTermuxActivityRootView;

    }



    public View getTermuxActivityBottomSpaceView() {

        return mTermuxActivityBottomSpaceView;

    }



    public ExtraKeysView getExtraKeysView() {

        return mExtraKeysView;

    }



    public TermuxTerminalExtraKeys getTermuxTerminalExtraKeys() {

        return mTermuxTerminalExtraKeys;

    }



    public void setExtraKeysView(ExtraKeysView extraKeysView) {

        mExtraKeysView = extraKeysView;

    }



    public DrawerLayout getDrawer() {

        return (DrawerLayout) findViewById(R.id.drawer_layout);

    }





    public ViewPager getTerminalToolbarViewPager() {

        return (ViewPager) findViewById(R.id.terminal_toolbar_view_pager);

    }



    public float getTerminalToolbarDefaultHeight() {

        return mTerminalToolbarDefaultHeight;

    }



    public boolean isTerminalViewSelected() {

        return getTerminalToolbarViewPager().getCurrentItem() == 0;

    }



    public boolean isTerminalToolbarTextInputViewSelected() {

        return getTerminalToolbarViewPager().getCurrentItem() == 1;

    }





    public void termuxSessionListNotifyUpdated() {

        mTermuxSessionListViewController.notifyDataSetChanged();

    }



    public boolean isVisible() {

        return mIsVisible;

    }



    public boolean isOnResumeAfterOnCreate() {

        return mIsOnResumeAfterOnCreate;

    }



    public boolean isActivityRecreated() {

        return mIsActivityRecreated;

    }







    public TermuxService getTermuxService() {

        return mTermuxService;

    }



    public TerminalView getTerminalView() {

        return mTerminalView;

    }



    public TermuxTerminalViewClient getTermuxTerminalViewClient() {

        return mTermuxTerminalViewClient;

    }



    public TermuxTerminalSessionActivityClient getTermuxTerminalSessionClient() {

        return mTermuxTerminalSessionActivityClient;

    }



    @Nullable

    public TerminalSession getCurrentSession() {

        if (mTerminalView != null)

            return mTerminalView.getCurrentSession();

        else

            return null;

    }



    public TermuxAppSharedPreferences getPreferences() {

        return mPreferences;

    }



    public TermuxAppSharedProperties getProperties() {

        return mProperties;

    }









    public static void updateTermuxActivityStyling(Context context, boolean recreateActivity) {

        // Make sure that terminal styling is always applied.

        Intent stylingIntent = new Intent(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);

        stylingIntent.putExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, recreateActivity);

        context.sendBroadcast(stylingIntent);

    }



    private void registerTermuxActivityBroadcastReceiver() {

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH);

        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_RELOAD_STYLE);

        intentFilter.addAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);



        registerReceiver(mTermuxActivityBroadcastReceiver, intentFilter);

    }



    private void unregisterTermuxActivityBroadcastReceiver() {

        unregisterReceiver(mTermuxActivityBroadcastReceiver);

    }



    private void fixTermuxActivityBroadcastReceiverIntent(Intent intent) {

        if (intent == null) return;



        String extraReloadStyle = intent.getStringExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);

        if ("storage".equals(extraReloadStyle)) {

            intent.removeExtra(TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE);

            intent.setAction(TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS);

        }

    }



    class TermuxActivityBroadcastReceiver extends BroadcastReceiver {

        @Override

        public void onReceive(Context context, Intent intent) {

            if (intent == null) return;



            if (mIsVisible) {

                fixTermuxActivityBroadcastReceiverIntent(intent);



                switch (intent.getAction()) {

                    case TERMUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH:

                        Logger.logDebug(LOG_TAG, "Received intent to notify app crash");

                        TermuxCrashUtils.notifyAppCrashFromCrashLogFile(context, LOG_TAG);

                        return;

                    case TERMUX_ACTIVITY.ACTION_RELOAD_STYLE:

                        Logger.logDebug(LOG_TAG, "Received intent to reload styling");

                        reloadActivityStyling(intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY, true));

                        return;

                    case TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS:

                        Logger.logDebug(LOG_TAG, "Received intent to request storage permissions");

                        requestStoragePermission(false);

                        return;

                    default:

                }

            }

        }

    }



    private void reloadActivityStyling(boolean recreateActivity) {

        if (mProperties != null) {

            reloadProperties();



            if (mExtraKeysView != null) {

                mExtraKeysView.setButtonTextAllCaps(mProperties.shouldExtraKeysTextBeAllCaps());

                mExtraKeysView.reload(mTermuxTerminalExtraKeys.getExtraKeysInfo(), mTerminalToolbarDefaultHeight);

            }



            // Update NightMode.APP_NIGHT_MODE

            TermuxThemeUtils.setAppNightMode(mProperties.getNightMode());

        }



        setMargins();

        setTerminalToolbarHeight();



        FileReceiverActivity.updateFileReceiverActivityComponentsState(this);



        if (mTermuxTerminalSessionActivityClient != null)

            mTermuxTerminalSessionActivityClient.onReloadActivityStyling();



        if (mTermuxTerminalViewClient != null)

            mTermuxTerminalViewClient.onReloadActivityStyling();



        // To change the activity and drawer theme, activity needs to be recreated.

        // It will destroy the activity, including all stored variables and views, and onCreate()

        // will be called again. Extra keys input text, terminal sessions and transcripts will be preserved.

        if (recreateActivity) {

            Logger.logDebug(LOG_TAG, "Recreating activity");

            TermuxActivity.this.recreate();

        }

    }







    public static void startTermuxActivity(@NonNull final Context context) {

        ActivityUtils.startActivity(context, newInstance(context));

    }



    public static Intent newInstance(@NonNull final Context context) {

        Intent intent = new Intent(context, TermuxActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;

    }

    public void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择要发送给 AI 的文件"), 1001);
        } catch (android.content.ActivityNotFoundException ex) {
            Logger.showToast(this, "未找到文件管理器", true);
        }
    }

    private void handleSelectedFile(android.net.Uri uri) {
        new Thread(() -> {
            try {
                String fileName = "upload_file";
                try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (index != -1) {
                            fileName = cursor.getString(index);
                        }
                    }
                }
                
                // Sanitize filename
                fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

                java.io.File homeDir = new java.io.File("/data/data/com.termux/files/home");
                if (!homeDir.exists()) {
                    homeDir = new java.io.File("/data/user/0/com.termux/files/home");
                }
                if (!homeDir.exists()) {
                    homeDir.mkdirs();
                }

                java.io.File destFile = new java.io.File(homeDir, fileName);
                try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                final String insertedPath = "~/" + fileName;
                runOnUiThread(() -> {
                    TerminalView terminalView = getTerminalView();
                    if (terminalView != null) {
                        com.termux.terminal.TerminalSession session = terminalView.getCurrentSession();
                        if (session != null) {
                            session.write(insertedPath);
                        }
                    }
                    Logger.showToast(this, "已上传到 " + insertedPath, false);
                });
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to copy selected file", e);
                runOnUiThread(() -> Logger.showToast(this, "文件上传失败: " + e.getMessage(), true));
            }
        }).start();
    }

    private void writeGitHubConfig() {
        new Thread(() -> {
            try {
                java.io.File rootfsRoot = new java.io.File("/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root");
                if (!rootfsRoot.exists()) {
                    rootfsRoot = new java.io.File("/data/user/0/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root");
                }
                if (rootfsRoot.exists() && rootfsRoot.isDirectory()) {
                    // 1. Write .bashrc
                    java.io.File bashrc = new java.io.File(rootfsRoot, ".bashrc");
                    StringBuilder bashrcContent = new StringBuilder();
                    if (bashrc.exists()) {
                        byte[] bytes = java.nio.file.Files.readAllBytes(bashrc.toPath());
                        String fileContent = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                        for (String line : fileContent.split("\n")) {
                            if (!line.contains("GITHUB_TOKEN") && !line.contains("GITHUB_USERNAME") && !line.contains("GITHUB_EMAIL")) {
                                bashrcContent.append(line).append("\n");
                            }
                        }
                    }
                    if (com.termux.BuildConfig.GITHUB_TOKEN != null && !com.termux.BuildConfig.GITHUB_TOKEN.isEmpty()) {
                        bashrcContent.append("export GITHUB_TOKEN=\"").append(com.termux.BuildConfig.GITHUB_TOKEN).append("\"\n");
                    }
                    if (com.termux.BuildConfig.GITHUB_USERNAME != null && !com.termux.BuildConfig.GITHUB_USERNAME.isEmpty()) {
                        bashrcContent.append("export GITHUB_USERNAME=\"").append(com.termux.BuildConfig.GITHUB_USERNAME).append("\"\n");
                    }
                    if (com.termux.BuildConfig.GITHUB_EMAIL != null && !com.termux.BuildConfig.GITHUB_EMAIL.isEmpty()) {
                        bashrcContent.append("export GITHUB_EMAIL=\"").append(com.termux.BuildConfig.GITHUB_EMAIL).append("\"\n");
                    }
                    java.nio.file.Files.write(bashrc.toPath(), bashrcContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    // 2. Write .gitconfig
                    if (com.termux.BuildConfig.GITHUB_USERNAME != null && !com.termux.BuildConfig.GITHUB_USERNAME.isEmpty()) {
                        java.io.File gitconfig = new java.io.File(rootfsRoot, ".gitconfig");
                        String gitconfigContent = "[user]\n    name = " + com.termux.BuildConfig.GITHUB_USERNAME + "\n    email = " + com.termux.BuildConfig.GITHUB_EMAIL + "\n";
                        java.nio.file.Files.write(gitconfig.toPath(), gitconfigContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    
                    Logger.logDebug(LOG_TAG, "GitHub configuration successfully written.");
                }
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to write GitHub configuration", e);
            }
        }).start();
    }

}

