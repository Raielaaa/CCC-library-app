package com.example.ccc_library_app.ui.dashboard.home

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import com.example.ccc_library_app.R
import com.example.ccc_library_app.databinding.FragmentHomeBinding
import com.example.ccc_library_app.ui.account.util.Resources
import com.example.ccc_library_app.ui.dashboard.home.popular.PopularAdapter
import com.example.ccc_library_app.ui.dashboard.home.popular.PopularModel
import kotlinx.coroutines.*
import java.util.Timer
import java.util.TimerTask
import kotlin.coroutines.CoroutineContext

class HomeFragment : Fragment(), CoroutineScope {
    //  Views
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: PopularAdapter

    //  ViewModel
    private lateinit var homeFragmentViewModel: HomeFragmentViewModel

    //  Slideshow
    private val imageSlideshow = ImageSlideshow()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    //  General
    private val TAG: String = "MyTag"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeFragmentViewModel = ViewModelProvider(this)[HomeFragmentViewModel::class.java]

        initRecyclerView()
        initClickableViews()
        imageSlideshow.startSlideshow(binding.ivSlideshow)
        initBottomNavigationBar()

        return binding.root
    }

    private fun initBottomNavigationBar() {
        binding.apply {
            homeFragmentViewModel.navigateToBookList(this@HomeFragment, ivBookList)
            homeFragmentViewModel.navigateToBookmark(this@HomeFragment, ivBookmark)
            homeFragmentViewModel.navigateToSettings(this@HomeFragment, ivSettings)
        }
    }

    private fun initClickableViews() {
        binding.apply {
            cvCaptureQR.setOnClickListener {
                Resources.displayCustomDialog(
                    requireActivity(),
                    this@HomeFragment,
                    R.layout.custom_dialog_loading
                )
                homeFragmentViewModel.captureQR(requireActivity())
            }
            ivTakeQr.setOnClickListener {
                Resources.displayCustomDialog(
                    requireActivity(),
                    this@HomeFragment,
                    R.layout.custom_dialog_loading
                )
                homeFragmentViewModel.captureQR(requireActivity())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext[Job]?.cancel()
        imageSlideshow.stopSlideshow()
    }

    private fun initRecyclerView() {
        adapter = PopularAdapter()

        binding.apply {
            adapter.setList(
                listOf(
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_1"), "Data Science"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_2"), "Max. Impact"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_3"), "Techno-crimes"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_4"), "Data Science 2"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_5"), "Data Science 3"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_6"), "Data Science 4"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_7"), "Data Science 5"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_8"), "Data Science 6"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_9"), "Data Science 7"),
                    PopularModel(Uri.parse("android.resource://com.example.ccc_library_app/drawable/book_10"), "Data Science 8"),
                )
            )

            rvPopular.adapter = adapter
        }
    }
}

class ImageSlideshow {
    private val images = intArrayOf(R.drawable.slideshow_pic1, R.drawable.slideshow_pic2, R.drawable.slideshow_pic3, R.drawable.slideshow_pic4)
    private var currentImageIndex = 0
    private var timer: Timer? = null
    private lateinit var imageView: ImageView

    fun startSlideshow(imageView: ImageView) {
        this.imageView = imageView
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                changeImageWithAnimation(imageView)
            }
        }, 0, 5000) // Change image every 5 seconds
    }

    fun stopSlideshow() {
        timer?.cancel()
        timer?.purge()
    }

    private fun changeImageWithAnimation(imageView: ImageView) {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(fadeIn)

        imageView.startAnimation(animationSet)

        currentImageIndex = (currentImageIndex + 1) % images.size
        // Preload the next image in the background
        val nextImageIndex = (currentImageIndex + 1) % images.size
        val nextImage = images[nextImageIndex]
        imageView.post {
            imageView.setImageResource(nextImage)
        }
    }
}