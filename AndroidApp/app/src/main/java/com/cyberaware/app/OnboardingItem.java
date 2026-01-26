package com.cyberaware.app;

public class OnboardingItem {
    int animationRes; // অ্যানিমেশন রিসোর্স আইডি (যেমন R.raw.scan_anim)
    String title;
    String description;

    public OnboardingItem(int animationRes, String title, String description) {
        this.animationRes = animationRes;
        this.title = title;
        this.description = description;
    }

    public int getAnimationRes() { return animationRes; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}