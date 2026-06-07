package com.example.rssreader;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.rssreader.adapter.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupViewPager();
        setupBottomNavigation();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(true); // 允许滑动切换

        // ViewPager 页面改变时更新底部导航
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNavigation.setSelectedItemId(R.id.nav_home);
                        break;
                    case 1:
                        bottomNavigation.setSelectedItemId(R.id.nav_favorites);
                        break;
                    case 2:
                        bottomNavigation.setSelectedItemId(R.id.nav_feeds);
                        break;
                    case 3:
                        bottomNavigation.setSelectedItemId(R.id.nav_ai_summary);
                        break;
                }
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (id == R.id.nav_favorites) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (id == R.id.nav_feeds) {
                viewPager.setCurrentItem(2, true);
                return true;
            } else if (id == R.id.nav_ai_summary) {
                viewPager.setCurrentItem(3, true);
                return true;
            }
            return false;
        });
    }
}
