package com.bonushub.crdb.india.view.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.databinding.FragmentInitConfirmBinding
import com.bonushub.crdb.india.utils.ToastUtils
import com.bonushub.crdb.india.utils.dialog.DialogUtilsNew1
import com.bonushub.crdb.india.utils.dialog.OnClickDialogOkCancel
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.viewmodel.BankFunctionsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InitConfirmFragment : Fragment() {

    lateinit var binding:FragmentInitConfirmBinding
    private val bankFunctionsViewModel : BankFunctionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentInitConfirmBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.txtViewInitTerminal?.setOnClickListener {

            DialogUtilsNew1.showDialog(activity,getString(R.string.super_admin_password),getString(R.string.hint_enter_super_admin_password),onClickDialogOkCancel, false)

        }


    }

    var onClickDialogOkCancel: OnClickDialogOkCancel = object : OnClickDialogOkCancel {

        override fun onClickOk(dialog: Dialog, password:String) {

            //dialogSuperAdminPassword = dialog

            bankFunctionsViewModel.isSuperAdminPassword(password)?.observe(viewLifecycleOwner) {

                if (it) {
                    dialog?.dismiss()
                    (activity as NavigationActivity).transactFragment(
                        InitFragment(),
                        true
                    )
                } else {
                    ToastUtils.showToast(requireContext(), R.string.invalid_password)
                }
            }

        }

        override fun onClickCancel() {

        }

    }
}