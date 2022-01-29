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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.durun.timestampcalendar.R
import io.github.durun.timestampcalendar.libs.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PREFERENCES_FILE_NAME = "preferences.txt"
        private const val TAG = "MainActivity"
    }

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
        setContentView(R.layout.activity_main)
        preferences = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE)
        rows = preferences.loadRows()
        auth = MyAuth(this)

        // リスト
        val rowsAdapter = MyAdapter(rows)
        recyclerView.apply {
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
            it.attachToRecyclerView(recyclerView)
        }

        // リスト追加ボタン
        addButton.setOnClickListener {
            val newText = textInput.text?.toString() ?: return@setOnClickListener
            textInput.setText("")
            rows.addRowDataIfNotBlank(newText)
            rowsAdapter.notifyItemInserted(rows.lastIndex)
        }

        // 設定ボタン -> 設定画面へ遷移
        configButton.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        preferences.saveRows(rows)
    }

    class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val handle: ImageButton = itemView.findViewById(R.id.handle)
        val textView: TextView = itemView.findViewById(R.id.rowTextView)
        val sendButton: ImageButton = itemView.findViewById(R.id.sendButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    // リストの行
    inner class MyAdapter(
        private val rows: MutableList<RowData>
    ) : RecyclerView.Adapter<MyHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            val inflated = LayoutInflater.from(parent.context)
                .inflate(R.layout.row, parent, false)
            return MyHolder(inflated)
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            // ハンドル
            holder.handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder)
                }
                false
            }

            // テキスト
            holder.textView.text = rows[position].text

            // ゴミ箱ボタン
            holder.deleteButton.setOnClickListener {
                //Toast.makeText(applicationContext, "Deleting rows[$position]", Toast.LENGTH_SHORT).show()
                rows.removeAt(position)
                notifyItemRemoved(position)
                notifyDataSetChanged()
            }

            // 送信ボタン
            holder.sendButton.setOnClickListener {
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