package com.teamname.canopy

import android.animation.ValueAnimator
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import com.teamname.canopy.databinding.FragmentLoginLandingBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginLandingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginLandingFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentLoginLandingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding=FragmentLoginLandingBinding.inflate(layoutInflater)
        binding.fragmentLoginLandingLoginButton.setOnClickListener{
            Log.i("PolarisLoginActivity", "onCreateView: Login button clicked")
            requireActivity().supportFragmentManager.beginTransaction().replace(R.id.login_activity_fragmentContainerView,LoginFormFragment()).commit()
        }
        binding.fragmentLoginLandingRegisterButton.setOnClickListener{
            Log.i("PolarisLoginActivity", "onCreateView: Register button clicked")
            requireActivity().supportFragmentManager.beginTransaction().replace(R.id.login_activity_fragmentContainerView,LoginRegisterFormFragment()).commit()
        }
        var animationLottie = binding.fragmentLoginLandingAnimation
        animationLottie.animate()
        animationLottie.repeatCount=ValueAnimator.INFINITE





        val imageView = binding.fragmentLoginLandingAppIconImageView
        imageView.post {
            val matrix = Matrix()
            val drawable = imageView.drawable ?: return@post // Ensure drawable is not null
            val drawableWidth = drawable.intrinsicWidth.toFloat()
            val drawableHeight = drawable.intrinsicHeight.toFloat()

            val viewWidth = imageView.width.toFloat()
            val viewHeight = imageView.height.toFloat()

            // Desired scaling factor
            val scaleFactor = 3f

            // Calculate scaled dimensions of the drawable
            val scaledDrawableWidth = drawableWidth * scaleFactor
            val scaledDrawableHeight = drawableHeight * scaleFactor

            // Center the scaled drawable within the ImageView
            val dx = (viewWidth - scaledDrawableWidth) / 2
            val dy = (viewHeight - scaledDrawableHeight) / 2

            // Apply scaling and translation to the matrix
            matrix.setScale(scaleFactor, scaleFactor)
            matrix.postTranslate(dx, dy)

            // Set the matrix to the ImageView
            imageView.imageMatrix = matrix
            imageView.scaleType = ImageView.ScaleType.MATRIX
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
         * @return A new instance of fragment LoginLandingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginLandingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}