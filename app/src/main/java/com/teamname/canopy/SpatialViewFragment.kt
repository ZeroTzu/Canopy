package com.teamname.canopy

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.EntityManager
import com.google.ar.sceneform.rendering.ViewRenderable
import com.teamname.canopy.databinding.FragmentSpatialViewBinding
import com.teamname.canopy.utils.SceneAnnotation
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.ViewNode
import io.github.sceneview.node.ViewNode2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "canopyName"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SpatialViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SpatialViewFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var canopyName: String? = null
    private var param2: String? = null
    private lateinit var sceneView: SceneView
    private lateinit var binding: FragmentSpatialViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            canopyName = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentSpatialViewBinding.inflate(inflater,container,false)
        binding.fragmentSpatialViewCanopyNameTextView.text=canopyName
        // Inflate the layout for this fragment
        sceneView=binding.fragmentSpatialViewSceneView
        viewLifecycleOwner.lifecycleScope.launch {
            sceneView.cameraNode.apply {
                position = Position(z = 20.0f)
            }
            var modelFile = "environments/avocado1.glb"
            modelFile = "environments/proid_model_3.glb"
            val modelInstance = sceneView.modelLoader.createModelInstance(Uri.parse(modelFile).toString())
            val modelNode = ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 2.0f,
            )
            modelNode.scale = Scale(0.05f)
            sceneView.addChildNode(modelNode)

            val center = Position(0.0f, 0.0f, 0.0f)
            val radius = 16.0f
            var angle = 0.0
            val cameraNode = sceneView.cameraNode

            var annotations = listOf(
                SceneAnnotation(Position(5.0f, 0.0f, 10.0f), "Sensor 1"),
                SceneAnnotation(Position(-5.0f, 0.0f, 10.0f), "Sensor 2"),
                SceneAnnotation(Position(0.0f, 5.0f, 15.0f), "Sensor 3")
            )


        }
        binding.fragmentSpatialViewCloseButton.setOnClickListener {
            val fragmentTransaction = parentFragmentManager.beginTransaction().replace(R.id.nav_host_fragment,Home())
            fragmentTransaction.commit()
        }
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SpatialViewFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(canopyName: String, param2: String) =
            SpatialViewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, canopyName)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}