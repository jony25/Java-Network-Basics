public interface NetworkListener {
    void onMessageReceived(String user, String message);
    void onAuthResult(boolean success, String message);
    void onSystemMessage(String message);
    void onUserPresence(String user, boolean isOnline);
    void onVoicePresence(String channel, String user, boolean joined);
    void onServerList(java.util.List<String> servers);
    void onServerInfo(String name, String owner, java.util.List<String> textChannels, java.util.List<String> voiceChannels, java.util.List<String> members);
    void onAvatarReceived(String user, String base64);
}
