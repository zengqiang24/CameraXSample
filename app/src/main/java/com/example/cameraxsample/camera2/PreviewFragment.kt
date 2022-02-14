package com.example.cameraxsample.camera2

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.MainApplication
import com.example.cameraxsample.common.AVMPreviewConfig
import com.example.cameraxsample.databinding.CameraLayoutFragmentBinding

class PreviewFragment(private val cameraId: String) : Fragment() {
    private lateinit var binding: CameraLayoutFragmentBinding
    private var viewModel: VCameraViewModel? = null
    private lateinit var _context: Context
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this._context = context
    }

    companion object {
        fun newInstance(cameraId: String): Fragment {
            return PreviewFragment(cameraId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mainApplication = _context.applicationContext as MainApplication
        viewModel = VCameraViewModel(
            CameraFactoryImpl().createCamera(
                _context,
                cameraId,
                mainApplication.getRespository()
            )
        )
        binding = CameraLayoutFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel?.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel?.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cameraPreviewView = binding.viewFinder

        cameraPreviewView.holder.addCallback(object : SurfaceHolder.Callback {
            @RequiresApi(Build.VERSION_CODES.P)
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                val previewSurface = cameraPreviewView.holder.surface
                val targets = mutableListOf<Surface>(previewSurface)
                with(
                    AVMPreviewConfig(
                        surfaces = targets,
                    )
                ) {
                    viewModel?.preview(this)
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel?.releaseCameras()
    }
}