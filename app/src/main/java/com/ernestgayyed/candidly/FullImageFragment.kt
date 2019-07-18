package com.ernestgayyed.candidly


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class FullImageFragment : Fragment() {

    lateinit var imageFullScreen: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_full_image, container, false)

        imageFullScreen = view.findViewById(R.id.img_full_screen)

        return view
    }

    override fun onResume() {
        super.onResume()

        val bundle = arguments
        val imagePath = bundle?.get("image_path")

        context?.let {
            Glide.with(it).load(imagePath)
                .placeholder(R.mipmap.ic_launcher).fitCenter()
                .into(imageFullScreen)
        }
    }
}
