public interface NetworkListener {
    void onMessageReceived(String user, String message);
    void onAuthResult(boolean success, String message);
    void onSystemMessage(String message);
    void onUserPresence(String user, boolean isOnline);
    void onVoicePresence(String channel, String user, boolean joined);
}
