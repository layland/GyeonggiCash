package kr.local.timedclick;

import android.view.accessibility.AccessibilityNodeInfo;

public class AccessibilityMatcherTest {
    private static AccessibilityNodeInfo node(String text, boolean click) { return new AccessibilityNodeInfo(text, click); }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        AccessibilityNodeInfo root = node("", false), button = node("", true);
        button.add(node("빠른 충전\n바로가기 ›", false)); root.add(button);
        check(AccessibilityMatcher.uniqueButton(root, AccessibilityMatcher.QUICK) == button);
        // Multiple text leaves belonging to the same clickable button remain unambiguous.
        button.add(node("빠른 충전 바로가기", false));
        check(AccessibilityMatcher.uniqueButton(root, AccessibilityMatcher.QUICK) == button);
        AccessibilityNodeInfo duplicate = node("빠른 충전 바로가기", true); root.add(duplicate);
        check(AccessibilityMatcher.uniqueButton(root, AccessibilityMatcher.QUICK) == null);
        duplicate.visible = false;
        check(AccessibilityMatcher.uniqueButton(root, AccessibilityMatcher.QUICK) == button);
        button.enabled = false;
        check(AccessibilityMatcher.uniqueButton(root, AccessibilityMatcher.QUICK) == null);
        check(!AccessibilityMatcher.regionSheet(node("수원시", true)));
        check(AccessibilityMatcher.regionSheet(node("충전하실 지역을\n선택해 주세요", false)));
        AccessibilityNodeInfo huge = node("수원시", true);
        for (int i = 0; i < 2001; i++) huge.add(node("", false));
        check(AccessibilityMatcher.uniqueButton(huge, AccessibilityMatcher.CITY) == null);
        System.out.println("8 button matching checks passed");
    }
}
