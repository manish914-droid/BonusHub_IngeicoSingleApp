package com.bonushub.crdb.india.view.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentReportsBinding
import com.bonushub.crdb.india.utils.SettingsItem
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.SettingsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(), ISettingsFragmentItemClick {


    var binding: FragmentReportsBinding? = null
    private val settingsItemList: MutableList<SettingsItem> by lazy { mutableListOf<SettingsItem>() }
    private lateinit var settingsAdapter:SettingsAdapter
    private var iSettingsFragmentItemClick: ISettingsFragmentItemClick? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_settings, container, false)
        binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as NavigationActivity).manageTopToolBar(false)

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.settings)
        binding?.subHeaderView?.headerImage?.setImageResource(R.drawable.ic_setting)

        iSettingsFragmentItemClick = this

        settingsItemList.clear()
        settingsItemList.addAll(SettingsItem.values())
        setupRecyclerview()

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            //parentFragmentManager.popBackStackImmediate()
            try {
                (activity as NavigationActivity).decideDashBoardOnBackPress()
            }catch (ex:Exception){
                ex.printStackTrace()

            }
        }

    }

    private fun setupRecyclerview() {
        settingsAdapter = SettingsAdapter(settingsItemList, iSettingsFragmentItemClick)
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = settingsAdapter
            }

        }
    }

    override fun SettingsOptionItemClick(settingsItem: SettingsItem, itemPosition: Int) {

        when(settingsItem){

            SettingsItem.WIFI_SETTINGS ->{
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setupRecyclerview()
    }
}

interface ISettingsFragmentItemClick {

    fun SettingsOptionItemClick(settingsItem: SettingsItem, itemPosition:Int)
}