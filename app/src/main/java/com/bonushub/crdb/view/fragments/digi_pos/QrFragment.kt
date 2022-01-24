package com.bonushub.crdb.view.fragments.digi_pos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentQrBinding
import com.bonushub.crdb.model.local.DigiPosDataTable
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.utils.BitmapUtils.convertCompressedByteArrayToBitmap
import com.bonushub.crdb.utils.Field48ResponseTimestamp.deleteDigiposData
import com.bonushub.crdb.utils.Field48ResponseTimestamp.insertOrUpdateDigiposData
import com.bonushub.crdb.view.base.BaseActivityNew
import com.bonushub.crdb.utils.EDashboardItem
import com.bonushub.crdb.utils.printerUtils.PrintUtil
import com.bonushub.pax.utils.KeyExchanger.Companion.getDigiPosStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class QrFragment : Fragment() {

    var binding:FragmentQrBinding? = null
    lateinit var transactionType: EDashboardItem
    private lateinit var QrBytes: ByteArray
    private lateinit var digiPosTabledata: DigiPosDataTable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentQrBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem

        digiPosTabledata = arguments?.getParcelable("tabledata") ?: DigiPosDataTable()
        //getting byte[] from argument
        try {
            QrBytes = arguments?.getByteArray("QrByteArray") as ByteArray
            //convert byte[] to bitmap
            val qrCodeBitmap = convertCompressedByteArrayToBitmap(QrBytes)
            //showing bitmap on imageView
            binding?.imgViewQr?.setImageBitmap(qrCodeBitmap)
        }catch (ex:Exception)
        {
            ex.printStackTrace()
        }

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                //parentFragmentManager.popBackStackImmediate()
                parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

//        val paymsg = when (transactionType) {
//            EDashboardItem.DYNAMIC_QR -> {
//
//                getString(R.string.scan_qr_code_to_pay_n_nwould_you_like_to_check_payment_status_now)
//            }
//            else -> {
//                binding?.btnNo?.visibility = View.GONE
//                binding?.btnYes?.text = getString(R.string.key_ok)
//                getString(R.string.scan_qr_pay)
//
//            }
//        }
//        binding?.payMsg?.text = paymsg
//        binding?.payMsg2?.visibility = View.GONE


        binding?.btnNo?.setOnClickListener {
            logger("btnNo","click","e")
           // parentFragmentManager.popBackStackImmediate()
            parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);

        }

        binding?.btnYes?.setOnClickListener {
            logger("btnYes","click","e")

            when (transactionType) {
                EDashboardItem.BHARAT_QR -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            (activity as BaseActivityNew).showProgress()
                        }
                        val req57 = EnumDigiPosProcess.GET_STATUS.code + "^" + digiPosTabledata.partnerTxnId + "^"

                        getDigiPosStatus(
                            req57,
                            EnumDigiPosProcessingCode.DIGIPOSPROCODE.code,
                            false
                        ) { isSuccess, responseMsg, responsef57, fullResponse ->
                            try {
                                (activity as BaseActivityNew).hideProgress()
                                if (isSuccess) {
                                    val statusRespDataList =
                                        responsef57.split("^")
                                    if( statusRespDataList[1]== EDigiPosTerminalStatusResponseCodes.SuccessString.statusCode){
                                        val tabledata =
                                            DigiPosDataTable()
                                        tabledata.requestType =
                                            statusRespDataList[0].toInt()
                                        //  tabledata.partnerTxnId = statusRespDataList[1]
                                        tabledata.status =
                                            statusRespDataList[1]
                                        tabledata.statusMsg =
                                            statusRespDataList[2]
                                        tabledata.statusCode =
                                            statusRespDataList[3]
                                        tabledata.mTxnId =
                                            statusRespDataList[4]
                                        tabledata.partnerTxnId =
                                            statusRespDataList[6]
                                        tabledata.transactionTimeStamp =
                                            statusRespDataList[7]
                                        tabledata.displayFormatedDate =
                                            getDateInDisplayFormatDigipos(
                                                statusRespDataList[7]
                                            )
                                        val dateTime =
                                            statusRespDataList[7].split(
                                                " "
                                            )
                                        tabledata.txnDate = dateTime[0]
                                        tabledata.txnTime = dateTime[1]
                                        tabledata.amount =
                                            statusRespDataList[8]
                                        tabledata.paymentMode =
                                            statusRespDataList[9]
                                        tabledata.customerMobileNumber =
                                            statusRespDataList[10]
                                        tabledata.description =
                                            statusRespDataList[11]
                                        tabledata.pgwTxnId =
                                            statusRespDataList[12]


                                        when (statusRespDataList[5]) {
                                            EDigiPosPaymentStatus.Pending.desciption -> {
                                                tabledata.txnStatus =
                                                    statusRespDataList[5]

                                                insertOrUpdateDigiposData(
                                                    tabledata
                                                )
                                                Log.e("F56->>", responsef57)
                                                lifecycleScope.launch(Dispatchers.Main){
                                                ToastUtils.showToast(requireContext(),getString(R.string.txn_status_still_pending))
                                                }
                                                lifecycleScope.launch(Dispatchers.Main) {
//                                                    parentFragmentManager.popBackStack(
//                                                        DigiPosMenuFragment::class.java.simpleName,
//                                                        0
//                                                    )
                                                    parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);

                                                }

                                            }

                                            EDigiPosPaymentStatus.Approved.desciption -> {
                                                tabledata.txnStatus =
                                                    statusRespDataList[5]
                                                insertOrUpdateDigiposData(
                                                    tabledata
                                                )
                                                Log.e("F56->>", responsef57)

                                                lifecycleScope.launch(Dispatchers.Main){
                                                (activity as BaseActivityNew).alertBoxMsgWithIconOnly(R.drawable.ic_tick,"Transaction Approved")
                                                    }
                                                //txnSuccessToast(activity as Context)
                                                // kushal
                                                PrintUtil(context).printSMSUPIChagreSlip(
                                                    tabledata,
                                                    EPrintCopyType.MERCHANT,
                                                    context
                                                ) { alertCB, printingFail ->
                                                    //context.hideProgress()
                                                    if (!alertCB) {
                                                        lifecycleScope.launch(Dispatchers.Main) {
                                                            parentFragmentManager.popBackStack(
                                                                DigiPosMenuFragment::class.java.simpleName,
                                                                0
                                                            );

                                                        }

                                                    }
                                                }
                                            }
                                            else -> {
                                                deleteDigiposData(
                                                    tabledata.partnerTxnId
                                                )
                                                lifecycleScope.launch(Dispatchers.Main){
                                                ToastUtils.showToast(requireContext(),statusRespDataList[5])
                                                }

                                            }
                                        }
                                    }
                                    else
                                    {
                                        lifecycleScope.launch(
                                            Dispatchers.Main
                                        ) {
                                            (activity as BaseActivityNew).alertBoxWithAction(
                                                getString(R.string.transaction_failed_msg),
                                                statusRespDataList[1],
                                                false,
                                                getString(R.string.positive_button_ok),
                                                { alertPositiveCallback ->
                                                    if (alertPositiveCallback) {
                                                        deleteDigiposData(
                                                            digiPosTabledata.partnerTxnId)
                                                        lifecycleScope.launch(Dispatchers.Main) {
                                                            parentFragmentManager.popBackStack(
                                                                DigiPosMenuFragment::class.java.simpleName,
                                                                0
                                                            )}
                                                    }
                                                },
                                                {})
                                        }
                                    }
                                } else
                                {
                                    lifecycleScope.launch(
                                        Dispatchers.Main
                                    ) {
                                        (activity as BaseActivityNew).alertBoxWithAction(
                                            getString(R.string.transaction_failed_msg),
                                            responseMsg,
                                            false,
                                            getString(R.string.positive_button_ok),
                                            { alertPositiveCallback ->
                                                if (alertPositiveCallback) {
                                                    deleteDigiposData(
                                                        digiPosTabledata.partnerTxnId
                                                    )
                                                    //parentFragmentManager.popBackStack()
                                                    parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);
                                                }
                                            },
                                            {})
                                    }
                                }

                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                logger(
                                    LOG_TAG.DIGIPOS.tag,
                                    "Somethig wrong... in response data field 57"
                                )
                            }
                        }
                    }
                }
                EDashboardItem.STATIC_QR -> {
                    // below commented code is for check the deleting qr code
                    /* activity?.deleteFile("$QR_FILE_NAME.jpg")
                     var imgbm: Bitmap? = null
                     runBlocking {
                         imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                     }
                     if (imgbm == null) {
                         logger("StaticQr", "  DELETED SUCCESS", "e")
                     }*/
                   // parentFragmentManager.popBackStack()
                    parentFragmentManager.popBackStack(DigiPosMenuFragment::class.java.simpleName, 0);
                }

                else -> {

                }
            }
        }

    }
}