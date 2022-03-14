package com.bonushub.crdb.india.view.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentBankFunctionsInitPaymentAppBinding
import com.bonushub.crdb.india.di.DBModule
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.adapter.BankFunctionsInitPaymentAppAdapter
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.crdb.india.viewmodel.BankFunctionsViewModel
import com.bonushub.crdb.india.viewmodel.InitViewModel
import com.bonushub.pax.utils.KeyExchanger
import com.google.gson.Gson
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class BankFunctionsInitPaymentAppFragment : Fragment() {

    private var iDialog: IDialog? = null
    private val initViewModel : InitViewModel by viewModels()

    var binding: FragmentBankFunctionsInitPaymentAppBinding? = null
    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBankFunctionsInitPaymentAppBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.subHeaderView?.subHeaderText?.text = getString(R.string.init_payment_app_header)

        iDialog = (activity as NavigationActivity)

        lifecycleScope.launch {

            bankFunctionsViewModel.getAllTidsWithStatus()?.observe(viewLifecycleOwner,{
                    //tpt ->

                setupRecyclerview(it)

                //logger("tpt","data"+tpt.LinkTidType.toString())
                // link tid type use for find type of child tid , tid type use for find base tid
                /*var tidType = tpt.tidType
                var linkTidType = tpt.LinkTidType
                var tids = tpt.terminalId*/

                // temp
                /*var tidType = listOf<String>("0","1","0","0","0")
                var linkTidType = listOf<String>("0","1","2","3","9")
                var tids = listOf<String>("30160043","30160033","30160044","30160045","30160048")*/

                /*
                if tidtype value is  1 then it is base Tid
                else - child tids
                and
                LinkTidType :
                for Amex             - 0
                DC type              - 1
                offus Tid            - 2
                3 months onus        - 3
                6 months onus        - 6
                9 months onus        - 9
                12 months onus       - 12
                and so on*/



            })


        }



        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            parentFragmentManager.popBackStackImmediate()
        }

        binding?.textViewInitAllTids?.setOnClickListener {

            iDialog?.showProgress(getString(R.string.please_wait_host))

            runBlocking {
                val tids = checkBaseTid(DBModule.appDatabase?.appDao)

                if(!tids.get(0).isEmpty()!!) {

                    logger("get tid", "by table")
                    // get tid from table and init

                    initViewModel.insertInfo1(tids[0] ?:"")
                    observeMainViewModel()
                }else{
// get tid by user
                    logger("get tid","by user")
                    (activity as NavigationActivity).transactFragment(InitFragment())
                }
            }
        }
    }

    private fun setupRecyclerview(tidsWithStatusList:ArrayList<TidsListModel>){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = BankFunctionsInitPaymentAppAdapter(tidsWithStatusList)
            }

        }
    }

    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {

                    var isStaticQrAvailable=false

                    lifecycleScope.launch(Dispatchers.IO) {

                        KeyExchanger.getDigiPosStatus(
                            EnumDigiPosProcess.InitializeDigiPOS.code,
                            EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false
                        ) { isSuccess, responseMsg, responsef57, fullResponse ->
                            try {
                                if (isSuccess) {
                                    //1^Success^Success^S101^Active^Active^Active^Active^0^1
                                    val responsF57List = responsef57.split("^")
                                    Log.e("F56->>", responsef57)
                                    //  if (responsF57List[4] == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                    val tpt1 = Field48ResponseTimestamp.getTptData()
                                    tpt1?.digiPosResponseType = responsF57List[0].toString()
                                    tpt1?.digiPosStatus = responsF57List[1].toString()
                                    tpt1?.digiPosStatusMessage =
                                        responsF57List[2].toString()
                                    tpt1?.digiPosStatusCode = responsF57List[3].toString()
                                    tpt1?.digiPosTerminalStatus = responsF57List[4].toString()
                                    tpt1?.digiPosBQRStatus = responsF57List[5].toString()
                                    tpt1?.digiPosUPIStatus = responsF57List[6].toString()
                                    tpt1?.digiPosSMSpayStatus = responsF57List[7].toString()
                                    tpt1?.digiPosStaticQrDownloadRequired =
                                        responsF57List[8].toString()
                                    tpt1?.digiPosCardCallBackRequired =
                                        responsF57List[9].toString()

                                    if ((tpt1?.digiPosTerminalStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) && (tpt1?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                || tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                || tpt1.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode)
                                    ) {
                                        tpt1.isDigiposActive = "1"
                                    } else {
                                        tpt1?.isDigiposActive = "0"
                                    }

                                    if (tpt1 != null) {
                                        Field48ResponseTimestamp.performOperation(tpt1) {
                                            logger(
                                                LOG_TAG.DIGIPOS.tag,
                                                "Terminal parameter Table updated successfully $tpt1 "
                                            )
                                            //val ttp = TerminalParameterTable.selectFromSchemeTable()
                                            val ttp = Field48ResponseTimestamp.getTptData()
                                            val tptObj = Gson().toJson(ttp)
                                            logger(
                                                LOG_TAG.DIGIPOS.tag,
                                                "After success      $tptObj "
                                            )
                                        }
                                        if (tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                            var imgbm: Bitmap? = null
                                            runBlocking(Dispatchers.IO) {
                                                val tpt = Field48ResponseTimestamp.getTptData()
                                                imgbm =
                                                    loadStaticQrFromInternalStorage() // it return null when file not exist
                                                if (imgbm == null || tpt?.digiPosStaticQrDownloadRequired == "1") {
                                                    isStaticQrAvailable = true
                                                }
                                            }

                                        }

                                    }

                                    //  }

                                    /* else {
                                            logger("DIGI_POS", "DIGI_POS_UNAVAILABLE")
                                        }*/
                                } else {
                                    //VFService.showToast(responseMsg)
                                }

                            } catch (ex: java.lang.Exception) {
                                ex.printStackTrace()
                                logger(
                                    LOG_TAG.DIGIPOS.tag,
                                    "Somethig wrong... in response data field 57"
                                )
                            }
                        }

                        if (isStaticQrAvailable) {
                            // getting static qr from server if required
                            //withContext(Dispatchers.IO){
                            getStaticQrFromServerAndSaveToFile(requireActivity()) {
                                // FAIL AND SUCCESS HANDELED IN FUNCTION getStaticQrFromServerAndSaveToFile itself
                            }
                            //}

                        }

                        withContext(Dispatchers.Main)
                        {
                            iDialog?.hideProgress()
                            (activity as NavigationActivity).transactFragment(DashboardFragment())
                        }
                    }

                }
                Status.ERROR -> {
                    iDialog?.hideProgress()
                    ToastUtils.showToast(activity,"Error called  ${result.error}")
                }
                Status.LOADING -> {
                    iDialog?.showProgress("Sending/Receiving From Host")

                }
            }

        })


    }

}

data class TidsListModel(var tids:String, var des:String,var status:String,var linkTidType:String)