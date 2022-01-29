package io.github.durun.timestampcalendar.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.durun.timestampcalendar.databinding.ActivityMainBinding
import io.github.durun.timestampcalendar.databinding.RowBinding
import io.github.durun.timestampcalendar.libs.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val PREFERENCES_FILE_NAME = "preferences.txt"
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var preferences: SharedPreferences
    private lateinit var rows: RowDataList
    private lateinit var auth: MyAuth

    private val startSignIn =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) auth.handleSignInResult(
                data
            )
            Log.i(TAG, "Signed in: ${auth.credential.selectedAccount?.name}")
            Toast.makeText(
                applicationContext,
                "Signed in: ${auth.credential.selectedAccount?.name}",
                Toast.LENGTH_SHORT
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE)
        rows = preferences.loadRows()
        auth = MyAuth(this)

        // リスト
        val rowsAdapter = MyAdapter(rows)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = rowsAdapter
        }

        // リストのドラッグ処理
        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.ACTION_STATE_IDLE
        ) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                rows.move(fromPos, toPos)
                rowsAdapter.notifyItemMoved(fromPos, toPos)
                rowsAdapter.notifyDataSetChanged()
                return true
            }
        }).also {
            it.attachToRecyclerView(binding.recyclerView)
        }

        // リスト追加ボタン
        binding.addButton.setOnClickListener {
            val newText = binding.textInput.text?.toString() ?: return@setOnClickListener
            binding.textInput.setText("")
            rows.addRowDataIfNotBlank(newText)
            rowsAdapter.notifyItemInserted(rows.lastIndex)
        }

        // 設定ボタン -> 設定画面へ遷移
        binding.configButton.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        preferences.saveRows(rows)
    }

    class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = RowBinding.bind(itemView)
    }

    // リストの行
    inner class MyAdapter(
        private val rows: MutableList<RowData>
    ) : RecyclerView.Adapter<MyHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            val binding = RowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyHolder(binding.root)
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            // ハンドル
            holder.binding.handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder)
                }
                false
            }

            // テキスト
            holder.binding.rowTextView.text = rows[position].text

            // ゴミ箱ボタン
            holder.binding.deleteButton.setOnClickListener {
                //Toast.makeText(applicationContext, "Deleting rows[$position]", Toast.LENGTH_SHORT).show()
                rows.removeAt(position)
                notifyItemRemoved(position)
                notifyDataSetChanged()
            }

            // 送信ボタン
            holder.binding.sendButton.setOnClickListener {
                val row = rows[position]
                if (auth.isSignedIn()) {
                    // サインイン済み
                    val intent = Intent(this@MainActivity, SendRowService::class.java)
                        .setAction(Intent.ACTION_SEND)
                        .putExtra(RowData.INTENT_KEY, row)
                    // SendRowServiceを起動
                    startService(intent)
                } else {
                    // Googleにサインイン
                    startSignIn.launch(auth.signInIntent())
                }
            }
        }

        override fun getItemCount(): Int = rows.size
    }
}