package com.way.capture.utils;

/**
 * Created by android on 16-11-25.
 */

public final class RxEvent {
    private RxEvent() {
    }

    public static class NewPathEvent {
        public String path;
        public int type;

        public NewPathEvent(int type, String path) {
            this.type = type;
            this.path = path;
        }

    }
}
