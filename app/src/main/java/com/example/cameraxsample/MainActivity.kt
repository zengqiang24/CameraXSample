package com.example.cameraxsample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cameraxsample.camera2.Camera2Activity
import kotlinx.android.synthetic.main.activity_main.*
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rl_camera_entrance.layoutManager = LinearLayoutManager(this)

        val adapter = EntranceRecycleViewAdapter(configEntrances()) {
            val intent = Intent(this.application, it.clazz)
            this.startActivity(intent)
        }
        rl_camera_entrance.adapter = adapter

    }

    private fun configEntrances(): MutableList<EntranceData> = arrayListOf(
        EntranceData(CameraXActivity::class.java, "CameraX API"),
        EntranceData(Camera2Activity::class.java, "Camera2 API")
    )

    private data class EntranceData(val clazz: Class<*>, val name: String)

    private class EntranceRecycleViewAdapter(
        private val entranceList: MutableList<EntranceData>,
        private val onClick: (EntranceData) -> Unit
    ) :
        RecyclerView.Adapter<EntranceRecycleViewAdapter.EntranceRLViewHolder>() {

        class EntranceRLViewHolder(itemView: View, onClick: (EntranceData) -> Unit) :
            RecyclerView.ViewHolder(itemView) {
            val demoName: TextView = itemView.findViewById(R.id.tv_use_case_name)
            var currentEntranceData: EntranceData? = null

            init { // when does init block code can be executed?
                itemView.setOnClickListener {
                    currentEntranceData?.let {
                        onClick(it)
                    }
                }
            }

            fun bind(entranceData: EntranceData) {
                this.currentEntranceData = entranceData
                demoName.text = entranceData.name
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntranceRLViewHolder {
            return EntranceRLViewHolder(parent, onClick)
        }

        override fun onBindViewHolder(holder: EntranceRLViewHolder, position: Int) {

        }

        override fun getItemCount(): Int {
            return entranceList.size
        }

    }

    companion object {
        private const val TAG = "MainActivity"
    }
}