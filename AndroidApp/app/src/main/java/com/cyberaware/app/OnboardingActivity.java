package com.cyberaware.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter adapter;
    private ViewPager2 viewPager;
    private MaterialButton btnNext;
    private TextView tvIndicator, tvSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        tvSkip = findViewById(R.id.tvSkip);
        tvIndicator = findViewById(R.id.tvIndicator);

        setupOnboardingItems();

        // পেজ পাল্টালে কি হবে
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tvIndicator.setText((position + 1) + " / 3");

                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText("FINISH");
                } else {
                    btnNext.setText("NEXT");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < adapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                finishOnboarding();
            }
        });

        tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> items = new ArrayList<>();

        // Slide 1: Scan
        items.add(new OnboardingItem(
                R.raw.scan_anim,
                "AI Powered Shield",
                "Advanced AI algorithms scan every message to detect phishing and scams instantly."
        ));

        // Slide 2: Security (Lock) - নিশ্চিত করুন lock_anim.json আছে
        // ফাইল না থাকলে R.raw.scan_anim ব্যবহার করুন টেম্পোরারিভাবে
        items.add(new OnboardingItem(
                R.raw.lock_anim, // পরিবর্তন করে R.raw.lock_anim দিন
                "Ironclad Security",
                "Your data remains encrypted and secure on your device. No privacy compromise."
        ));

        // Slide 3: Cloud (Report) - নিশ্চিত করুন cloud_anim.json আছে
        items.add(new OnboardingItem(
                R.raw.cloud_anim, // পরিবর্তন করে R.raw.cloud_anim দিন
                "Community Protection",
                "Report threats to the cloud intelligence network and protect others."
        ));

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);
    }

    private void finishOnboarding() {
        // মেইন অ্যাপে না পাঠিয়ে লগইন পেজে পাঠানো হচ্ছে
        startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        finish();
    }
}