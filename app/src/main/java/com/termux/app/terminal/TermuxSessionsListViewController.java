package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.shared.theme.NightMode;
import com.termux.shared.theme.ThemeUtils;
import com.termux.terminal.TerminalSession;

import java.util.List;
import java.io.File;

public class TermuxSessionsListViewController extends ArrayAdapter<TermuxSession> implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    final TermuxActivity mActivity;

    final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
    final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    public TermuxSessionsListViewController(TermuxActivity activity, List<TermuxSession> sessionList) {
        super(activity.getApplicationContext(), R.layout.item_terminal_sessions_list, sessionList);
        this.mActivity = activity;
    }

    @SuppressLint("SetTextI18n")
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
                            content = content.replaceAll("<[^>]+>", "");
                            content = content.replaceAll("(?i)Antigravity-cli", "");
                            content = content.replaceAll("(?i)Antigravity cli", "");
                            content = content.replaceAll("(?i)Antigravity", "");
                            content = content.trim();
                            content = content.replaceAll("^[\\s\\r\\n\\t]+|[\\s\\r\\n\\t]+$", "");
                            if (content.length() > 18) {
                                return content.substring(0, 18) + "...";
                            }
                            return content;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getDynamicSessionTitle(TerminalSession sessionAtRow, int position) {
        String name = sessionAtRow.mSessionName;
        String sessionTitle = sessionAtRow.getTitle();
        
        String uuid = null;
        com.termux.shared.termux.shell.command.runner.terminal.TermuxSession ts = null;
        try {
            com.termux.app.TermuxService service = mActivity.getTermuxService();
            if (service != null) {
                ts = service.getTermuxSessionForTerminalSession(sessionAtRow);
                if (ts != null) {
                    String[] args = ts.getExecutionCommand().arguments;
                    if (args != null) {
                        for (String arg : args) {
                            if (arg != null && arg.startsWith("--conversation=")) {
                                uuid = arg.substring("--conversation=".length());
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        String cleanTitle = null;
        if (uuid != null) {
            String dirPath = "/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/.gemini/antigravity-cli/brain/" + uuid;
            File customTitleFile = new File(dirPath, "custom_title.txt");
            if (customTitleFile.exists()) {
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(customTitleFile))) {
                    cleanTitle = r.readLine();
                } catch (Exception ignored) {}
            }
            if (cleanTitle == null || cleanTitle.isEmpty()) {
                File transcriptFile = new File(dirPath, ".system_generated/logs/transcript.jsonl");
                if (transcriptFile.exists()) {
                    cleanTitle = extractTopicFromTranscript(transcriptFile);
                }
            }
        }

        if (cleanTitle == null || cleanTitle.isEmpty()) {
            String sessionNamePart = (TextUtils.isEmpty(name) ? "" : name);
            String sessionTitlePart = (TextUtils.isEmpty(sessionTitle) ? "" : ((sessionNamePart.isEmpty() ? "" : "\n") + sessionTitle));
            cleanTitle = sessionNamePart + sessionTitlePart;
            if (cleanTitle.isEmpty()) {
                cleanTitle = "会话";
            }
        }
        
        return "🟢 [" + (position + 1) + "] " + cleanTitle;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View sessionRowView = convertView;
        if (sessionRowView == null) {
            LayoutInflater inflater = mActivity.getLayoutInflater();
            sessionRowView = inflater.inflate(R.layout.item_terminal_sessions_list, parent, false);
        }

        TextView sessionTitleView = sessionRowView.findViewById(R.id.session_title);

        TerminalSession sessionAtRow = getItem(position).getTerminalSession();
        if (sessionAtRow == null) {
            sessionTitleView.setText("null session");
            return sessionRowView;
        }

        boolean shouldEnableDarkTheme = ThemeUtils.shouldEnableDarkTheme(mActivity, NightMode.getAppNightMode().getName());

        sessionTitleView.setBackground(
            ContextCompat.getDrawable(mActivity, R.drawable.session_background_black_selected)
        );

        String fullSessionTitle = getDynamicSessionTitle(sessionAtRow, position);
        SpannableString fullSessionTitleStyled = new SpannableString(fullSessionTitle);
        // Styled without italic style (use normal style)
        fullSessionTitleStyled.setSpan(boldSpan, 0, fullSessionTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        sessionTitleView.setText(fullSessionTitleStyled);

        boolean sessionRunning = sessionAtRow.isRunning();

        if (sessionRunning) {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            sessionTitleView.setPaintFlags(sessionTitleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        int defaultColor = Color.WHITE;
        int color = sessionRunning || sessionAtRow.getExitStatus() == 0 ? defaultColor : Color.RED;
        sessionTitleView.setTextColor(color);
        return sessionRowView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TermuxSession clickedSession = getItem(position);
        mActivity.getTermuxTerminalSessionClient().setCurrentSession(clickedSession.getTerminalSession());
        mActivity.getDrawer().closeDrawers();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final TermuxSession selectedSession = getItem(position);
        new android.app.AlertDialog.Builder(mActivity)
            .setTitle("会话操作")
            .setItems(new CharSequence[]{"重命名", "关闭并删除会话"}, (dialog, which) -> {
                if (which == 0) {
                    mActivity.getTermuxTerminalSessionClient().renameSession(selectedSession.getTerminalSession());
                } else if (which == 1) {
                    new android.app.AlertDialog.Builder(mActivity)
                        .setTitle("确认关闭")
                        .setMessage("确定要关闭并删除当前会话吗？")
                        .setPositiveButton(android.R.string.yes, (dialog2, which2) -> {
                            selectedSession.getTerminalSession().finishIfRunning();
                            mActivity.getTermuxTerminalSessionClient().removeFinishedSession(selectedSession.getTerminalSession());
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                }
            })
            .show();
        return true;
    }

}
