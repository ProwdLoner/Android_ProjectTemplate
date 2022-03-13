package com.example.prowd_android_template.activity_set.activity_basic_tab_layout_sample.fragment1

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.prowd_android_template.R

class FragmentActivityBasicTabLayoutSampleFragment1 : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(
            R.layout.fragment_activity_basic_tab_layout_sample1,
            container,
            false
        )
    }
}