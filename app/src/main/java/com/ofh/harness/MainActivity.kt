package com.ofh.harness

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/** OFH launcher: entry point into the harness. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // First run: no provider configured yet, so route through the setup wizard.
        if (ProviderConfig.readCurrent(this) == null) {
            startActivity(Intent(this, SetupWizardActivity::class.java))
            finish()
            return
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 0)
        }
        root.addView(TextView(this).apply {
            text = "🌲 Open Forest Harness"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Independent agent harness · provider-agnostic · local-first"
            textSize = 14f
            setPadding(0, 8, 0, 24)
        })
        root.addView(MaterialButton(this).apply {
            text = "Open Chat"
            setOnClickListener { startActivity(Intent(this@MainActivity, WebViewActivity::class.java)) }
        })
        setContentView(root)
    }
}
