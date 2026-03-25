package com.shadowcontacts.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.shadowcontacts.app.BuildConfig
import com.shadowcontacts.app.R
import com.shadowcontacts.app.databinding.ActivityInstructionsBinding

class InstructionsActivity : AppCompatActivity() {

    companion object {
        const val GITHUB_URL = "https://github.com/amishounak/ShadowContacts"
    }

    private lateinit var binding: ActivityInstructionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Instructions"

        setupFlavorVisibility()
    }

    private fun setupFlavorVisibility() {
        if (BuildConfig.HAS_CALLER_ID) {
            // Full version: show caller ID sections, hide promo card
            findViewById<View>(R.id.cardCallerId).visibility = View.VISIBLE
            findViewById<View>(R.id.cardPermissions).visibility = View.VISIBLE
            findViewById<View>(R.id.cardRecommended).visibility = View.VISIBLE
            findViewById<View>(R.id.cardFullVersion).visibility = View.GONE
        } else {
            // Play Store version: hide caller ID sections, show promo card
            findViewById<View>(R.id.cardCallerId).visibility = View.GONE
            findViewById<View>(R.id.cardPermissions).visibility = View.GONE
            findViewById<View>(R.id.cardRecommended).visibility = View.GONE
            findViewById<View>(R.id.cardFullVersion).visibility = View.VISIBLE

            // Make GitHub link clickable
            findViewById<View>(R.id.tvGitHubLink).setOnClickListener {
                openGitHub()
            }
            findViewById<View>(R.id.tvGitHubUrl).setOnClickListener {
                openGitHub()
            }
        }
    }

    private fun openGitHub() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
        } catch (_: Exception) { }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
