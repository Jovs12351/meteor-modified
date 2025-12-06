package meteordevelopment.meteorclient.utils.network;

import com.google.gson.annotations.SerializedName;
import meteordevelopment.meteorclient.MeteorClient;

import java.io.*;
import java.net.http.HttpResponse;
import java.util.List;

public class UpdateChecker {
    private static final String GITHUB_REPO_OWNER = "Jovs12351";
    private static final String GITHUB_REPO_NAME = "meteor-modified";
    private static final String RELEASES_API_URL = "https://api.github.com/repos/%s/%s/releases";
    private static final String COMMITS_API_URL = "https://api.github.com/repos/%s/%s/commits";
    
    private static final String GITHUB_TOKEN = "YOUR_GITHUB_TOKEN_HERE";
    
    private static final File LAST_SEEN_FILE = new File(MeteorClient.FOLDER, "last_seen_update.txt");
    
    private static ReleaseInfo latestRelease;
    private static CommitInfo[] latestCommits;
    private static boolean updateAvailable = false;
    private static boolean checked = false;

    private static Http.Request authenticatedRequest(String url) {
        Http.Request request = Http.get(url);
        if (GITHUB_TOKEN != null && !GITHUB_TOKEN.isEmpty() && !GITHUB_TOKEN.equals("YOUR_GITHUB_TOKEN_HERE")) {
            request.bearer(GITHUB_TOKEN);
        }
        return request;
    }

    public static void check(Runnable onUpdateAvailable) {
        if (checked) {
            if (updateAvailable && onUpdateAvailable != null) {
                onUpdateAvailable.run();
            }
            return;
        }
        
        MeteorExecutor.execute(() -> {
            try {
                HttpResponse<ReleaseInfo[]> response = authenticatedRequest(String.format(RELEASES_API_URL, GITHUB_REPO_OWNER, GITHUB_REPO_NAME))
                    .sendJsonResponse(ReleaseInfo[].class);
                
                if (response.statusCode() == Http.SUCCESS && response.body() != null && response.body().length > 0) {
                    latestRelease = response.body()[0];
                    
                    String lastSeenTag = getLastSeenTag();
                    if (lastSeenTag == null || !lastSeenTag.equals(latestRelease.tagName)) {
                        updateAvailable = true;
                    }
                }
                
                HttpResponse<CommitInfo[]> commitsResponse = authenticatedRequest(String.format(COMMITS_API_URL + "?per_page=10", GITHUB_REPO_OWNER, GITHUB_REPO_NAME))
                    .sendJsonResponse(CommitInfo[].class);
                
                if (commitsResponse.statusCode() == Http.SUCCESS && commitsResponse.body() != null) {
                    latestCommits = commitsResponse.body();
                }
                
                checked = true;
                
                if (updateAvailable && onUpdateAvailable != null) {
                    onUpdateAvailable.run();
                }
            } catch (Exception e) {
                MeteorClient.LOG.error("Failed to check for updates", e);
            }
        });
    }
    
    public static ReleaseInfo getLatestRelease() {
        return latestRelease;
    }
    
    public static CommitInfo[] getLatestCommits() {
        return latestCommits;
    }
    
    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public static boolean hasChecked() {
        return checked;
    }
    
    public static void markAsSeen() {
        if (latestRelease != null) {
            saveLastSeenTag(latestRelease.tagName);
            updateAvailable = false;
        }
    }
    
    private static String getLastSeenTag() {
        if (!LAST_SEEN_FILE.exists()) return null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(LAST_SEEN_FILE))) {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }
    
    private static void saveLastSeenTag(String tag) {
        try {
            LAST_SEEN_FILE.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_SEEN_FILE))) {
                writer.write(tag);
            }
        } catch (IOException e) {
            MeteorClient.LOG.error("Failed to save last seen update tag", e);
        }
    }
    
    public static class ReleaseInfo {
        @SerializedName("tag_name")
        public String tagName;
        
        public String name;
        public String body;
        
        @SerializedName("html_url")
        public String htmlUrl;
        
        @SerializedName("published_at")
        public String publishedAt;
        
        public List<AssetInfo> assets;
        
        public Author author;
    }
    
    public static class AssetInfo {
        public String name;
        
        @SerializedName("browser_download_url")
        public String downloadUrl;
        
        public long size;
    }
    
    public static class Author {
        public String login;
        
        @SerializedName("avatar_url")
        public String avatarUrl;
    }
    
    public static class CommitInfo {
        public String sha;
        public CommitDetails commit;
        
        @SerializedName("html_url")
        public String htmlUrl;
        
        public Author author;
    }
    
    public static class CommitDetails {
        public String message;
        public CommitAuthor author;
    }
    
    public static class CommitAuthor {
        public String name;
        public String date;
    }
}
