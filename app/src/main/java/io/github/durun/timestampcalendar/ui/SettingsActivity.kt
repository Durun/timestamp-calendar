package io.github.durun.timestampcalendar.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import io.github.durun.timestampcalendar.R
import io.github.durun.timestampcalendar.databinding.SettingsActivityBinding
import io.github.durun.timestampcalendar.libs.MyAuth

class SettingsActivity : AppCompatActivity() {
    lateinit var binding: SettingsActivityBinding
    lateinit var settingsFragment: SettingsFragment

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
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    fun selectSpreadSheet(view: View) {
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
                    .setTitle("ラジオボタンダイアログ")
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
                    Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT)
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
        println("Accessing Drive")
        val files = service.Files()
            .list()
            .setCorpora("user")
            .setOrderBy("viewedByMeTime desc")
            .setQ("mimeType='application/vnd.google-apps.spreadsheet'")
            .setPageSize(100)
            .execute()
            .files
        println("Received from Drive")
        return files.map { SheetEntry(it.name, it.id) }
    }
}