package kr.local.timedclick;

import java.util.List;

/** Selects the top application, never a background target behind another app/dialog. */
public final class WindowPolicy {
    public static final int NO_APPLICATION = -1, OTHER_APPLICATION = -2, BLOCKED = -3, UNREADABLE = -4;
    public static final class Window {
        final boolean application, ownOverlay, focusedOther;
        final int layer;
        final String owner;
        public Window(boolean application, boolean ownOverlay, boolean focusedOther, int layer, String owner) {
            this.application = application; this.ownOverlay = ownOverlay;
            this.focusedOther = focusedOther; this.layer = layer; this.owner = owner;
        }
    }
    public static int select(List<Window> windows, String target) {
        int top = NO_APPLICATION;
        for (int i = 0; i < windows.size(); i++) {
            Window w = windows.get(i);
            if (w.ownOverlay) continue;
            if (!w.application && w.focusedOther) return BLOCKED;
            if (w.application && (top < 0 || w.layer > windows.get(top).layer)) top = i;
        }
        if (top < 0) return NO_APPLICATION;
        String owner = windows.get(top).owner;
        if (owner == null || owner.isEmpty()) return UNREADABLE;
        return owner.equals(target) ? top : OTHER_APPLICATION;
    }
    private WindowPolicy() {}
}
