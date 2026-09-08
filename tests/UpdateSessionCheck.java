import com.group_finity.mascot.chat.ConversationHistory;
import com.group_finity.mascot.chat.LongTermMemoryStore;
import java.io.File;

public class UpdateSessionCheck {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        LongTermMemoryStore store = new LongTermMemoryStore(new File(dir, "update_check.db"));
        store.initialize(dir);

        ConversationHistory h = new ConversationHistory(50);
        h.addUser("第一句话");
        h.addAssistant("回复一");

        String id = store.updateSession("sess_1", h.getMessages(), null);
        if (!"sess_1".equals(id)) throw new AssertionError("create failed: " + id);

        h.addUser("第二句话");
        h.addAssistant("回复二");
        String id2 = store.updateSession("sess_1", h.getMessages(), null);
        if (!"sess_1".equals(id2)) throw new AssertionError("update failed: " + id2);

        String id3 = store.updateSession("sess_1", h.getMessages(), null);
        if (!"sess_1".equals(id3)) throw new AssertionError("same-content update failed: " + id3);

        // 跨会话去重：不同 ID 但内容完全相同，应复用已有会话而不是新建一行
        String id4 = store.updateSession("sess_2", h.getMessages(), null);
        if (!"sess_1".equals(id4)) throw new AssertionError("cross-session dedup failed: " + id4);

        java.sql.Connection c = java.sql.DriverManager.getConnection(store.getJdbcUrl());
        java.sql.Statement s = c.createStatement();
        java.sql.ResultSet rs = s.executeQuery(
            "SELECT message_count FROM memory_sessions WHERE session_id='sess_1'");
        rs.next();
        if (rs.getInt(1) != 4) throw new AssertionError("message_count=" + rs.getInt(1));
        rs = s.executeQuery(
            "SELECT COUNT(*) FROM memory_sessions WHERE content_hash="
            + "(SELECT content_hash FROM memory_sessions WHERE session_id='sess_1')");
        rs.next();
        if (rs.getInt(1) != 1) throw new AssertionError("dedup hash rows=" + rs.getInt(1));
        rs = s.executeQuery(
            "SELECT COUNT(*) FROM conversation_messages WHERE session_id='sess_1'");
        rs.next();
        if (rs.getInt(1) != 4) throw new AssertionError("message rows=" + rs.getInt(1));
        rs.close(); s.close(); c.close();
        store.close();
        System.out.println("UPDATE_SESSION_OK");
    }
}
