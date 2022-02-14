package com.example.cameraxsample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cameraxsample.camera2.Camera2Activity
import com.example.cameraxsample.camerax.CameraXActivity
import com.example.cameraxsample.opengl.GLSurfaceViewActivity
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Entrance for features(Cameras2 API and CamerasX API)
 */
typealias OnItemClick = (MainActivity.EntranceData) -> Unit

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
//        EntranceData(CameraXActivity::class.java, "CameraX API"),
//        EntranceData(ShowAllCameraActivity::class.java, "All cameras"),
        EntranceData(Camera2Activity::class.java, "录制图像"),
//        EntranceData(GLSurfaceViewActivity::class.java, "Opengl ES"),
//        EntranceData(TestAppWidgetActivity::class.java, "TestAppWidget")
    )

    data class EntranceData(
        val clazz: Class<*>,  // Intent have to input a java class,  so there Class as its parameters rather then KClass.
        val name: String
    )

    private class EntranceRecycleViewAdapter(
        private val entranceList: MutableList<EntranceData>,
        private val onClick: OnItemClick
    ) :
        RecyclerView.Adapter<EntranceRecycleViewAdapter.EntranceRLViewHolder>() {

        class EntranceRLViewHolder(itemView: View, onClick: OnItemClick) :
            RecyclerView.ViewHolder(itemView) {
            val demoName: TextView = itemView.findViewById(R.id.tv_use_case_name)
            var currentEntranceData: EntranceData? = null

            init { // when does init block code can be executed?  init block have a higher priority than constructor.
                Log.d(TAG, "init called")
                itemView.setOnClickListener {
                    currentEntranceData?.let {
                        onClick(it)
                    }
                }
            }

            constructor(itemView: View, onClick: OnItemClick, name: String) : this(
                itemView,
                onClick
            ) {
                Log.d(
                    TAG,
                    "secondary constructor() called with: itemView = $itemView, onClick = $onClick, name = $name"
                )
            }

            fun bind(entranceData: EntranceData) {
                this.currentEntranceData = entranceData
                demoName.text = entranceData.name
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntranceRLViewHolder {
            //create view holder to control view
            val contentView = LayoutInflater.from(parent.context)
                .inflate(R.layout.entrance_itemview, parent, false)
            return EntranceRLViewHolder(contentView, onClick, "name")
        }

        override fun onBindViewHolder(holder: EntranceRLViewHolder, position: Int) {
            //input data to view holder as model
            holder.bind(entranceList[position])
        }

        override fun getItemCount(): Int {
            return entranceList.size
        }

    }

    companion object {
        private const val TAG = "MainActivity"
    }
}