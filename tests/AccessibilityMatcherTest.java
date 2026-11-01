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
        check(AccessibilityMatcher.uniqueButton(huge, "수원시") == null);
        AccessibilityNodeInfo cities = node("충전하실 지역을 선택해 주세요", false);
        AccessibilityNodeInfo suwon = node("수원시", true), hwaseong = node("화성시", true);
        cities.add(suwon).add(node("용인시", true)).add(hwaseong);
        check(AccessibilityMatcher.uniqueButton(cities, "화성시") == hwaseong);
        System.out.println("9 button matching checks passed");
    }
}
