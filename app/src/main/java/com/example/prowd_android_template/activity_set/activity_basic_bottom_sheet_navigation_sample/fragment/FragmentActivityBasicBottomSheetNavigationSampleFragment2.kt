package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.R
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.ActivityBasicBottomSheetNavigationSample
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.ActivityBasicBottomSheetNavigationSampleViewModel

class FragmentActivityBasicBottomSheetNavigationSampleFragment2 : Fragment() {
    private lateinit var parentActivity: ActivityBasicBottomSheetNavigationSample
    private lateinit var parentViewModel: ActivityBasicBottomSheetNavigationSampleViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        parentActivity = requireActivity() as ActivityBasicBottomSheetNavigationSample
        parentViewModel = parentActivity.viewModelMbr

        return inflater.inflate(
            R.layout.fragment_activity_basic_bottom_sheet_navigation_sample_fragment2,
            container,
            false
        )
    }
}