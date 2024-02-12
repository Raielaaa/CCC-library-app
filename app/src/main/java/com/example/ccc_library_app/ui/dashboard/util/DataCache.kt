package com.example.ccc_library_app.ui.dashboard.util

import android.graphics.Bitmap

object DataCache {
    var userImageProfile: Bitmap? = null
    val booksFullInfo = ArrayList<CompleteBookInfoModel>()

    var bookmarkPastDueCounter: Int = 0
    var bookmarkBorrowCount: Int = 0
}