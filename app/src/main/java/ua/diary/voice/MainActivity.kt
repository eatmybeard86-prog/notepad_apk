package ua.diary.voice

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.diary.voice.ui.DiaryTheme
import ua.diary.voice.ui.DiaryViewModel
import ua.diary.voice.ui.EntryScreen
import ua.diary.voice.ui.HomeScreen
import ua.diary.voice.ui.LockScreen

class MainActivity : AppCompatActivity() {
    /** Відбиток або, як запасний варіант, PIN/графічний ключ телефона. */
    private val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private var lockEnabled by mutableStateOf(true)
    private var unlocked by mutableStateOf(false)
    private var prompting = false

    private fun canAuthenticate() =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    private val needsLock get() = lockEnabled && !unlocked && canAuthenticate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lockEnabled = prefs.getBoolean(KEY_LOCK, true)
        unlocked = savedInstanceState?.getBoolean(KEY_UNLOCKED) ?: false
        applySecureFlag()

        setContent {
            DiaryTheme {
                if (needsLock) {
                    LockScreen(onUnlock = ::authenticate)
                } else {
                    val vm: DiaryViewModel = viewModel()
                    val editor by vm.editor.collectAsStateWithLifecycle()
                    val state = editor
                    if (state == null) {
                        HomeScreen(vm, lockEnabled = lockEnabled, onToggleLock = ::setLock)
                    } else {
                        BackHandler { vm.closeEditor() }
                        EntryScreen(vm, state)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (needsLock) authenticate()
    }

    override fun onStop() {
        super.onStop()
        // Після виходу з додатка знову просимо відбиток
        if (!isChangingConfigurations && lockEnabled) unlocked = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_UNLOCKED, unlocked)
    }

    private fun authenticate() {
        if (prompting) return
        prompting = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    prompting = false
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    prompting = false
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Щоденник")
            .setSubtitle("Прикладіть палець, щоб відкрити")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private fun setLock(enabled: Boolean) {
        if (enabled && !canAuthenticate()) {
            Toast.makeText(this, "Спочатку налаштуйте відбиток або PIN у налаштуваннях телефона", Toast.LENGTH_LONG).show()
            return
        }
        lockEnabled = enabled
        unlocked = true
        prefs.edit().putBoolean(KEY_LOCK, enabled).apply()
        applySecureFlag()
    }

    /** Коли блокування увімкнене, ховаємо вміст у списку останніх додатків і забороняємо скриншоти. */
    private fun applySecureFlag() {
        if (lockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    companion object {
        private const val KEY_LOCK = "lock_enabled"
        private const val KEY_UNLOCKED = "unlocked"
    }
}
