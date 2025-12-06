package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.network.UpdateChecker;
import net.minecraft.util.Util;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class UpdateLogScreen extends WindowScreen {
    private boolean loaded = false;

    public UpdateLogScreen(GuiTheme theme) {
        super(theme, "Update Log");
        
        locked = true;
        lockedAllowClose = true;

        if (!UpdateChecker.hasChecked()) {
            UpdateChecker.check(() -> {
                taskAfterRender = this::populateContent;
            });
        } else {
            taskAfterRender = this::populateContent;
        }
    }

    public static void open() {
        MeteorExecutor.execute(() -> {
            mc.execute(() -> mc.setScreen(new UpdateLogScreen(GuiThemes.get())));
        });
    }

    public static void checkAndShow() {
        UpdateChecker.check(() -> {
            if (UpdateChecker.isUpdateAvailable()) {
                mc.execute(() -> mc.setScreen(new UpdateLogScreen(GuiThemes.get())));
            }
        });
    }

    @Override
    public void initWidgets() {
    }

    private void populateContent() {
        if (loaded) return;
        loaded = true;

        UpdateChecker.ReleaseInfo release = UpdateChecker.getLatestRelease();
        UpdateChecker.CommitInfo[] commits = UpdateChecker.getLatestCommits();

        if (release != null) {
            WSection releaseSection = add(theme.section("Latest Release: " + release.name, true)).expandX().widget();
            
            WHorizontalList releaseInfo = releaseSection.add(theme.horizontalList()).expandX().widget();
            releaseInfo.add(theme.label("Version: ")).widget().color = theme.textSecondaryColor();
            releaseInfo.add(theme.label(release.tagName)).expandX();
            
            if (release.publishedAt != null) {
                try {
                    String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(release.publishedAt));
                    WHorizontalList dateRow = releaseSection.add(theme.horizontalList()).expandX().widget();
                    dateRow.add(theme.label("Released: ")).widget().color = theme.textSecondaryColor();
                    dateRow.add(theme.label(date)).expandX();
                } catch (Exception ignored) {}
            }
            
            releaseSection.add(theme.horizontalSeparator()).padVertical(theme.scale(4)).expandX();
            
            if (release.body != null && !release.body.isEmpty()) {
                String[] lines = release.body.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        releaseSection.add(theme.label(formatLine(line))).expandX();
                    }
                }
            }
            
            releaseSection.add(theme.horizontalSeparator()).padVertical(theme.scale(4)).expandX();
            
            WHorizontalList buttons = releaseSection.add(theme.horizontalList()).expandX().widget();
            
            WButton viewButton = buttons.add(theme.button("View on GitHub")).expandX().widget();
            viewButton.action = () -> Util.getOperatingSystem().open(release.htmlUrl);
            
            if (release.assets != null && !release.assets.isEmpty()) {
                for (UpdateChecker.AssetInfo asset : release.assets) {
                    if (asset.name.endsWith(".jar")) {
                        WButton downloadButton = buttons.add(theme.button("Download")).expandX().widget();
                        downloadButton.action = () -> Util.getOperatingSystem().open(asset.downloadUrl);
                        break;
                    }
                }
            }
        }
        
        if (commits != null && commits.length > 0) {
            add(theme.horizontalSeparator()).padVertical(theme.scale(8)).expandX();
            
            WSection commitsSection = add(theme.section("Recent Commits", true)).expandX().widget();
            
            WTable table = commitsSection.add(theme.table()).expandX().widget();
            table.horizontalSpacing = 0;
            
            int count = 0;
            for (UpdateChecker.CommitInfo commit : commits) {
                if (count >= 10) break;
                
                if (commit.commit != null && commit.commit.author != null && commit.commit.author.date != null) {
                    try {
                        String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(commit.commit.author.date));
                        table.add(theme.label(date)).top().right().widget().color = theme.textSecondaryColor();
                    } catch (Exception e) {
                        table.add(theme.label("--")).top().right().widget().color = theme.textSecondaryColor();
                    }
                } else {
                    table.add(theme.label("--")).top().right().widget().color = theme.textSecondaryColor();
                }
                
                String message = getCommitMessage(commit);
                table.add(theme.label(" - " + message)).widget().action = () -> {
                    if (commit.htmlUrl != null) {
                        Util.getOperatingSystem().open(commit.htmlUrl);
                    }
                };
                table.row();
                
                count++;
            }
        }
        
        add(theme.horizontalSeparator()).padVertical(theme.scale(8)).expandX();
        
        WHorizontalList bottomButtons = add(theme.horizontalList()).expandX().widget();
        
        WButton markSeenButton = bottomButtons.add(theme.button("Mark as Seen")).expandX().widget();
        markSeenButton.action = () -> {
            UpdateChecker.markAsSeen();
            close();
        };
        
        WButton closeButton = bottomButtons.add(theme.button("Close")).expandX().widget();
        closeButton.action = this::close;
        
        locked = false;
    }

    private String formatLine(String line) {
        line = line.trim();
        if (line.startsWith("- ")) {
            return "• " + line.substring(2);
        }
        if (line.startsWith("* ")) {
            return "• " + line.substring(2);
        }
        if (line.startsWith("# ")) {
            return line.substring(2);
        }
        if (line.startsWith("## ")) {
            return line.substring(3);
        }
        if (line.startsWith("### ")) {
            return line.substring(4);
        }
        return line;
    }

    private String getCommitMessage(UpdateChecker.CommitInfo commit) {
        if (commit.commit == null || commit.commit.message == null) {
            return "No message";
        }
        
        StringBuilder sb = new StringBuilder();
        String message = commit.commit.message;

        for (int i = 0; i < message.length(); i++) {
            if (i >= 60) {
                sb.append("...");
                break;
            }

            char c = message.charAt(i);
            if (c == '\n') {
                sb.append("...");
                break;
            }

            sb.append(c);
        }

        return sb.toString();
    }
}
