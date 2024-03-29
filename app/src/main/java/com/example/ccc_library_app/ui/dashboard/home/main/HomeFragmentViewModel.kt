package com.example.ccc_library_app.ui.dashboard.home.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.ccc_library_app.R
import com.example.ccc_library_app.ui.dashboard.home.featured.CompleteFeaturedBookModel
import com.example.ccc_library_app.ui.dashboard.home.inventory.InventoryItemsDataModel
import com.example.ccc_library_app.ui.dashboard.home.inventory.InventorySeeAllBottomSheetFragment
import com.example.ccc_library_app.ui.dashboard.home.popular.FirebaseDataModel
import com.example.ccc_library_app.ui.dashboard.home.popular.PopularAdapter
import com.example.ccc_library_app.ui.dashboard.home.popular.PopularModel
import com.example.ccc_library_app.ui.dashboard.list.BookListItemModel
import com.example.ccc_library_app.ui.dashboard.util.CompleteBookInfoModel
import com.example.ccc_library_app.ui.dashboard.util.DataCache
import com.example.ccc_library_app.ui.dashboard.util.Resources
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    @Named("FirebaseFireStore.Instance")
    val firebaseFireStore: FirebaseFirestore,
    @Named("FirebaseStorage.Instance")
    val firebaseStorage: StorageReference,
    @Named("FirebaseStorage.Reference")
    val storage: FirebaseStorage
) : ViewModel() {
    private var TAG: String = "MyTag"

    private lateinit var bookListPopularPermanent: ArrayList<BookListItemModel>
    private lateinit var bookListPopularTemp: ArrayList<FirebaseDataModel>
    private var bookListPopularFinal: ArrayList<PopularModel> = ArrayList()
    private lateinit var completeBookInfoModel: ArrayList<CompleteBookInfoModel>

    fun captureQR(activity: Activity) {
        com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
        //  Prompting the user for camera permission. Camera permission is used so that we can use
        //  the camera of the phone of the user
        cameraPermissions(activity)
    }

    private fun cameraPermissions(activity: Activity) {
        val cameraPermission = android.Manifest.permission.CAMERA
        val permissionGranted = PackageManager.PERMISSION_GRANTED

        if (ContextCompat.checkSelfPermission(activity, cameraPermission) != permissionGranted) {
            // Request camera permission if it's not granted.
            ActivityCompat.requestPermissions(activity, arrayOf(cameraPermission), 111)
        } else {
            // Camera permission is already granted, proceed to capturing the image.
            takeImage(activity)
        }
    }

    private fun takeImage(activity: Activity) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            activity.startActivityForResult(intent, 1)
        } catch (e: Exception) {
            Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "takeImage: ${e.message}")
        }
    }

    fun navigateToBookList(hostFragment: Fragment, ivBookList: ImageView) {
        Resources.navigate(hostFragment, ivBookList, R.id.action_homeFragment_to_bookListFragment)
    }

    fun navigateToBookmark(hostFragment: Fragment, ivBookmark: ImageView) {
        Resources.navigate(hostFragment, ivBookmark, R.id.action_homeFragment_to_bookmarkFragment)
    }

    fun navigateToSettings(hostFragment: Fragment, ivSettings: ImageView) {
        Resources.navigate(hostFragment, ivSettings, R.id.action_homeFragment_to_settingsFragment)
    }

    fun initPopularRecyclerView(
        recyclerView: RecyclerView,
        activity: Activity,
        hostFragment: Fragment
    ) {
        try {
            com.example.ccc_library_app.ui.account.util.Resources.displayCustomDialog(
                activity,
                R.layout.custom_dialog_loading
            )

            bookListPopularTemp = ArrayList()

            firebaseFireStore.collection("ccc-library-app-book-info")
                .get()
                .addOnSuccessListener { bookInfoListFromDB ->
                    if (bookInfoListFromDB.isEmpty) {
                        Toast.makeText(
                            activity,
                            "Popular books unavailable",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val genreHolder: ArrayList<String> = ArrayList()

                        for (bookInfo in bookInfoListFromDB) {
                            bookListPopularTemp.add(
                                FirebaseDataModel(
                                    bookInfo.get("modelBookTitle").toString(),
                                    bookInfo.get("modelBookImage").toString(),
                                )
                            )
                            genreHolder.add(bookInfo.get("modelBookGenre").toString())
                        }

                        val adapter = PopularAdapter(
                            hostFragment.requireContext(),
                            storage,
                            bookListPopularTemp
                        ) {
                            clickedFunction(it, hostFragment)
                        }
                        adapter.setList(bookListPopularTemp)

                        recyclerView.adapter = adapter

                        com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
                    }
                }
        } catch (exception: Exception) {
            Toast.makeText(
                activity,
                "An error occurred: ${exception.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "initPopularRecyclerView: ", exception)
        }
    }


//    //  Displaying items in popular recycler view
//    private fun displayInfoToRecyclerView(recyclerView: RecyclerView, activity: Activity, bookListPopularFinal: ArrayList<PopularModel>, hostFragment: Fragment) {
//        val adapter = PopularAdapter { clickedItemInfo ->
//            clickedFunction(clickedItemInfo, hostFragment)
//        }
//        recyclerView.adapter = adapter
//        adapter.setList(bookListPopularFinal)
//
//        com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
//    }

    private fun clickedFunction(clickedItemInfo: FirebaseDataModel, hostFragment: Fragment) {
        hostFragment.findNavController().navigate(R.id.action_homeFragment_to_clickedBookFragment, bundleOf("bookTitleKey" to clickedItemInfo.modelBookTitle))
    }

    private fun bitmapToUri(activity: Activity, bitmap: Bitmap, bookNumber: String): Uri? {
        // Get the directory for storing the image
        val imagesDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val formattedFileName = bookNumber.split("/")
        val imageFile = File(imagesDir, "${formattedFileName[1]}.png")

        try {
            // Create a FileOutputStream to write the bitmap to the file
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()

            // Create a Uri from the file
            return Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun showToastMessage(activity: Activity, message: String) {
        Toast.makeText(
            activity,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun initFeaturedBook(
        ivFeaturedImage: ImageView,
        tvFeaturedTitle: TextView,
        tvFeaturedDescription: TextView,
        context: Context
    ) {
        firebaseFireStore.collection("ccc-library-app-book-data")
            .orderBy("modelBookCount", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshotRoot ->
                for (item in querySnapshotRoot.documents) {
                    val bookBookCodeForImage = item.get("modelBookCode").toString()
                    val bookTitle = item.get("modelBookName").toString()

                    firebaseFireStore.collection("ccc-library-app-book-info")
                        .document(bookBookCodeForImage)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val bookDescription = querySnapshot.get("modelBookDescription").toString()

                            tvFeaturedTitle.text = bookTitle
                            tvFeaturedDescription.text = bookDescription

                            val gsReference = storage.getReferenceFromUrl("gs://ccc-library-system.appspot.com/book_images/$bookBookCodeForImage.jpg")
                            Glide.with(context)
                                .load(gsReference)
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image)
                                .into(ivFeaturedImage)
                        }.addOnFailureListener { exception ->
                            Log.e("MyTag", "initFeaturedBook: $exception")
                            Toast.makeText(
                                context,
                                "An error occurred: ${exception.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }.addOnFailureListener { exception ->
                Log.e("MyTag", "initFeaturedBook: $exception")
                Toast.makeText(
                    context,
                    "An error occurred: ${exception.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getBookCompleteInfo(
        document: QueryDocumentSnapshot?,
        activity: Activity,
        callback: (CompleteFeaturedBookModel?) -> Unit
    ) {
        var completeFeaturedBookModelTemp: CompleteFeaturedBookModel?

        firebaseFireStore.collection("ccc-library-app-book-info")
            .document(document!!.get("modelBookCode").toString())
            .get()
            .addOnSuccessListener { documentSnapshot ->
                var imageBitmap: Bitmap? = null
//                getImage(documentSnapshot.data!!["modelBookImage"].toString(), activity) { bitmapImage ->
//                    if (bitmapImage == null) {
//                        showToastMessage(activity, "An error occurred displaying the books")
//                    } else {
//                        imageBitmap = bitmapImage
//                    }
//
//                    completeFeaturedBookModelTemp = CompleteFeaturedBookModel(
//                        bitmapToUri(activity, imageBitmap!!, "book_images/${documentSnapshot.data!!["modelBookCode"]}"
//                        )!!,
//                        documentSnapshot.data!!["modelBookTitle"].toString(),
//                        documentSnapshot.data!!["modelBookDescription"].toString(),
//                        document.get("modelBookCount").toString()
//                    )
//
//                    // Pass the result to the callback
//                    callback(completeFeaturedBookModelTemp)
//                }
            }
    }

    fun initFeaturedClickedFunction(
        hostFragment: Fragment,
        bookTitle: String
    ) {
        hostFragment.findNavController().navigate(R.id.action_homeFragment_to_clickedBookFragment, bundleOf("bookTitleKey" to bookTitle))
    }

    fun displayTally(
        tvInventoryCurrent: TextView,
        tvInventoryBorrowed: TextView,
        context: Context
    ) {
        completeBookInfoModel = ArrayList()

        firebaseFireStore.collection("ccc-library-app-book-info")
            .get()
            .addOnCompleteListener { querySnapshot ->
                for (items in querySnapshot.result) {
                    val modelBookAuthor = items.get("modelBookAuthor").toString()
                    val modelBookCode = items.get("modelBookCode").toString()
                    val modelBookDescription = items.get("modelBookDescription").toString()
                    val modelBookGenre = items.get("modelBookGenre").toString()
                    val modelBookImage = items.get("modelBookImage").toString()
                    val modelBookPublicationDate = items.get("modelBookPublicationDate").toString()
                    val modelBookPublisher = items.get("modelBookPublisher").toString()
                    val modelBookTitle = items.get("modelBookTitle").toString()
                    val modelBookStatus = items.get("modelStatus").toString()

                    completeBookInfoModel.add(
                        CompleteBookInfoModel(
                            modelBookAuthor,
                            modelBookCode,
                            modelBookDescription,
                            modelBookGenre,
                            modelBookImage,
                            modelBookPublicationDate,
                            modelBookPublisher,
                            modelBookTitle,
                            modelBookStatus
                        )
                    )

                    var inventoryCurrent = 0
                    var inventoryBorrowed = 0

                    for (data in completeBookInfoModel) {
                        if (data.modelBookStatus == "Available") inventoryCurrent++
                        else inventoryBorrowed++
                    }

                    //  Setting values to the Inventory and Borrowed CardView Display
                    tvInventoryCurrent.text = inventoryCurrent.toString()
                    tvInventoryBorrowed.text = inventoryBorrowed.toString()

                    //  Storing retrieved data to DataCache
                    DataCache.booksFullInfo.add(
                        CompleteBookInfoModel(
                            modelBookAuthor,
                            modelBookCode,
                            modelBookDescription,
                            modelBookGenre,
                            modelBookImage,
                            modelBookPublicationDate,
                            modelBookPublisher,
                            modelBookTitle,
                            modelBookStatus
                        )
                    )
                }
            }.addOnFailureListener { exception ->
                Log.e("MyTag", "displayTally: ${exception.localizedMessage}")
                Toast.makeText(
                    context,
                    "An error occurred: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun initSeeAllBottomDialog(
        tvInventoryCurrent: TextView,
        ivInventoryCurrent: ImageView,
        tvInventoryBorrowed: TextView,
        ivInventoryBorrow: ImageView,
        hostFragment: Fragment,
        activity: Activity
    ) {
        tvInventoryBorrowed.setOnClickListener {
            bottomSheetSeeAllBorrow(hostFragment, activity)
        }
        ivInventoryBorrow.setOnClickListener {
            bottomSheetSeeAllBorrow(hostFragment, activity)
        }


        tvInventoryCurrent.setOnClickListener {
            bottomSheetSeeAllCurrent(hostFragment, activity)
        }
        ivInventoryCurrent.setOnClickListener {
            bottomSheetSeeAllCurrent(hostFragment, activity)
        }
    }

    private fun bottomSheetSeeAllCurrent(hostFragment: Fragment, activity: Activity) {
        com.example.ccc_library_app.ui.account.util.Resources.displayCustomDialog(
            activity,
            R.layout.custom_dialog_loading
        )

        try {
            val listOfBookInfo = ArrayList<CompleteBookInfoModel>()

            for (items in removeDuplicates(DataCache.booksFullInfo)) {
                if (items.modelBookStatus == "Available") {
                    listOfBookInfo.add(items)
                }
            }

            if (listOfBookInfo.isNotEmpty()) {
                InventorySeeAllBottomSheetFragment(listOfBookInfo, "Borrowed Books")
                    .show(hostFragment.parentFragmentManager, "SeeAll_BottomSheetFragment")
            } else {
                Toast.makeText(
                    hostFragment.requireContext(),
                    "Nothing to show",
                    Toast.LENGTH_SHORT
                ).show()
            }

            com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
        } catch (err: Exception) {
            showToastMessage(activity, "An error occurred: ${err.localizedMessage}")
            Log.e(TAG, "bottomSheetSeeAllCurrent: ${err.message}", )
            com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
        }
    }

    private fun removeDuplicates(listItem: ArrayList<CompleteBookInfoModel>): ArrayList<CompleteBookInfoModel> {
        return ArrayList(listItem.distinctBy { it.modelBookCode })
    }

    private fun bottomSheetSeeAllBorrow(hostFragment: Fragment, activity: Activity) {
        com.example.ccc_library_app.ui.account.util.Resources.displayCustomDialog(
            activity,
            R.layout.custom_dialog_loading
        )

        try {
            val listOfBookInfo = ArrayList<CompleteBookInfoModel>()

            for (items in removeDuplicates(DataCache.booksFullInfo)) {
                if (items.modelBookStatus != "Available") {
                    listOfBookInfo.add(items)
                }
            }

            if (listOfBookInfo.isNotEmpty()) {
                InventorySeeAllBottomSheetFragment(listOfBookInfo, "Borrowed Books")
                    .show(hostFragment.parentFragmentManager, "SeeAll_BottomSheetFragment")
            } else {
                Toast.makeText(
                    hostFragment.requireContext(),
                    "Nothing to show",
                    Toast.LENGTH_SHORT
                ).show()
            }

            com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
        } catch (err: Exception) {
            showToastMessage(activity, "An error occurred: ${err.localizedMessage}")
            Log.e(TAG, "bottomSheetSeeAllBorrow: ${err.message}", )
            com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
        }
    }

    fun refreshPersistentData(
        hostFragment: Fragment,
        swipeRefreshLayout: SwipeRefreshLayout
    ) {
        completeBookInfoModel = ArrayList()
        //  Clear the current data inside the DataCache
        DataCache.booksFullInfo.clear()

        //  Loading dialog
        com.example.ccc_library_app.ui.account.util.Resources.displayCustomDialog(
            hostFragment.requireActivity(),
            R.layout.custom_dialog_loading
        )

        firebaseFireStore.collection("ccc-library-app-book-info")
            .get()
            .addOnCompleteListener { querySnapshot ->
                for (items in querySnapshot.result) {
                    val modelBookAuthor = items.get("modelBookAuthor").toString()
                    val modelBookCode = items.get("modelBookCode").toString()
                    val modelBookDescription = items.get("modelBookDescription").toString()
                    val modelBookGenre = items.get("modelBookGenre").toString()
                    val modelBookImage = items.get("modelBookImage").toString()
                    val modelBookPublicationDate = items.get("modelBookPublicationDate").toString()
                    val modelBookPublisher = items.get("modelBookPublisher").toString()
                    val modelBookTitle = items.get("modelBookTitle").toString()
                    val modelBookStatus = items.get("modelStatus").toString()

                    completeBookInfoModel.add(
                        CompleteBookInfoModel(
                            modelBookAuthor,
                            modelBookCode,
                            modelBookDescription,
                            modelBookGenre,
                            modelBookImage,
                            modelBookPublicationDate,
                            modelBookPublisher,
                            modelBookTitle,
                            modelBookStatus
                        )
                    )

                    //  Storing retrieved data to DataCache
                    DataCache.booksFullInfo.add(
                        CompleteBookInfoModel(
                            modelBookAuthor,
                            modelBookCode,
                            modelBookDescription,
                            modelBookGenre,
                            modelBookImage,
                            modelBookPublicationDate,
                            modelBookPublisher,
                            modelBookTitle,
                            modelBookStatus
                        )
                    )
                }

                com.example.ccc_library_app.ui.account.util.Resources.dismissDialog()
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(
                    hostFragment.requireContext(),
                    "All done! Your data is now up to date.",
                    Toast.LENGTH_SHORT
                ).show()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    hostFragment.findNavController().navigate(R.id.homeFragment)
                }
            }.addOnFailureListener { exception ->
                Log.e("MyTag", "displayTally: ${exception.localizedMessage}")
                Toast.makeText(
                    hostFragment.requireContext(),
                    "An error occurred: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}