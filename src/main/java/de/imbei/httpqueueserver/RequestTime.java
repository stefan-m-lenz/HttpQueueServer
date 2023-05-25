package de.imbei.httpqueueserver;

// Encapsulates a request ID and the time when it has been received.
public class RequestTime {
    private final int requestId;
    private final long time;

    public RequestTime(int requestId, long time) {
        this.requestId = requestId;
        this.time = time;
    }

    public int getRequestId() {
        return requestId;
    }

    public long getTime() {
        return time;
    }

}
