package com.bonushub.crdb.view.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bonushub.crdb.IDialog
import com.bonushub.crdb.R
import com.bonushub.crdb.model.local.TerminalCommunicationTable
import com.bonushub.crdb.utils.Result
import com.bonushub.crdb.viewmodel.MainViewModel
import com.mindorks.example.coroutines.utils.Status


import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_info.*

@AndroidEntryPoint
class MainInfoFragment : Fragment() {
    private var iDialog: IDialog? = null
    private val mainViewModel : MainViewModel by viewModels()
    private var mainInfoView : View? = null
    private lateinit var progressDialog: Dialog
    lateinit var progressTitleMsg: TextView
    lateinit var progressPercent: ProgressBar
    lateinit var progressPercentTv: TextView
    lateinit var horizontalPLL: LinearLayout
    lateinit var verticalProgressBar: ProgressBar
    var mContainerId:Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is IDialog) iDialog = context
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainInfoView = inflater.inflate(R.layout.fragment_info, container, false)
        mContainerId = container?.id?:-1
        return  mainInfoView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel.insertInfo1("41501375")

        button_save.setOnClickListener {

          //  val student = getEnteredStudentDetails()
          //  mainViewModel.insertInfo(student)
            mainViewModel.insertInfo1("41501375")

        }
        button_cancel.setOnClickListener {
            activity?.let{

                activity?.supportFragmentManager?.popBackStack()
            }
        }
       // observeViewModel()

        observeMainViewModel()
    }



    fun getEnteredStudentDetails() : TerminalCommunicationTable {
        var age : String? = null
        if(!ed_pcno.text.toString().isNullOrEmpty() && ed_pcno.text.toString().isDigitsOnly()) {
            age =  (ed_pcno.text.toString())
        }

       return TerminalCommunicationTable("1","1", "1","1")
    }


    fun observeViewModel(){
        mainViewModel.fetchError().observe(viewLifecycleOwner,
            Observer<String> { t -> Toast.makeText(activity,t, Toast.LENGTH_LONG).show() })

        mainViewModel.fetchInsertedId().observe(viewLifecycleOwner,
            Observer<Long> { t ->
                if(t != -1L){



                    Toast.makeText(activity,"Inserted Successfully in DB $t", Toast.LENGTH_LONG).show()
                    activity?.let{

                        activity?.supportFragmentManager?.popBackStack()
                    }

                }else{
                    Toast.makeText(activity,"Insert Failed",Toast.LENGTH_LONG).show()

                }

            })
    }

    fun observeMainViewModel(){
        mainViewModel.mutableLiveData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {

                    Toast.makeText(activity,"Success called $", Toast.LENGTH_LONG).show()
                }

                Status.ERROR -> {
                    Toast.makeText(activity,"Error called  ${result.message}", Toast.LENGTH_LONG).show()
                }

                Status.LOADING -> {
                    Toast.makeText(activity,"Loading called $", Toast.LENGTH_LONG).show()
                }
            }
            mainViewModel.isLoading().observe(viewLifecycleOwner, Observer {
                if(it) {
                    Toast.makeText(activity, "loding", Toast.LENGTH_SHORT).show()
                }else
                    Toast.makeText(activity, "done", Toast.LENGTH_SHORT).show()
            })
        })
    }

}