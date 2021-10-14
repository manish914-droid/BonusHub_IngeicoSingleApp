package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonushub.crdb.R

import com.bonushub.crdb.model.TerminalCommunicationTable
import com.bonushub.crdb.utils.replaceFragment
import com.bonushub.crdb.view.adapter.MainAdapter
import com.bonushub.crdb.viewmodel.MainViewModel

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_list.*

@AndroidEntryPoint
class MainInfoListFragment : Fragment(), LifecycleOwner {

    private var mainInfoListView : View? = null
    var mContainerId:Int = -1
    private var mainAdapter : MainAdapter? = null
    private val mainViewModel : MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainInfoListView = inflater.inflate(R.layout.fragment_list, container, false)
        mContainerId = container?.id?:-1
        return  mainInfoListView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.lifecycle.addObserver(mainViewModel)
        add_student_floating_btn.setOnClickListener {
            launchAddFragment()
        }
        initAdapter()
    }

    override fun onResume() {
        super.onResume()
        fetchDataFromViewModel()
    }
    fun launchAddFragment(){
        activity?.replaceFragment(MainInfoFragment(), mContainerId)
    }

    private fun initAdapter(){
        mainAdapter = MainAdapter(arrayListOf())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = mainAdapter

        }

    }

    private fun fetchDataFromViewModel(){
        // viewModel.fetchRoomData()
        mainViewModel.userFinalList.observe(this,
            Observer<MutableList<TerminalCommunicationTable>> {
                    t -> println("Received UserInfo List $t")
                mainAdapter?.refreshAdapter(t)
            }
        )
    }
}