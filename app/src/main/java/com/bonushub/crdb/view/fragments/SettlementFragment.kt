package com.bonushub.crdb.view.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.databinding.FragmentInitBinding
import com.bonushub.crdb.databinding.FragmentSettlementBinding
import com.bonushub.crdb.databinding.ItemSettlementBinding
import com.bonushub.crdb.db.AppDao
import com.bonushub.crdb.disputetransaction.CreateSettlementPacket
import com.bonushub.crdb.model.local.BatchFileDataTable
import com.bonushub.crdb.utils.*
import com.bonushub.crdb.view.activity.NavigationActivity
import com.bonushub.crdb.view.base.IDialog
import com.bonushub.crdb.viewmodel.InitViewModel
import com.bonushub.crdb.viewmodel.SettlementViewModel
import com.ingenico.hdfcpayment.listener.OnOperationListener
import com.ingenico.hdfcpayment.request.SettlementRequest
import com.ingenico.hdfcpayment.response.OperationResult
import com.ingenico.hdfcpayment.response.SettlementResponse
import com.mindorks.example.coroutines.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class SettlementFragment : Fragment() {

    private val fragmensettlementBinding : FragmentSettlementBinding by lazy {
        FragmentSettlementBinding.inflate(layoutInflater)
    }
    @Inject
    lateinit var appDao: AppDao
    private val settlementViewModel : SettlementViewModel by viewModels()
    private val dataList: MutableList<BatchFileDataTable> by lazy { mutableListOf<BatchFileDataTable>() }
    private val settlementAdapter by lazy { SettlementAdapter(dataList) }
    private var settlementByteArray: ByteArray? = null
    private var navController: NavController? = null
    private var iDialog: IDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IDialog) iDialog = context
    }

    override fun onDetach() {
        super.onDetach()
        iDialog = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return fragmensettlementBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
       // (activity as NavigationActivity).showBottomNavigationBar(isShow = false)
        fragmensettlementBinding.subHeaderView?.subHeaderText?.text = getString(R.string.settlement)
        fragmensettlementBinding.subHeaderView?.backImageButton?.setOnClickListener { navController?.popBackStack() }

       settlementViewModel.insertdata()

        //region===============Get Batch Data from HDFCViewModal:-
        settlementViewModel.getBatchData()?.observe(requireActivity()) { batchData ->
                Log.d("TPT Data:- ", batchData.toString())
                dataList.clear()
                dataList.addAll(batchData as MutableList<BatchFileDataTable>)
                setUpRecyclerView()
            }
        //endregion
        //region================================RecyclerView On Scroll Extended Floating Button Hide/Show:-
        fragmensettlementBinding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->

            //Scroll Down Condition:-
            if (scrollY > oldScrollY + 20 && fragmensettlementBinding.settlementFloatingButton?.isExtended == true)
                fragmensettlementBinding?.settlementFloatingButton?.shrink()

            //Scroll Up Condition:-
            if (scrollY < oldScrollY - 20 && fragmensettlementBinding?.settlementFloatingButton?.isExtended != true)
                fragmensettlementBinding?.settlementFloatingButton?.extend()

            //At the Top Condition:-
            if (scrollY == 0)
                fragmensettlementBinding?.settlementFloatingButton?.extend()
        })
        //endregion

        //region========================OnClick Event of SettleBatch Button:-
        fragmensettlementBinding?.settlementFloatingButton?.setOnClickListener {
            (activity as NavigationActivity).alertBoxWithAction(
                getString(R.string.settlement), getString(R.string.do_you_want_to_settle_batch), true, getString(R.string.yes), {
                    settlementViewModel.settlementResponse()
                    settlementViewModel.ingenciosettlement.observe(requireActivity()){ result ->

                        when (result.status) {
                            Status.SUCCESS -> {
                                CoroutineScope(Dispatchers.IO).launch{
                                    val data = CreateSettlementPacket(appDao).createSettlementISOPacket()
                                    settlementByteArray = data.generateIsoByteRequest()
                                    try {
                                        (activity as NavigationActivity).settleBatch(settlementByteArray) {
                                        }
                                    } catch (ex: Exception) {
                                        (activity as NavigationActivity).hideProgress()
                                        ex.printStackTrace()
                                    }
                                }
                                Toast.makeText(activity,"Sucess called  ${result.message}", Toast.LENGTH_LONG).show()
                            }
                            Status.ERROR -> {

                                Toast.makeText(activity,"Error called  ${result.error}", Toast.LENGTH_LONG).show()
                            }
                            Status.LOADING -> {
                                Toast.makeText(activity,"Loading called  ${result.message}", Toast.LENGTH_LONG).show()


                            }
                        }

                    }

                },
                {})
        }
        //endregion

    }
    //region====================================SetUp RecyclerView:-
    private fun setUpRecyclerView() {
        if (dataList.size > 0) {
            fragmensettlementBinding?.settlementFloatingButton?.visibility = View.VISIBLE
            fragmensettlementBinding?.settlementRv?.visibility = View.VISIBLE
            fragmensettlementBinding?.lvHeadingView?.visibility = View.VISIBLE

            fragmensettlementBinding?.settlementRv?.apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = settlementAdapter
            }
        } else {
            fragmensettlementBinding?.settlementFloatingButton?.visibility = View.GONE
            fragmensettlementBinding?.settlementRv?.visibility = View.GONE
            fragmensettlementBinding?.lvHeadingView?.visibility = View.GONE
            fragmensettlementBinding?.emptyViewPlaceholder?.visibility = View.VISIBLE
        }
    }
    //endregion
}

internal class SettlementAdapter(private val list: List<BatchFileDataTable>) :
    RecyclerView.Adapter<SettlementAdapter.SettlementHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SettlementHolder {
        val binding : ItemSettlementBinding by lazy {
            ItemSettlementBinding.inflate(LayoutInflater.from(p0.context),p0,false)
        }

        return SettlementHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: SettlementHolder, p1: Int) {
        holder.binding.tvInvoiceNumber.text = invoiceWithPadding(list[p1].invoiceNumber)
        val amount = "%.2f".format(list[p1].totalAmount.toDouble())
        holder.binding.tvBaseAmount.text = amount
        holder.binding.tvTransactionType.text = getTransactionTypeName(list[p1].transactionType)
        holder.binding.tvTransactionDate.text = list[p1].date
    }

    inner class SettlementHolder(val binding: ItemSettlementBinding) :
        RecyclerView.ViewHolder(binding.root)
}