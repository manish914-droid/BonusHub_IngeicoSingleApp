package com.bonushub.crdb.india.view.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import androidx.fragment.app.viewModels
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentInitBinding
import com.bonushub.crdb.india.viewmodel.InitViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.india.db.AppDao
import com.bonushub.crdb.india.utils.*

import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.IDialog
import com.bonushub.pax.utils.KeyExchanger
import com.google.gson.Gson
import com.bonushub.crdb.india.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class InitFragment : Fragment() {
    @Inject
    lateinit var appDao: AppDao
    private val initViewModel : InitViewModel by viewModels()
    private var progressBar : ProgressBar? = null
    private var iDialog: IDialog? = null
    private val fragmentInitBinding :FragmentInitBinding by lazy {
        FragmentInitBinding.inflate(layoutInflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is IDialog) iDialog = context

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ):View? {
        return fragmentInitBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as NavigationActivity).manageTopToolBar(true)

        // for screen awake
        (activity as NavigationActivity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // it's handling for init button is enable or disable ----> it will be enable when Tid length is equal to 8
        fragmentInitBinding.ifProceedBtn.isEnabled = false
        fragmentInitBinding.ifProceedBtn.isClickable = false
        fragmentInitBinding.ifEt.addTextChangedListener(textWatcher)
        fragmentInitBinding.ifEt.transformationMethod = null
       // fragmentInitBinding.ifProceedBtn.setBackgroundResource(R.drawable.edge_button_inactive_init);
        fragmentInitBinding.conLayInitBtn.alpha = .5f

        fragmentInitBinding.ifEt.addTextChangedListener(Utility.OnTextChange {
            fragmentInitBinding.ifProceedBtn.isEnabled = it.length == 8
            if (fragmentInitBinding.ifProceedBtn.isEnabled) {
                //fragmentInitBinding.ifProceedBtn.setBackgroundResource(R.drawable.edge_button_active_init);
                fragmentInitBinding.conLayInitBtn.alpha = 1f
            }
        })

        //region Below Code write App Revision ID to file when first time Init Screen opens
        //in App after that this file will override after settlement:-
        context?.let { writeAppRevisionIDInFile(it) }
        //endregion

        fragmentInitBinding.ifProceedBtn.setOnClickListener {
       iDialog?.showProgress(getString(R.string.please_wait_host))
            initViewModel.insertInfo1(fragmentInitBinding.ifEt.text.toString())
        }
        observeMainViewModel()
    }
// it's for watching length of tid and change the color of proceed button according to that
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            if (start < 8) {
                //fragmentInitBinding?.ifProceedBtn?.setBackgroundResource(R.drawable.edge_button_inactive);
                fragmentInitBinding.conLayInitBtn.alpha = .5f
            }
            else if(start>=8){
               // fragmentInitBinding?.ifProceedBtn?.setBackgroundResource(R.drawable.edge_button_active_init);
                fragmentInitBinding.conLayInitBtn.alpha = 1f

            }
        }
    }
    private fun observeMainViewModel(){

        initViewModel.initData.observe(viewLifecycleOwner, Observer { result ->

            when (result.status) {
                Status.SUCCESS -> {
                    var isStaticQrAvailable=false

                    lifecycleScope.launch(Dispatchers.IO){
                        Utility().readInitServer(result?.data?.data as java.util.ArrayList<ByteArray>) { result, message ->
                            //--region

                            lifecycleScope.launch(Dispatchers.IO){
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
                                        tpt1?.digiPosTerminalStatus  = responsF57List[4].toString()
                                        tpt1?.digiPosBQRStatus = responsF57List[5].toString()
                                        tpt1?.digiPosUPIStatus =  responsF57List[6].toString()
                                        tpt1?.digiPosSMSpayStatus = responsF57List[7].toString()
                                        tpt1?.digiPosStaticQrDownloadRequired =
                                            responsF57List[8].toString()
                                        tpt1?.digiPosCardCallBackRequired =
                                            responsF57List[9].toString()

                                        logger(
                                            LOG_TAG.DIGIPOS.tag,
                                            "statcic      $responsF57List[8].toString() "
                                        )
                                        if ((tpt1?.digiPosTerminalStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) && (tpt1?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                    || tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode
                                                    || tpt1.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) ){
                                            tpt1.isDigiposActive = "1"
                                        }
                                        else{
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
                                                    val tpt= Field48ResponseTimestamp.getTptData()
                                                    imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                                                    if(imgbm==null || tpt?.digiPosStaticQrDownloadRequired =="1") {
                                                        isStaticQrAvailable=true
                                                    }
                                                }

                                            }

                                        }

                                        //  }

                                        /* else {
                                                logger("DIGI_POS", "DIGI_POS_UNAVAILABLE")
                                            }*/
                                    }else{
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

                                if(isStaticQrAvailable){
                                    // getting static qr from server if required
                                    //withContext(Dispatchers.IO){
                                        getStaticQrFromServerAndSaveToFile(requireActivity()){
                                            // FAIL AND SUCCESS HANDELED IN FUNCTION getStaticQrFromServerAndSaveToFile itself
                                        }
                                    //}

                                }
                                (activity as NavigationActivity).hideProgress()
                               //showToast("Navigation")
                                var checkinitstatus = checkInitializationStatus(appDao)
                                if(checkinitstatus) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        (activity as? NavigationActivity)?.getString(R.string.successfull_init)?.let {
                                            (activity as? NavigationActivity)?.alertBoxMsgWithIconOnly(
                                                R.drawable.ic_success_with_star,
                                                it
                                            )
                                        }
                                    }
                                }
                                else{
                                    (activity as? NavigationActivity)?.transactFragment(DashboardFragment())
                                }
                            }
                            // end region
                      /*      (activity as NavigationActivity).hideProgress()
                            Field48ResponseTimestamp.showToast("Navigation")
                            CoroutineScope(Dispatchers.Main).launch {
                                (activity as NavigationActivity).alertBoxMsgWithIconOnly(R.drawable.ic_tick,
                                    (activity as NavigationActivity).getString(R.string.successfull_init))
                            }*/

                        }

                    }

                }
                Status.ERROR -> {
                    iDialog?.hideProgress()
                    CoroutineScope(Dispatchers.Main).launch {
                        (activity as? NavigationActivity)?.alertBoxWithActionNew("Error", result.error ?: "",
                            R.drawable.ic_info_orange,"OK","",false,false,{},{})
                    }
                }
                Status.LOADING -> {
                    iDialog?.showProgress(getString(R.string.sending_receiving_host))

                }
            }

        })


    }



}