package com.flore.instagramclone

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.flore.instagramclone.navigation.AlarmFragment
import com.flore.instagramclone.navigation.DetailViewFragment
import com.flore.instagramclone.navigation.GridFragment
import com.flore.instagramclone.navigation.UserFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when (p0.itemId) {
            R.id.home_menu -> {
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main, detailViewFragment).commit()
                return true
            }
            R.id.search_menu -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.content_main, gridFragment)
                    .commit()
                return true
            }

            R.id.add_photo_menu -> {

                return true
            }

            R.id.favorite_alarm_menu -> {
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.content_main, alarmFragment)
                    .commit()
                return true
            }

            R.id.account_menu -> {
                var userFragment = UserFragment()
                supportFragmentManager.beginTransaction().replace(R.id.content_main, userFragment)
                    .commit()
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottom_navigation.setOnNavigationItemSelectedListener(this)
    }
}