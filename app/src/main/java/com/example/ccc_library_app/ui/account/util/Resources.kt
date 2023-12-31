package com.example.ccc_library_app.ui.account.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ccc_library_app.R
import com.example.ccc_library_app.ui.dashboard.home.main.HomeFragment
import com.example.ccc_library_app.ui.dashboard.home.db.FirebaseDBManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

object Resources {
    private var dialog: Dialog? = null
    private var TAG: String = "MyTag"
    private lateinit var data: List<String>
    lateinit var navDrawer: NavigationView

    fun setGoogleSignInData(dataFromUser: List<String>) {
        data = dataFromUser
    }

    fun getGoogleSignInData(): List<String> = data

    @SuppressLint("ObsoleteSdkInt")
    fun displayCustomDialog(
        activity: Activity,
        hostFragment: Fragment,
        layoutDialog: Int
    ) {
        try {
            if (!activity.isFinishing) {
                dialog = Dialog(activity)

                dialog?.apply {
                    setContentView(layoutDialog)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        window!!.setBackgroundDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.custom_dialog_background, null))
                    window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setCancelable(false)
                    window!!.attributes.windowAnimations = R.style.animation
                    show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "displayCustomDialog: ${e.printStackTrace()}", )
            // Handle the exception, if necessary
        }

        try {
            dialog?.findViewById<Button>(R.id.btnOk)?.setOnClickListener {
                dialog?.dismiss()
                hostFragment.findNavController().navigate(R.id.homeAccountFragment)
            }
            dialog?.findViewById<Button>(R.id.btnCancel)?.setOnClickListener {
                dialog?.dismiss()
                hostFragment.findNavController().navigate(R.id.homeFragment)
            }
        } catch (ignored: Exception) { }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun displayCustomDialogForQr(
        parentFragment: HomeFragment,
        activity: Activity,
        layoutDialog: Int,
        imageBitmap: Bitmap,
        fireStore: FirebaseFirestore,
        auth: FirebaseAuth
    ) {
        try {
            if (!activity.isFinishing) {
                dialog = Dialog(activity)

                dialog?.apply {
                    setContentView(layoutDialog)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        window!!.setBackgroundDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.custom_dialog_background, null))
                    window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setCancelable(false)
                    window!!.attributes.windowAnimations = R.style.animation
                    show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "displayCustomDialog: ${e.printStackTrace()}", )
            // Handle the exception, if necessary
        }

        try {
            dialog?.apply {
                findViewById<Button>(R.id.btnProceed)?.setOnClickListener {
                    scanBitmapQR(imageBitmap, activity, fireStore, auth)
                }

                findViewById<Button>(R.id.btnCancel)?.setOnClickListener {
                    dismiss()
                }

                findViewById<ImageView>(R.id.ivQR)?.setImageBitmap(imageBitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "displayCustomDialog: ${e.message}")
        }
    }

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
        .enableAllPotentialBarcodes()
        .build()

    private fun scanBitmapQR(imageBitmap: Bitmap, activity: Activity, fireStore: FirebaseFirestore, auth: FirebaseAuth) {
        if (imageBitmap != null) {
            //  Create an InputImage object from the bitmap
            val image = InputImage.fromBitmap(imageBitmap, 0)
            //  Create a BarcodeScanner client with options
            val scanner = BarcodeScanning.getClient(options)

            //  Process the image for barcodes
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    //  If no barcode is found, show a toast message and exit function
                    if (barcodes.toString() == "[]") {
                        Toast.makeText(activity, "Nothing to scan. Please try again.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    //  Loop through each barcode found in the image
                    for (barcode in barcodes) {
                        //  If the barcode is of type text, extract the book name and author name
                        when (barcode.valueType) {
                            Barcode.TYPE_TEXT -> {
                                dismissDialog()

                                displayCustomDialog(
                                    activity,
                                    R.layout.custom_dialog_loading
                                )

                                FirebaseDBManager().insertDataToDB(
                                    barcode.rawValue,
                                    activity,
                                    fireStore,
                                    auth
                                )
                            }
                        }
                    }
                }
        }
        //  If image bitmap is null, show a toast message
        else {
            Toast.makeText(activity, "QR code not found", Toast.LENGTH_SHORT).show()
            dismissDialog()
        }
    }

    fun displayCustomDialog(
        activity: Activity,
        layoutDialog: Int
    ) {
        dialog = Dialog(activity)

        dialog?.apply {
            setContentView(layoutDialog)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                window!!.setBackgroundDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.custom_dialog_background, null))
            window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setCancelable(false)
            window!!.attributes.windowAnimations = R.style.animation
            show()
        }
        try {
            dialog?.findViewById<Button>(R.id.btnOk)?.setOnClickListener {
                dialog?.dismiss()
            }
        } catch (ignored: Exception) { }
    }

    fun dismissDialog() {
        dialog?.dismiss()
    }
}