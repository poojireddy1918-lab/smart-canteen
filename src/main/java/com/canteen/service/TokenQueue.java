package com.canteen.service;

import com.canteen.dao.TokenDAO;
import com.canteen.model.Token;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * FIFO queue of WAITING tokens: first order placed is the first served.
 * The database is the source of truth; reload() re-syncs the queue from it.
 */
public final class TokenQueue {

    private static final TokenQueue INSTANCE = new TokenQueue();

    private final Deque<Token> queue = new ArrayDeque<>();

    private TokenQueue() {}

    public static TokenQueue get() { return INSTANCE; }

    public synchronized void reload() throws SQLException {
        queue.clear();
        queue.addAll(TokenDAO.loadWaiting());
    }

    /** Add a new token at the back of the line. */
    public synchronized void enqueue(Token t) {
        queue.addLast(t);
    }

    /** Take the token at the front, mark it served in the database, and return it (null if empty). */
    public synchronized Token serveNext() throws SQLException {
        Token head = queue.peekFirst();
        if (head == null) return null;
        TokenDAO.markServed(head);
        queue.pollFirst();
        return head;
    }

    /** 1-based position in the line, or -1 if the token is not waiting (already served). */
    public synchronized int positionOf(int tokenId) {
        int pos = 1;
        for (Token t : queue) {
            if (t.getTokenId() == tokenId) return pos;
            pos++;
        }
        return -1;
    }

    public synchronized List<Token> snapshot() { return new ArrayList<>(queue); }

    public synchronized int size() { return queue.size(); }
}
