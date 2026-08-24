package com.ofh.harness

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * OFH first-run provider setup wizard.
 *
 * Lets the user pick one of the [ProviderConfig] presets, optionally fill in the
 * API key, and overrides the model / base URL before persisting the selection
 * via [ProviderConfig.save] and launching the chat UI.
 */
class SetupWizardActivity : AppCompatActivity() {

    private var selectedRoute = ProviderConfig.providers.first().route
    private lateinit var modelInput: TextInputEditText
    private lateinit var baseUrlInput: TextInputEditText
    private lateinit var keyLayout: TextInputLayout
    private lateinit var keyInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Root scroll container so the form works on small screens.
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "🌲 Setup your provider"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Choose a provider to connect the harness to."
            textSize = 14f
            setPadding(0, 8, 0, 24)
        })

        // Provider list.
        val group = RadioGroup(this)
        group.orientation = RadioGroup.VERTICAL
        group.setOnCheckedChangeListener { _, checkedId ->
            val route = group.findViewById<MaterialRadioButton>(checkedId)?.tag as? String
            if (route != null) selectProvider(route)
        }
        ProviderConfig.providers.forEach { preset ->
            val hint = if (preset.needsKey) preset.keyHint else "no key needed"
            val rb = MaterialRadioButton(this).apply {
                tag = preset.route
                id = preset.route.hashCode()
                text = preset.label
            }
            group.addView(rb)
            group.addView(TextView(this).apply {
                text = hint
                textSize = 12f
                setPadding(56, 0, 0, 12)
            })
        }
        root.addView(group)

        // Model field.
        root.addView(label("Model"))
        val modelLayout = TextInputLayout(this).apply {
            isHintEnabled = false
            addView(TextInputEditText(this@SetupWizardActivity).also { modelInput = it })
        }
        root.addView(modelLayout)

        // Base URL field.
        root.addView(label("Base URL"))
        val baseUrlLayout = TextInputLayout(this).apply {
            isHintEnabled = false
            addView(TextInputEditText(this@SetupWizardActivity).also { baseUrlInput = it })
        }
        root.addView(baseUrlLayout)

        // API key field (only shown for providers that need one).
        keyLayout = TextInputLayout(this).apply {
            isHintEnabled = false
            addView(TextInputEditText(this@SetupWizardActivity).also { keyInput = it })
        }
        root.addView(keyLayout)

        root.addView(MaterialButton(this).apply {
            text = "Save & Continue"
            setOnClickListener { saveAndContinue() }
        })
        root.addView(TextView(this).apply {
            text = "You can change provider settings later from the chat screen."
            textSize = 12f
            setPadding(0, 16, 0, 0)
        })

        // Populate the first preset so the form starts filled in.
        selectProvider(selectedRoute)
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setPadding(0, 16, 0, 4)
    }

    private fun selectProvider(route: String) {
        selectedRoute = route
        val preset = ProviderConfig.preset(route)
        modelInput.setText(preset.defaultModel)
        baseUrlInput.setText(ProviderConfig.readBaseUrl(this, route))
        if (preset.needsKey) {
            keyLayout.visibility = ViewGroup.VISIBLE
            keyInput.setText(ProviderConfig.readSavedKey(this, route))
        } else {
            keyLayout.visibility = ViewGroup.GONE
            keyInput.setText("")
        }
    }

    private fun saveAndContinue() {
        val preset = ProviderConfig.preset(selectedRoute)
        val baseUrl = baseUrlInput.text?.toString()?.trim() ?: ""
        val model = modelInput.text?.toString()?.trim() ?: ""
        val key = keyInput.text?.toString()?.trim() ?: ""

        // Base URL is required for the custom preset.
        if (selectedRoute == "custom" && baseUrl.isEmpty()) {
            baseUrlInput.error = "Base URL is required for a custom provider"
            return
        }

        if (ProviderConfig.save(this, preset, baseUrl, model, key)) {
            startActivity(Intent(this, WebViewActivity::class.java))
            finish()
        }
    }
}
