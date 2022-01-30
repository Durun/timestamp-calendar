package io.github.durun.timestampcalendar.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import io.github.durun.timestampcalendar.R
import io.github.durun.timestampcalendar.databinding.SettingsActivityBinding
import io.github.durun.timestampcalendar.libs.DataSheet
import io.github.durun.timestampcalendar.libs.MyAuth
import io.github.durun.timestampcalendar.libs.sheetsService

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsActivityBinding
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var auth: MyAuth
    private val startSignIn =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                auth.handleSignInResult(data)
                Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            settingsFragment = SettingsFragment()
            supportFragmentManager
                .beginTransaction()
                .replace(binding.settings.id, settingsFragment)
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        auth = MyAuth(this)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    fun loginToGoogle(view: View) {
        startSignIn.launch(auth.signInIntent())
    }

    fun createSpreadSheet(view: View) {
        // サインインしてなければ中止
        if (!auth.isSignedIn()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val progress = ProgressDialog(this)
            .apply {
                title = "Creating Data SpreadSheet"
                setProgressStyle(ProgressDialog.STYLE_SPINNER)
            }
        progress.show()
        Thread {
            Looper.prepare()
            val sheetId = DataSheet.getIdOrNull(auth.credential)
            if (sheetId != null) {
                // シートが見つかった
                runOnUiThread {
                    settingsFragment
                        .findPreference<EditTextPreference>("spread_sheet_id")
                        ?.text = sheetId
                    Toast.makeText(this, "${DataSheet.title} already exists", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                sheetsService(auth.credential).Spreadsheets()
                    .create(DataSheet.newSheet())
                    .execute()
                val id = DataSheet.getIdOrNull(auth.credential)
                    ?: kotlin.run {
                        progress.dismiss()
                        return@Thread
                    }
                runOnUiThread {
                    settingsFragment
                        .findPreference<EditTextPreference>("spread_sheet_id")
                        ?.text = id
                    Toast.makeText(this, "Created Sheet", Toast.LENGTH_SHORT).show()
                }
            }
            progress.dismiss()
        }.start()
    }

    fun selectSpreadSheet(view: View) {
        // サインインしてなければ中止
        if (!auth.isSignedIn()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val progress = ProgressDialog(this)
            .apply {
                setCancelable(true)
                setTitle("Loading sheet list")
                setProgressStyle(ProgressDialog.STYLE_SPINNER)
            }

        val thread = Thread {
            Looper.prepare()
            val sheets = getSpreadSheetList()
            val items = sheets.map { it.name }.toTypedArray()
            var selected: Int? = null

            progress.dismiss()
            runOnUiThread {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Select Data Sheet")
                    // 表示アイテムを指定する //
                    .setSingleChoiceItems(items, -1) { _, i ->
                        selected = i
                    }
                    // 決定・キャンセル用にボタンも配置 //
                    .setPositiveButton("OK") { _, _ ->
                        if (selected != null) {
                            settingsFragment
                                .findPreference<EditTextPreference>("spread_sheet_id")
                                ?.text = sheets[selected!!].id
                        }
                    }
                    .setNeutralButton("Cancel") { _, _ -> }
                    .create()
                dialog.show()
            }
        }
        progress.show()
        thread.start()
    }

    data class SheetEntry(val name: String, val id: String)

    private fun getSpreadSheetList(): List<SheetEntry> {
        val service = MyAuth(this)
            .let {
                it.signInLastAccount()
                if (!it.isSignedIn()) {
                    Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
                    return emptyList()
                }
                println("Account: ${it.credential.selectedAccount.name}")
                Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    it.credential
                )
                    .setApplicationName("Timestamp Calendar")
                    .build()
            }
        val files = service.Files()
            .list()
            .setCorpora("user")
            .setOrderBy("viewedByMeTime desc")
            .setQ("mimeType='application/vnd.google-apps.spreadsheet'")
            .setPageSize(100)
            .execute()
            .files
        return files.map { SheetEntry(it.name, it.id) }
    }
}