package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

/** In-memory tree fixture for matcher behavior; not an Android device substitute. */
public class AccessibilityNodeInfo {
    public String text = "", description = "";
    public boolean visible = true, enabled = true, clickable;
    private AccessibilityNodeInfo parent;
    private final List<AccessibilityNodeInfo> children = new ArrayList<>();
    public AccessibilityNodeInfo(String text, boolean clickable) { this.text = text; this.clickable = clickable; }
    public AccessibilityNodeInfo add(AccessibilityNodeInfo child) { children.add(child); child.parent = this; return this; }
    public CharSequence getText() { return text; }
    public CharSequence getContentDescription() { return description; }
    public boolean isVisibleToUser() { return visible; }
    public boolean isEnabled() { return enabled; }
    public boolean isClickable() { return clickable; }
    public int getChildCount() { return children.size(); }
    public AccessibilityNodeInfo getChild(int index) { return children.get(index); }
    public AccessibilityNodeInfo getParent() { return parent; }
}
