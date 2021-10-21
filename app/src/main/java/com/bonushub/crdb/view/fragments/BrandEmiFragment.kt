package com.bonushub.crdb.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bonushub.crdb.R
import com.bonushub.crdb.repository.GenericResponse
import com.bonushub.crdb.repository.ServerRepository
import com.bonushub.crdb.serverApi.RemoteService
import com.bonushub.crdb.utils.ToastUtils
import com.bonushub.crdb.viewmodel.BrandEmiViewModel
import com.bonushub.crdb.viewmodel.viewModelFactory.BrandEmiViewModelFactory
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrandEmiFragment : Fragment() {
    lateinit var brandEmiViewModel: BrandEmiViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_brand_emi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val remoteService = RemoteService()

        val serverRepository = ServerRepository(remoteService)
        brandEmiViewModel = ViewModelProvider(this, BrandEmiViewModelFactory(serverRepository)).get(
            BrandEmiViewModel::class.java)

        brandEmiViewModel.brandEMIMasterSubCategoryLivedata.observe(viewLifecycleOwner, {
            when (val genericResp = it) {
                is GenericResponse.Success -> {
                    ToastUtils.showToast(activity, genericResp.data.toString())
                  println(Gson().toJson(genericResp.data))
                    // todo setup recyclerview....


                }
                is GenericResponse.Error -> {
                    ToastUtils.showToast(activity, genericResp.errorMessage)
                    println(genericResp.errorMessage.toString())
                }
                is GenericResponse.Loading -> {

                }
            }
        })

    }

}