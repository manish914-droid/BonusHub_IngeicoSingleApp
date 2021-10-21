package com.bonushub.crdb.view.activity

import android.os.Bundle
import android.os.RemoteException
import com.bonushub.crdb.R
import com.bonushub.crdb.R.*
import com.bonushub.crdb.utils.DeviceHelper
import com.bonushub.crdb.utils.ingenico.BytesUtil
import com.bonushub.crdb.utils.ingenico.DemoConfig
import com.bonushub.crdb.utils.ingenico.TLV
import com.bonushub.crdb.utils.ingenico.TLVList
import com.bonushub.crdb.view.base.emv.BaseEMVActivity
import com.usdk.apiservice.aidl.emv.*

class TransactionActivity : BaseEMVActivity(),DeviceHelper.ServiceReadyListener {


    private var emvProcessOptimization = false

    override fun onCreateView(savedInstanceState: Bundle?) {
        super.onCreateView(savedInstanceState)

        emv.setEMVProcessOptimization(true)
        emvProcessOptimization = true

        outputBlackText("\n>>>>>>>>>> start trade <<<<<<<<<")
        startEMV(emvOption)
    }

    override fun onStop() {
        outputBlackText("\n>>>>>>>>>> stop trade <<<<<<<<<")
        stopEMV()
        stopSearch()
        halt()
        super.onStop()
    }

    @Throws(RemoteException::class)
    override fun doInitEMV() {
        super.doInitEMV()
        if (emvProcessOptimization) {
            manageCAPKey()
        }
    }

    @Throws(RemoteException::class)
    override fun doWaitCard(flag: Int) {
        outputText("=> onWaitCard | flag = $flag")
        when (flag) {
            WaitCardFlag.NORMAL -> searchCard(Runnable {
                if (emvProcessOptimization) {
                    return@Runnable
                }
                respondCard()
            })
            else -> super.doWaitCard(flag)
        }
    }

    fun searchCard(next: Runnable) {
        outputBlueText("******  search card ******")
        outputRedText(getString(string.insert_pass_swipe_card))
        try {
            emv.searchCard(cardOption.toBundle(), DemoConfig.TIMEOUT, object : SearchCardListener.Stub() {
                override fun onCardPass(cardType: Int) {
                    outputText("=> onCardPass | cardType = $cardType")
                    next.run()
                }

                override fun onCardInsert() {
                    outputText("=> onCardInsert")
                    next.run()
                }

                override fun onCardSwiped(track: Bundle) {
                    outputText("=> onCardSwiped")
                    outputText("==> Pan: " + track.getString("PAN"))
                    outputText("==> Track 1: " + track.getString("TRACK1"))
                    outputText("==> Track 2: " + track.getString("TRACK2"))
                    outputText("==> Track 3: " + track.getString("TRACK3"))
                    outputText("==> Service code: " + track.getString("SERVICE_CODE"))
                    outputText("==> Card exprited date: " + track.getString("EXPIRED_DATE"))
                    val trackStates = track.getIntArray("TRACK_STATES")
                    for (i in trackStates!!.indices) {
                        outputText(String.format("==> Track%s State：%d", i + 1, trackStates[i]))
                    }
                    stopEMV()
                }

                override fun onTimeout() {
                    outputRedText("=> onTimeout")
                    stopEMV()
                }

                override fun onError(code: Int, message: String) {
                    outputRedText(String.format("=> onError | %s[0x%02X]", message, code))
                    stopEMV()
                }
            })
        } catch (e: Exception) {
            handleException(e)
        }
    }

    @Throws(RemoteException::class)
    override fun doReadRecord(record: CardRecord?) {
        if (emvProcessOptimization) {
            // no need to setCAPubKey
        } else {
            // TODO setCAPubKey
        }
        super.doReadRecord(record)
    }

    @Throws(RemoteException::class)
    private fun manageCAPKey() {
        emv.manageCAPubKey(ActionFlag.CLEAR, null)
        outputBlueText("****** manage CAPKey ******")
        val ca = arrayOf(
                "9F0605A0000000659F220109DF05083230323931323331DF060101DF070101DF028180B72A8FEF5B27F2B550398FDCC256F714BAD497FF56094B7408328CB626AA6F0E6A9DF8388EB9887BC930170BCC1213E90FC070D52C8DCD0FF9E10FAD36801FE93FC998A721705091F18BC7C98241CADC15A2B9DA7FB963142C0AB640D5D0135E77EBAE95AF1B4FEFADCF9C012366BDDA0455C1564A68810D7127676D493890BDDF040103DF03144410C6D51C2F83ADFD92528FA6E38A32DF048D0A",
                "9F0605A0000000659F220110DF05083230323231323331DF060101DF070101DF02819099B63464EE0B4957E4FD23BF923D12B61469B8FFF8814346B2ED6A780F8988EA9CF0433BC1E655F05EFA66D0C98098F25B659D7A25B8478A36E489760D071F54CDF7416948ED733D816349DA2AADDA227EE45936203CBF628CD033AABA5E5A6E4AE37FBACB4611B4113ED427529C636F6C3304F8ABDD6D9AD660516AE87F7F2DDF1D2FA44C164727E56BBC9BA23C0285DF040103DF0314C75E5210CBE6E8F0594A0F1911B07418CADB5BAB",
                "9F0605A0000000659F220112DF05083230323431323331DF060101DF070101DF0281B0ADF05CD4C5B490B087C3467B0F3043750438848461288BFEFD6198DD576DC3AD7A7CFA07DBA128C247A8EAB30DC3A30B02FCD7F1C8167965463626FEFF8AB1AA61A4B9AEF09EE12B009842A1ABA01ADB4A2B170668781EC92B60F605FD12B2B2A6F1FE734BE510F60DC5D189E401451B62B4E06851EC20EBFF4522AACC2E9CDC89BC5D8CDE5D633CFD77220FF6BBD4A9B441473CC3C6FEFC8D13E57C3DE97E1269FA19F655215B23563ED1D1860D8681DF040103DF0314874B379B7F607DC1CAF87A19E400B6A9E25163E8",
                "9F0605A0000000659F220114DF05083230323631323331DF060101DF070101DF0281F8AEED55B9EE00E1ECEB045F61D2DA9A66AB637B43FB5CDBDB22A2FBB25BE061E937E38244EE5132F530144A3F268907D8FD648863F5A96FED7E42089E93457ADC0E1BC89C58A0DB72675FBC47FEE9FF33C16ADE6D341936B06B6A6F5EF6F66A4EDD981DF75DA8399C3053F430ECA342437C23AF423A211AC9F58EAF09B0F837DE9D86C7109DB1646561AA5AF0289AF5514AC64BC2D9D36A179BB8A7971E2BFA03A9E4B847FD3D63524D43A0E8003547B94A8A75E519DF3177D0A60BC0B4BAB1EA59A2CBB4D2D62354E926E9C7D3BE4181E81BA60F8285A896D17DA8C3242481B6C405769A39D547C74ED9FF95A70A796046B5EFF36682DC29DF040103DF0314C0D15F6CD957E491DB56DCDD1CA87A03EBE06B7B",
                "9F0605A0000003339F220101DF05083230323931323331DF060101DF070101DF028180BBE9066D2517511D239C7BFA77884144AE20C7372F515147E8CE6537C54C0A6A4D45F8CA4D290870CDA59F1344EF71D17D3F35D92F3F06778D0D511EC2A7DC4FFEADF4FB1253CE37A7B2B5A3741227BEF72524DA7A2B7B1CB426BEE27BC513B0CB11AB99BC1BC61DF5AC6CC4D831D0848788CD74F6D543AD37C5A2B4C5D5A93BDF040103DF0314E881E390675D44C2DD81234DCE29C3F5AB2297A0",
                "9F0605A0000003339F220102DF05083230323431323331DF060101DF070101DF028190A3767ABD1B6AA69D7F3FBF28C092DE9ED1E658BA5F0909AF7A1CCD907373B7210FDEB16287BA8E78E1529F443976FD27F991EC67D95E5F4E96B127CAB2396A94D6E45CDA44CA4C4867570D6B07542F8D4BF9FF97975DB9891515E66F525D2B3CBEB6D662BFB6C3F338E93B02142BFC44173A3764C56AADD202075B26DC2F9F7D7AE74BD7D00FD05EE430032663D27A57DF040103DF031403BB335A8549A03B87AB089D006F60852E4B8060",
                "9F0605A0000003339F220103DF05083230323731323331DF060101DF070101DF0281B0B0627DEE87864F9C18C13B9A1F025448BF13C58380C91F4CEBA9F9BCB214FF8414E9B59D6ABA10F941C7331768F47B2127907D857FA39AAF8CE02045DD01619D689EE731C551159BE7EB2D51A372FF56B556E5CB2FDE36E23073A44CA215D6C26CA68847B388E39520E0026E62294B557D6470440CA0AEFC9438C923AEC9B2098D6D3A1AF5E8B1DE36F4B53040109D89B77CAFAF70C26C601ABDF59EEC0FDC8A99089140CD2E817E335175B03B7AA33DDF040103DF031487F0CD7C0E86F38F89A66F8C47071A8B88586F26")
        for (item in ca) {
            val tlvList: TLVList = TLVList.fromBinary(item)
            val tag9F06: TLV = tlvList.getTLV("9F06")
            val rid: ByteArray = tag9F06.getBytesValue()
            val tag9F22: TLV = tlvList.getTLV("9F22")
            val index: Byte = tag9F22.getByteValue()
            val tagDF05: TLV = tlvList.getTLV("DF05")
            val expiredDate: ByteArray = tagDF05.getBCDValue()
            val tagDF02: TLV = tlvList.getTLV("DF02")
            val mod: ByteArray = tagDF02.getBytesValue()
            val capKey = CAPublicKey()
            capKey.rid = rid
            capKey.index = index
            capKey.expDate = expiredDate
            capKey.mod = mod
            if (tlvList.contains("DF04")) {
                val tagDF04: TLV = tlvList.getTLV("DF04")
                capKey.exp = tagDF04.getBytesValue()
            }
            if (tlvList.contains("DF03")) {
                val tagDF03: TLV = tlvList.getTLV("DF03")
                capKey.hash = tagDF03.getBytesValue()
                capKey.hashFlag = 0x01.toByte()
            } else {
                capKey.hashFlag = 0x00.toByte()
            }
            val ret = emv.manageCAPubKey(ActionFlag.ADD, capKey)
            outputResult(ret, "=> add CAPKey rid = : " + BytesUtil.bytes2HexString(rid).toString() + ", index = " + index)
        }
    }

    override fun onReady(version: String?) {
        TODO("Not yet implemented")
    }

}