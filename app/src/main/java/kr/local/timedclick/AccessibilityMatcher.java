package kr.local.timedclick;

import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

final class AccessibilityMatcher {
    static final String QUICK = "빠른 충전 바로가기";
    static final String CITY = "수원시";
    private static final int LIMIT = 2000;

    static String normalized(CharSequence value) {
        return value == null ? "" : value.toString().replaceAll("\\s+", "")
            .replace("›", "").replace("〉", "").replace(">", "");
    }
    static boolean regionSheet(AccessibilityNodeInfo root) {
        if (root == null) return false;
        ArrayDeque<AccessibilityNodeInfo> pending = new ArrayDeque<>();
        pending.add(root);
        int visited = 0;
        while (!pending.isEmpty() && visited++ < LIMIT) {
            AccessibilityNodeInfo node = pending.removeFirst();
            if (node.isVisibleToUser() && (normalized(node.getText()).contains("충전하실지역")
                    || normalized(node.getContentDescription()).contains("충전하실지역"))) return true;
            children(node, pending);
        }
        return false;
    }
    static AccessibilityNodeInfo uniqueButton(AccessibilityNodeInfo root, String label) {
        if (root == null) return null;
        String expected = normalized(label);
        Set<AccessibilityNodeInfo> buttons = new LinkedHashSet<>();
        ArrayDeque<AccessibilityNodeInfo> pending = new ArrayDeque<>();
        pending.add(root);
        int visited = 0;
        while (!pending.isEmpty() && visited++ < LIMIT) {
            AccessibilityNodeInfo node = pending.removeFirst();
            if (node.isVisibleToUser() && (normalized(node.getText()).equals(expected)
                    || normalized(node.getContentDescription()).equals(expected))) {
                AccessibilityNodeInfo parent = node;
                for (int depth = 0; parent != null && depth < 20; depth++, parent = parent.getParent()) {
                    if (parent.isVisibleToUser() && parent.isEnabled() && parent.isClickable()) {
                        buttons.add(parent);
                        break;
                    }
                }
            }
            children(node, pending);
        }
        return pending.isEmpty() && buttons.size() == 1 ? buttons.iterator().next() : null;
    }
    private static void children(AccessibilityNodeInfo node, ArrayDeque<AccessibilityNodeInfo> pending) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) pending.addLast(child);
        }
    }
    private AccessibilityMatcher() {}
}
