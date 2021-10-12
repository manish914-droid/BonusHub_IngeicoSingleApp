package com.bonushub.crdb.view

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bonushub.crdb.R
import com.bonushub.crdb.model.TerminalCommunicationTable
import com.bonushub.crdb.viewmodel.MainViewModel
import com.bonushub.pax.utils.KeyExchanger
import com.bonushub.pax.utils.Utility

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainInfoFragment : Fragment() {

    private val mainViewModel : MainViewModel by viewModels()
    private var mainInfoView : View? = null
    var mContainerId:Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        mainViewModel.insertInfo1("41501379")

        button_save.setOnClickListener {

          //  val student = getEnteredStudentDetails()
          //  mainViewModel.insertInfo(student)

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

        mainViewModel.fetchInitResponse().observe(viewLifecycleOwner, Observer<Boolean> { it ->
            Toast.makeText(activity,"Init Sucessfully $it", Toast.LENGTH_LONG).show()
        })


    }

}