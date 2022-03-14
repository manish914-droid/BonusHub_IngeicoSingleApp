package com.bonushub.crdb.india.view.fragments.digi_pos

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.india.databinding.FragmentDigiPosMenuBinding
import com.bonushub.crdb.india.databinding.ItemDigiPosBinding
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.BitmapUtils.convertBitmapToByteArray
import com.bonushub.crdb.india.utils.Field48ResponseTimestamp.getTptData
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class DigiPosMenuFragment : Fragment(), IDigiPosMenuItemClick {

    var binding:FragmentDigiPosMenuBinding? = null
    private lateinit var transactionType: EDashboardItem

    private val digiPosItem: MutableList<DigiPosItem> by lazy { mutableListOf<DigiPosItem>() }
    private var iDigiPosMenuItemClick: IDigiPosMenuItemClick? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDigiPosMenuBinding.inflate(inflater,container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionType = arguments?.getSerializable("type") as EDashboardItem

        binding?.subHeaderView?.subHeaderText?.text = transactionType.title
        binding?.subHeaderView?.headerImage?.setImageResource(transactionType.res)

        binding?.subHeaderView?.backImageButton?.setOnClickListener {
            try {
                parentFragmentManager.popBackStackImmediate()
            }catch (ex:Exception)
            {
                ex.printStackTrace()
            }
        }

        iDigiPosMenuItemClick = this
        digiPosItem.clear()
       // digiPosItem.addAll(DigiPosItem.values())

        // region get tpt data
        val tpt = getTptData()
        if (tpt?.digiPosUPIStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
            //binding?.upiBtn?.visibility = View.VISIBLE
            digiPosItem.add(DigiPosItem.UPI)
        }
        if (tpt?.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
//            binding?.staticQrBtn?.visibility = View.VISIBLE
//            binding?.dynamicQrBtn?.visibility = View.VISIBLE
            digiPosItem.add(DigiPosItem.BHARAT_QR)
            digiPosItem.add(DigiPosItem.STATIC_QR)

        }
        if (tpt?.digiPosSMSpayStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
           // binding?.smsPayBtn?.visibility = View.VISIBLE
            digiPosItem.add(DigiPosItem.SMS_PAY)
        }

        digiPosItem.add(DigiPosItem.PENDING_TXN)
        digiPosItem.add(DigiPosItem.TXN_LIST)
        // end region

        setupRecyclerview()

        /*try {
            val h1 = hexString2String("4E4F494441202020202020202020202020202020202020")
            val h2 = hexString2String("4E4F494441202020202020202055502020202020202020")
            logger("h1",h1)
            logger("h2",h2)
        }catch (ex:Exception)
        {
            ex.printStackTrace()
        }*/
    }

    private fun setupRecyclerview(){
        lifecycleScope.launch(Dispatchers.Main) {
            binding?.let {
                it.recyclerView.layoutManager = GridLayoutManager(activity, 1)
                it.recyclerView.adapter = DigiPosMenuAdapter(iDigiPosMenuItemClick, digiPosItem)
            }

        }
    }

    override fun digiPosMenuItemClick(digiPosItem: DigiPosItem) {

        when(digiPosItem)
        {
            DigiPosItem.UPI ->{
                (activity as NavigationActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", EDashboardItem.UPI)
                    }
                },false)
            }

            DigiPosItem.BHARAT_QR ->{
                (activity as NavigationActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", EDashboardItem.BHARAT_QR)
                    }
                },false)
            }

            DigiPosItem.STATIC_QR ->{

                var transactionTypeTemp = transactionType
                var imgbm: Bitmap? = null
                runBlocking(Dispatchers.IO) {
                    imgbm = loadStaticQrFromInternalStorage() // it return null when file not exist
                    if(imgbm!=null) {
                        val bmBytes = convertBitmapToByteArray(imgbm)
                        logger("StaticQr", "Already parsed Bitmap", "e")
                        (activity as NavigationActivity).transactFragment(QrFragment().apply {
                            arguments = Bundle().apply {
                                putByteArray("QrByteArray", bmBytes)
                                putSerializable("type",transactionTypeTemp)
                                putSerializable("type", EDashboardItem.STATIC_QR)
                                // putParcelable("tabledata",tabledata)
                            }
                        },false)
                    }
                    else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            getStaticQrFromServerAndSaveToFile(activity as BaseActivityNew) {
                                if (it) {
                                    logger(
                                        "StaticQr",
                                        "Get Static Qr from server and  saves to file success ",
                                        "e"
                                    )
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        imgbm =
                                            loadStaticQrFromInternalStorage() // it return null when file not exist
                                        if (imgbm != null) {
                                            val bmBytes = convertBitmapToByteArray(imgbm)
                                            logger("StaticQr", "Already parsed Bitmap", "e")
                                            (activity as NavigationActivity).transactFragment(QrFragment().apply {
                                                arguments = Bundle().apply {
                                                    putByteArray("QrByteArray", bmBytes)
                                                    putSerializable("type", transactionTypeTemp)
                                                    putSerializable("type", EDashboardItem.STATIC_QR)
                                                    // putParcelable("tabledata",tabledata)
                                                }
                                            },false)
                                        }
                                    }

                                } else {
                                    lifecycleScope.launch(Dispatchers.Main){
                                        ToastUtils.showToast(requireContext(),"Static Qr not available.")
                                    }
                                    logger(
                                        "StaticQr",
                                        "Get Static Qr from server and  file not successfully saved",
                                        "e"
                                    )

                                }

                            }
                        }
                    }
                }

                /*(activity as NavigationActivity).transactFragment(QrFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", EDashboardItem.STATIC_QR)
                    }
                })*/

            }

            DigiPosItem.SMS_PAY ->{
                (activity as NavigationActivity).transactFragment(UpiSmsDynamicPayQrInputDetailFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", EDashboardItem.SMS_PAY)
                    }
                },false)
            }

            DigiPosItem.PENDING_TXN ->{
                (activity as NavigationActivity).transactFragment(PendingTxnFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", EDashboardItem.PENDING_TXN)
                    }
                },false)
            }

            DigiPosItem.TXN_LIST ->{
                (activity as NavigationActivity).transactFragment(TxnListFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("type", EDashboardItem.TXN_LIST)
                    }
                },false)
            }
        }

    }
}

interface IDigiPosMenuItemClick{

    fun digiPosMenuItemClick(digiPosItem: DigiPosItem)
}

// addapter
class DigiPosMenuAdapter(private var iDigiPosMenuItemClick: IDigiPosMenuItemClick?, private val digiPosItem: MutableList<DigiPosItem>) : RecyclerView.Adapter<DigiPosMenuAdapter.DigiPosMenuViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DigiPosMenuViewHolder {

        val itemBinding = ItemDigiPosBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return DigiPosMenuViewHolder(itemBinding)
    }

    override fun getItemCount(): Int = digiPosItem.size

    override fun onBindViewHolder(holder: DigiPosMenuViewHolder, position: Int) {

        val model = digiPosItem[position]
        holder.viewBinding.textView.text = model.title

        holder.viewBinding.imgViewIcon.setImageResource(model.res)


        holder.viewBinding.relLayParent.setOnClickListener {

            iDigiPosMenuItemClick?.digiPosMenuItemClick(model)

        }

    }

    inner class DigiPosMenuViewHolder(val viewBinding: ItemDigiPosBinding) : RecyclerView.ViewHolder(viewBinding.root)
}