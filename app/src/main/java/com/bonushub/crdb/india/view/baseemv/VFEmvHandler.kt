package com.bonushub.crdb.india.view.baseemv

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.transactionprocess.CompleteSecondGenAc
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.ingenico.DialogUtil
import com.bonushub.crdb.india.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.utils.ingenico.TLVList
import com.bonushub.crdb.india.view.activity.TransactionActivity
import com.bonushub.crdb.india.vxutils.Utility.byte2HexStr
import com.usdk.apiservice.aidl.data.BytesValue


import com.usdk.apiservice.aidl.emv.*
import com.usdk.apiservice.aidl.pinpad.*
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*


open class VFEmvHandler constructor(): EMVEventHandler.Stub() {

    private var lastCardRecord: CardRecord? = null

    private var pinPad: UPinpad? = null
    private var emv: UEMV? = null
    private lateinit var activity: TransactionActivity
    private lateinit var cardProcessedDataModal: CardProcessedDataModal
    private lateinit var vfEmvHandlerCallback: (CardProcessedDataModal) -> Unit

    constructor(
        pinPad: UPinpad?,
        emv: UEMV?, activity: TransactionActivity, cardProcessedDataModal: CardProcessedDataModal,
        vfEmvHandlerCallback: (CardProcessedDataModal) -> Unit):this()
    {
        this.pinPad = pinPad
        this.emv = emv
        this.activity = activity
        this.cardProcessedDataModal = cardProcessedDataModal
        this.vfEmvHandlerCallback = vfEmvHandlerCallback
    }

    @Throws(RemoteException::class)
    override fun onInitEMV() {
        doInitEMV()

    }

    @Throws(RemoteException::class)
    override fun onWaitCard(flag: Int) {

    }

    @Throws(RemoteException::class)
    override fun onCardChecked(cardType: Int) {
        // Only happen when use startProcess()
    }

    @Throws(RemoteException::class)
    override fun onAppSelect(reSelect: Boolean, list: List<CandidateAID>) {
        doAppSelect(reSelect, list)
    }

    @Throws(RemoteException::class)
    override fun onFinalSelect(finalData: FinalData?) {
        if (finalData != null) {
            doFinalSelect(finalData)
        }
    }

    @Throws(RemoteException::class)
    override fun onReadRecord(cardRecord: CardRecord) {
        lastCardRecord = cardRecord
        doReadRecord(cardRecord)
    }

    @Throws(RemoteException::class)
    override fun onCardHolderVerify(cvmMethod: CVMMethod?) {
        if (cvmMethod != null) {
            doCardHolderVerify(cvmMethod)
        }
    }

    @Throws(RemoteException::class)
    override fun onOnlineProcess(transData: TransData?) {
        if (transData != null) {
            doOnlineProcess(transData)
        }

    }

    @Throws(RemoteException::class)
    override fun onEndProcess(result: Int, transData: TransData?) {
        doEndProcess(result, transData)
    }

    @Throws(RemoteException::class)
    override fun onVerifyOfflinePin(flag: Int, random: ByteArray?, caPublicKey: CAPublicKey?, offlinePinVerifyResult: OfflinePinVerifyResult?) {
        if (offlinePinVerifyResult != null) {
            doVerifyOfflinePin(flag, random, caPublicKey, offlinePinVerifyResult)
        }
    }

    @Throws(RemoteException::class)
    override fun onObtainData(ins: Int, data: ByteArray?) {
        //	outputText("=> onObtainData: instruction is 0x" + Integer.toHexString(ins) + ", data is " + BytesUtil.bytes2HexString(data));
    }

    @Throws(RemoteException::class)
    override fun onSendOut(ins: Int, data: ByteArray?) {
        doSendOut(ins, data!!)

    }

    @Throws(RemoteException::class)
    fun doInitEMV() {
        println("=> onInitEMV ")
        manageAID()
      /*  if (emv!!.setEMVProcessOptimization(true)) {
           // manageCAPKey()
        }*/

        //  init transaction parameters，please refer to transaction parameters
        //  chapter about onInitEMV event in《UEMV develop guide》
        //  For example, if VISA is supported in the current transaction,
        //  the label: DEF_TAG_PSE_FLAG(M) must be set, as follows:
        emv!!.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PSE_FLAG, "03")

        // For example, if AMEX is supported in the current transaction，
        // labels DEF_TAG_PSE_FLAG(M) and DEF_TAG_PPSE_6A82_TURNTO_AIDLIST(M) must be set, as follows：
        // emv.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PSE_FLAG, "03");
        // emv.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PPSE_6A82_TURNTO_AIDLIST, "01");
    }
    @Throws(RemoteException::class)
    protected fun manageAID() {
        println("****** manage AID ******")
        val aids = arrayOf(
            "A000000025",
            "A000000333010106",
            "A000000333010103",
            "A000000333010102",
            "A000000333010101",
            "A0000000651010",
            "A0000000043060",
            "A0000000041010",
            "A000000003101001",
            "A000000003101002",
            "A000000003101004",
            "A0000000031010"
        )
        for (aid in aids) {
            val ret = emv!!.manageAID(ActionFlag.ADD, aid, true)
            println("$ret=> add AID : $aid")
        }
    }

    @Throws(RemoteException::class)
    open fun manageCAPKey() {
        emv!!.manageCAPubKey(ActionFlag.CLEAR, null)
        println("****** manage CAPKey ******")
        val ca = arrayOf(
       /*     "9F0605A0000000659F220109DF05083230323931323331DF060101DF070101DF028180B72A8FEF5B27F2B550398FDCC256F714BAD497FF56094B7408328CB626AA6F0E6A9DF8388EB9887BC930170BCC1213E90FC070D52C8DCD0FF9E10FAD36801FE93FC998A721705091F18BC7C98241CADC15A2B9DA7FB963142C0AB640D5D0135E77EBAE95AF1B4FEFADCF9C012366BDDA0455C1564A68810D7127676D493890BDDF040103DF03144410C6D51C2F83ADFD92528FA6E38A32DF048D0A",
            "9F0605A0000000659F220110DF05083230323231323331DF060101DF070101DF02819099B63464EE0B4957E4FD23BF923D12B61469B8FFF8814346B2ED6A780F8988EA9CF0433BC1E655F05EFA66D0C98098F25B659D7A25B8478A36E489760D071F54CDF7416948ED733D816349DA2AADDA227EE45936203CBF628CD033AABA5E5A6E4AE37FBACB4611B4113ED427529C636F6C3304F8ABDD6D9AD660516AE87F7F2DDF1D2FA44C164727E56BBC9BA23C0285DF040103DF0314C75E5210CBE6E8F0594A0F1911B07418CADB5BAB",
            "9F0605A0000000659F220112DF05083230323431323331DF060101DF070101DF0281B0ADF05CD4C5B490B087C3467B0F3043750438848461288BFEFD6198DD576DC3AD7A7CFA07DBA128C247A8EAB30DC3A30B02FCD7F1C8167965463626FEFF8AB1AA61A4B9AEF09EE12B009842A1ABA01ADB4A2B170668781EC92B60F605FD12B2B2A6F1FE734BE510F60DC5D189E401451B62B4E06851EC20EBFF4522AACC2E9CDC89BC5D8CDE5D633CFD77220FF6BBD4A9B441473CC3C6FEFC8D13E57C3DE97E1269FA19F655215B23563ED1D1860D8681DF040103DF0314874B379B7F607DC1CAF87A19E400B6A9E25163E8",
            "9F0605A0000000659F220114DF05083230323631323331DF060101DF070101DF0281F8AEED55B9EE00E1ECEB045F61D2DA9A66AB637B43FB5CDBDB22A2FBB25BE061E937E38244EE5132F530144A3F268907D8FD648863F5A96FED7E42089E93457ADC0E1BC89C58A0DB72675FBC47FEE9FF33C16ADE6D341936B06B6A6F5EF6F66A4EDD981DF75DA8399C3053F430ECA342437C23AF423A211AC9F58EAF09B0F837DE9D86C7109DB1646561AA5AF0289AF5514AC64BC2D9D36A179BB8A7971E2BFA03A9E4B847FD3D63524D43A0E8003547B94A8A75E519DF3177D0A60BC0B4BAB1EA59A2CBB4D2D62354E926E9C7D3BE4181E81BA60F8285A896D17DA8C3242481B6C405769A39D547C74ED9FF95A70A796046B5EFF36682DC29DF040103DF0314C0D15F6CD957E491DB56DCDD1CA87A03EBE06B7B",
            "9F0605A000000333" +
                    "9F220101" +
                    "DF05083230323931323331" +
                    "DF060101" +
                    "DF070101" +
                    "DF028180BBE9066D2517511D239C7BFA77884144AE20C7372F515147E8CE6537C54C0A6A4D45F8CA4D290870CDA59F1344EF71D17D3F35D92F3F06778D0D511EC2A7DC4FFEADF4FB1253CE37A7B2B5A3741227BEF72524DA7A2B7B1CB426BEE27BC513B0CB11AB99BC1BC61DF5AC6CC4D831D0848788CD74F6D543AD37C5A2B4C5D5A93BDF040103" +
                    "DF0314E881E390675D44C2DD81234DCE29C3F5AB2297A0",
            "9F0605A0000003339F220102DF05083230323431323331DF060101DF070101DF028190A3767ABD1B6AA69D7F3FBF28C092DE9ED1E658BA5F0909AF7A1CCD907373B7210FDEB16287BA8E78E1529F443976FD27F991EC67D95E5F4E96B127CAB2396A94D6E45CDA44CA4C4867570D6B07542F8D4BF9FF97975DB9891515E66F525D2B3CBEB6D662BFB6C3F338E93B02142BFC44173A3764C56AADD202075B26DC2F9F7D7AE74BD7D00FD05EE430032663D27A57DF040103DF031403BB335A8549A03B87AB089D006F60852E4B8060",
            "9F0605A0000003339F220103DF05083230323731323331DF060101DF070101DF0281B0B0627DEE87864F9C18C13B9A1F025448BF13C58380C91F4CEBA9F9BCB214FF8414E9B59D6ABA10F941C7331768F47B2127907D857FA39AAF8CE02045DD01619D689EE731C551159BE7EB2D51A372FF56B556E5CB2FDE36E23073A44CA215D6C26CA68847B388E39520E0026E62294B557D6470440CA0AEFC9438C923AEC9B2098D6D3A1AF5E8B1DE36F4B53040109D89B77CAFAF70C26C601ABDF59EEC0FDC8A99089140CD2E817E335175B03B7AA33DDF040103DF031487F0CD7C0E86F38F89A66F8C47071A8B88586F26"*/

            //Amex Test Cap keys
   "9F0605A0000000259F2201C8DF0503201231DF060101DF070101DF028190BF0CFCED708FB6B048E3014336EA24AA007D7967B8AA4E613D26D015C4FE7805D9DB131CED0D2A8ED504C3B5CCD48C33199E5A5BF644DA043B54DBF60276F05B1750FAB39098C7511D04BABC649482DDCF7CC42C8C435BAB8DD0EB1A620C31111D1AAAF9AF6571EEBD4CF5A08496D57E7ABDBB5180E0A42DA869AB95FB620EFF2641C3702AF3BE0B0C138EAEF202E21DDF040103DF031433BD7A059FAB094939B90A8F35845C9DC779BD50", //c8
"9F0605A0000000259F2201C9DF0503201231DF060101DF070101DF0281B0B362DB5733C15B8797B8ECEE55CB1A371F760E0BEDD3715BB270424FD4EA26062C38C3F4AAA3732A83D36EA8E9602F6683EECC6BAFF63DD2D49014BDE4D6D603CD744206B05B4BAD0C64C63AB3976B5C8CAAF8539549F5921C0B700D5B0F83C4E7E946068BAAAB5463544DB18C63801118F2182EFCC8A1E85E53C2A7AE839A5C6A3CABE73762B70D170AB64AFC6CA482944902611FB0061E09A67ACB77E493D998A0CCF93D81A4F6C0DC6B7DF22E62DBDF040103DF03148E8DFF443D78CD91DE88821D70C98F0638E51E49", //c9
"9F0605A0000000259F2201CADF0503201231DF060101DF070101DF0281F8C23ECBD7119F479C2EE546C123A585D697A7D10B55C2D28BEF0D299C01DC65420A03FE5227ECDECB8025FBC86EEBC1935298C1753AB849936749719591758C315FA150400789BB14FADD6EAE2AD617DA38163199D1BAD5D3F8F6A7A20AEF420ADFE2404D30B219359C6A4952565CCCA6F11EC5BE564B49B0EA5BF5B3DC8C5C6401208D0029C3957A8C5922CBDE39D3A564C6DEBB6BD2AEF91FC27BB3D3892BEB9646DCE2E1EF8581EFFA712158AAEC541C0BBB4B3E279D7DA54E45A0ACC3570E712C9F7CDF985CFAFD382AE13A3B214A9E8E1E71AB1EA707895112ABC3A97D0FCB0AE2EE5C85492B6CFD54885CDD6337E895CC70FB3255E3DF040103DF03146BDA32B1AA171444C7E8F88075A74FBFE845765F" //CA,

        )
        for (item: String in ca) {
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
            val ret = emv!!.manageCAPubKey(ActionFlag.ADD,capKey)
            println("=> add CAPKey rid = : " + BytesUtil.bytes2HexString(rid).toString() + ", index = " + index+ "return type "+ret)
        }
    }

    open fun doAppSelect(reSelect: Boolean, candList: List<CandidateAID>) {
        println("=> onAppSelect: cand AID size = " + candList.size)
        if (candList.size > 1) {
            selectApp(candList, object : DialogUtil.OnSelectListener {
                override fun onCancel() {
                    try {
                        emv!!.stopEMV()
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }

                override fun onSelected(item: Int) {
                    respondAID(candList[item].aid)
                }
            })
        } else {
            respondAID(candList[0].aid)
        }
    }

    protected open fun selectApp(candList: List<CandidateAID>, listener: DialogUtil.OnSelectListener?) {
        val aidInfoList: MutableList<String> = ArrayList()
        for (candAid in candList) {
            aidInfoList.add(String(candAid.apn))
        }
        activity.runOnUiThread {
            DialogUtil.showSelectDialog(
                activity,
                "Please select app",
                aidInfoList,
                0,
                listener
            )
        }
    }

    @SuppressLint("NewApi")
    @Throws(RemoteException::class)
    open fun doFinalSelect(finalData: FinalData) {
        println("=> onFinalSelect | " + EMVInfoUtil.getFinalSelectDesc(finalData))

        val datetime: String = DeviceHelper.getCurentDateTime()
        val splitStr = datetime.split("\\s+".toRegex()).toTypedArray()
        var txnAmount = addPad(cardProcessedDataModal.getTransactionAmount().toString(), "0", 12, true)

        var aidstr = BytesUtil.bytes2HexString(finalData.aid).subSequence(0, 10).toString()

        var tlvList: String? = null
        when (finalData.kernelID) {
            KernelID.EMV.toByte() ->

                if(aidstr == "A000000025"){
                // Parameter settings, see transaction parameters of EMV Contact Level 2 in《UEMV develop guide》
                // For reference only below
                tlvList = StringBuilder()
                    .append("9F0206").append(txnAmount) //Txn Amount
                    .append("9F0306000000000000")       //Other Amount
                    .append("9A03").append(splitStr[0])   //Txn Date - M
                    .append("9F2103").append(splitStr[1]) //Txn Time - M
                    .append("9F410400000001") //Transaction Sequence Counter - 0
                    .append("9F350122")     //Terminal type
                    .append("9F3303E0F8C8")     //Terminal capability
                    .append("9F40056000F0A001")   //additional terminal capability
                    .append("9F1A020356")  //Terminal country code - M
                    .append("5F2A020356") //Terminal currency code - M*/
                    .append("9C0100")       //Transaction type - o
                    .toString();
            }

            KernelID.AMEX.toByte() ->
                tlvList = StringBuilder()
                    .append("9F350122")
                    .append("9F3303E0E8C8")
                    .append("9F40056000F0B001")
                    .append("9F1A020356")
                    .append("5F2A020356")
                    .append("9F09020001")
                    .append("9C0100")
                    .append("9F0206").append(txnAmount) //Txn Amount
                    .append("9F0306000000000000")
                    .append("9A03").append(splitStr[0])   //Txn Date - M
                    .append("9F2103").append(splitStr[1]) //Txn Time - M
                    .append("9F410400000001")
                    .append("DF918111050000000000") // Terminal action code(decline)
                    .append("DF918112050000000000") // Terminal action code(online)
                    .append("DF918110050000000000")  // Terminal action code(default)
                    .append("9F6D01C0")              // Contactless Reader Capabilities
                    .append("9F6E04D8E00000")      //  Enhanced Contactless Reader Capabilities
                    .append("DF812406000000010000") //Terminal Contactless Transaction Limit
                    .append("DF812606000000010000") // Terminal CVM Required Limit
                    .append("DF812306000000010000")  //Terminal Contactless Floor Limit
                    .append("DF81300100")            //Try Again Flag
                    .toString()


            KernelID.PBOC.toByte() ->                // if suport PBOC Ecash，see transaction parameters of PBOC Ecash in《UEMV develop guide》.
                // If support qPBOC, see transaction parameters of QuickPass in《UEMV develop guide》.
                // For reference only below
                tlvList =
                    "9F02060000000001009F03060000000000009A031710209F21031505129F4104000000019F660427004080"
            KernelID.VISA.toByte() ->                // Parameter settings, see transaction parameters of PAYWAVE in《UEMV develop guide》.
                tlvList = StringBuilder()
                    .append("9C0100")
                    .append("9F0206000000000100")
                    .append("9A03171020")
                    .append("9F2103150512")
                    .append("9F410400000001")
                    .append("9F350122")
                    .append("9F1A020156")
                    .append("5F2A020156")
                    .append("9F1B0400003A98")
                    .append("9F660436004000")
                    .append("DF06027C00")
                    .append("DF812406000000100000")
                    .append("DF812306000000100000")
                    .append("DF812606000000100000")
                    .append("DF918165050100000000")
                    .append("DF040102")
                    .append("DF810602C000")
                    .append("DF9181040100").toString()
            KernelID.MASTER.toByte() ->                // Parameter settings, see transaction parameters of PAYPASS in《UEMV develop guide》.
                tlvList = StringBuilder()
                    .append("9F350122")
                    .append("9F3303E0F8C8")
                    .append("9F40056000F0A001")
                    .append("9A03171020")
                    .append("9F2103150512")
                    .append("9F0206000000000100")
                    .append("9F1A020156")
                    .append("5F2A020156")
                    .append("9C0100")
                    .append("DF918111050000000000")
                    .append("DF91811205FFFFFFFFFF")
                    .append("DF91811005FFFFFFFFFF")
                    .append("DF9182010102")
                    .append("DF9182020100")
                    .append("DF9181150100")
                    .append("DF9182040100")
                    .append("DF812406000000010000")
                    .append("DF812506000000010000")
                    .append("DF812606000000010000")
                    .append("DF812306000000010000")
                    .append("DF9182050160")
                    .append("DF9182060160")
                    .append("DF9182070120")
                    .append("DF9182080120").toString()
            KernelID.AMEX.toByte() ->
                tlvList = StringBuilder()
                .append("9F350122")
                .append("9F3303E0E8C8")
                .append("9F40056000F0B001")
                .append("9F1A020156")
                .append("5F2A020156")
                .append("9F09020001")
                .append("9C0100")
                .append("9F0206000000000100")
                .append("9A03171020")
                .append("9F2103150512")
                .append("9F410400000001")
                .append("DF918111050000000000")
                .append("DF918112050000000000")
                .append("DF918110050000000000")
                .append("9F6D01C0")
                .append("9F6E04D8E00000")
                .append("DF812406000000010000")
                .append("DF812606000000010000")
                .append("DF812306000000010000")
                .append("DF81300100").toString()
            KernelID.DISCOVER.toByte() -> {
            }
            KernelID.JCB.toByte() -> {
            }
            else -> {
            }
        }

        println(""+emv!!.setTLVList(finalData.kernelID.toInt(),tlvList) +"...onFinalSelect: setTLVList")

        println("...onFinalSelect: respondEvent" + emv!!.respondEvent(null))
    }

    @Throws(RemoteException::class)
    open fun doOnlineProcess(transData: TransData) {
        System.out.println("=> onOnlineProcess | TLVData for online:" + BytesUtil.bytes2HexString(transData.tlvData))

        println("EMV Balance is" + emv!!.balance)
        println("TLV data is" + emv!!.getTLV("9F02"))
        val tagList = arrayOf(
            0x5F2A,
            0x5F34,
            0x82,
            0x84,
            0x95,
            0x9A,
            0x9B,
            0x9C,
            0x9F02,
            0x9F03,
            0x9F06,
            0x9F1A,
            0x9F6E,
            0x9F26,
            0x9F27,
            0x9F33,
            0x9F34,
            0x9F35,
            0x9F36,
            0x9F37,
            0x9F10
        )
        val out = BytesValue()
        val tagOfF55 = SparseArray<String>()
        for (tag in tagList) {
            //  System.out.println("TLV data is "+emv.getTLV(Integer.toHexString(tag)));
            val tlv = emv!!.getTLV(Integer.toHexString(tag!!))
            if (null != tlv && !tlv.isEmpty()) {
                Log.d("EmvHelper -> " + Integer.toHexString(tag!!), tlv)
                //   String length = Integer.toHexString(tlv.size);
                tagOfF55.put(tag!!, tlv)
                if (null != tag && "84" == Integer.toHexString(tag)) {
                    println("Aid value with Tag is ---> " + Integer.toHexString(tag) + tlv)
                    //  cardProcessedDataModal.setAID(Utility.byte2HexStr(tlv))
                }
            } else {
                Log.e("EmvHelper", "getEmvData:" + Integer.toHexString(tag!!) + ", fails")
            }
        }

        cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())

        val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))

        cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

        // val onlineResult: String = doOnlineProcess()
        // val ret = emv!!.respondEvent(onlineResult)
        // println("...onOnlineProcess: respondEvent" + ret)
        tagOfF55.toString()

        val field55: String = getFields55()
        println("Field 55 is $field55")
        cardProcessedDataModal.setField55(field55)
        vfEmvHandlerCallback(cardProcessedDataModal)
    }


    @Throws(RemoteException::class)
    open fun getFields55(): String {
        val tagList = arrayOf(
            0x5F2A,
            0x5F34,
            0x82,
            0x84,
            0x95,
            0x9A,
            0x9B,
            0x9C,
            0x9F02,
            0x9F03,
            0x9F06,
            0x9F1A,
            /*      0x9F6E,*/
            0x9F26,
            0x9F27,
            0x9F33,
            0x9F34,
            0x9F35,
            0x9F36,
            0x9F37,
            0x9F10
        )
        val sb = StringBuilder()
        for (f in tagList) {
            val v1 = emv!!.getTLV(Integer.toHexString(f).toUpperCase(Locale.ROOT))
            val v = BytesUtil.hexString2Bytes(v1)
            if (v != null) {
                sb.append(Integer.toHexString(f))
                var l = Integer.toHexString(v.size)
                if (l.length < 2) {
                    l = "0$l"
                }
                if (f == 0x9F10 /*&& CardAid.AMEX.aid == cardProcessedDataModal.getAID()*/) {
                    val c = l + BytesUtil.bytes2HexString(v)
                    var le = Integer.toHexString(c.length / 2)
                    if (le.length < 2) {
                        le = "0$le"
                    }
                    sb.append(le)
                    sb.append(c)
                } else {
                    sb.append(l)
                    sb.append(BytesUtil.bytes2HexString(v))
                }
            } else if (f == 0x9F03) {
                sb.append(Integer.toHexString(f))
                sb.append("06")
                sb.append("000000000000")
            } else if (f == 0x5f34 /*&& CardAid.Rupay.aid.equals(cardProcessedDataModal.getAID())*/) {
                sb.append(Integer.toHexString(f))
                sb.append("01")
                sb.append("00")
            }
        }
        return sb.toString().toUpperCase(Locale.ROOT)
    }

    open fun doOnlineProcess(): String {
        println("****** doOnlineProcess ******")
        println("... ...")
        println("... ...")
        val onlineSuccess = true
        return if (onlineSuccess) {
            val onlineResult = StringBuffer()
            onlineResult.append(EMVTag.DEF_TAG_ONLINE_STATUS).append("01").append("00")
            val hostRespCode = "3030"
            onlineResult.append(EMVTag.EMV_TAG_TM_ARC).append("02").append(hostRespCode)
            val onlineApproved = true
            onlineResult.append(EMVTag.DEF_TAG_AUTHORIZE_FLAG).append("01")
                .append(if (onlineApproved) "01" else "00")
            val hostTlvData =
                "9F3501229C01009F3303E0F1C89F02060000000000019F03060000000000009F101307010103A0A802010A010000000052856E2C9B9F2701809F260820F63D6E515BD2CC9505008004E8009F1A0201565F2A0201569F360201C982027C009F34034203009F37045D5F084B9A031710249F1E0835303530343230308408A0000003330101019F090200309F410400000001"
            onlineResult.append(
                TLV.fromData(
                    EMVTag.DEF_TAG_HOST_TLVDATA,
                    BytesUtil.hexString2Bytes(hostTlvData)
                ).toString()
            )
            activity.runOnUiThread {
                Toast.makeText(activity, hostTlvData, Toast.LENGTH_LONG).show()
            }
            onlineResult.toString()
        } else {
            println("!!! online failed !!!")
            "DF9181090101"
        }
    }

    open fun doEndProcess(result: Int, transData: TransData?) {
        if (result != EMVError.SUCCESS) {

             if(cardProcessedDataModal.getSuccessResponseCode() == "00"){
                 if(this::testCompleteSecondGenAc.isInitialized){
                     testCompleteSecondGenAc.getEndProcessData(result,transData)
                 }
             }
            else{
                 vfEmvHandlerCallback(cardProcessedDataModal)
             }


            System.out.println("=> onEndProcess | " + EMVInfoUtil.getErrorMessage(result))
        } else {
            System.out.println("=> onEndProcess | EMV_RESULT_NORMAL | " + EMVInfoUtil.getTransDataDesc(transData))

            if (transData != null) {
                getFlowTypeDesc(transData.flowType,result, transData)
            }

        }
        println("\n")
    }

    fun getFlowTypeDesc(flowType: Byte, result: Int, transData: TransData) {
        val desc: String
          when (flowType) {
            FlowType.EMV_FLOWTYPE_EMV.toByte() -> {
                if(this::testCompleteSecondGenAc.isInitialized){
                    testCompleteSecondGenAc.getEndProcessData(result,transData)
                }

            }
            FlowType.EMV_FLOWTYPE_ECASH.toByte() -> "ECASH"
            FlowType.EMV_FLOWTYPE_QPBOC.toByte() -> "QPBOC"
            FlowType.EMV_FLOWTYPE_QVSDC.toByte() -> {

                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)

                val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                var field57 =   "35|"+track2data.replace("D", "=")?.replace("F", "")
                println("Field 57 data is"+field57)
                val encrptedPan = getEncryptedPanorTrackData(field57,true)
                cardProcessedDataModal.setEncryptedPan(encrptedPan)

                val field55: String = getFields55()
                println("Field 55 is $field55")
                cardProcessedDataModal.setField55(field55)

                vfEmvHandlerCallback(cardProcessedDataModal)
            }


            FlowType.EMV_FLOWTYPE_PBOC_CTLESS.toByte() -> "PBOC_CTLESS"
            FlowType.EMV_FLOWTYPE_M_CHIP.toByte() -> "M_CHIP"
            FlowType.EMV_FLOWTYPE_M_STRIPE.toByte() -> "M_STRIPE"
            FlowType.EMV_FLOWTYPE_MSD.toByte() -> "MSD"
            FlowType.EMV_FLOWTYPE_MSD_LEGACY.toByte() -> "MSD_LEGACY"
            FlowType.EMV_FLOWTYPE_WAVE2.toByte() -> "WAVE2"
            FlowType.EMV_FLOWTYPE_A_XP2_MS.toByte() -> {

                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())
                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE)

                val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                val tagDF46 = emv!!.getTLV(Integer.toHexString(0xDF46))
                var tagDf46Str = hexString2String(tagDF46)
                var track2data = tagDf46Str.substring(1,tagDf46Str.length-1)
                var field57 =   "35|"+track2data.replace("D", "=")?.replace("F", "")
                println("Field 57 data is"+field57)
                val encrptedPan = getEncryptedPanorTrackData(field57,true)
                cardProcessedDataModal.setEncryptedPan(encrptedPan)

                vfEmvHandlerCallback(cardProcessedDataModal)

            }
            FlowType.EMV_FLOWTYPE_A_XP2_EMV.toByte() -> {
                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)

                val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                var field57 =   "35|"+track2data.replace("D", "=")?.replace("F", "")
                println("Field 57 data is"+field57)
                val encrptedPan = getEncryptedPanorTrackData(field57,true)
                cardProcessedDataModal.setEncryptedPan(encrptedPan)

                val field55: String = getFields55()
                println("Field 55 is $field55")

                cardProcessedDataModal.setField55(field55)
            }
            else -> "Unkown Type"
        }
        //return desc + String.format("[0x%02X]", flowType)
    }


    open fun doVerifyOfflinePin(flag: Int, random: ByteArray?, capKey: CAPublicKey?, result: OfflinePinVerifyResult) {
        println("=> onVerifyOfflinePin")
        try {
            // * 内置插卡- 0；内置挥卡 – 6；外置设备接USB - 7；外置设备接COM口 -8
            // * inside insert card - 0；inside swing card – 6；External device is connected to the USB port - 7；External device is connected to the COM port -8
            val icToken = 0
            //Specify the type of "PIN check APDU message" that will be sent to the IC card.Currently only support VCF_DEFAULT.
            val cmdFmt = OfflinePinVerify.VCF_DEFAULT
            val offlinePinVerify = OfflinePinVerify(flag.toByte(), icToken, cmdFmt, random)
            val pinVerifyResult = PinVerifyResult()
            val ret: Boolean = pinPad!!.verifyOfflinePin(offlinePinVerify, getPinPublicKey(capKey), pinVerifyResult)
            if (!ret) {
                println("verifyOfflinePin fail: " + pinPad!!.getLastError())
                //stopEMV()
                return
            }
            val apduRet = pinVerifyResult.apduRet
            val sw1 = pinVerifyResult.sW1
            val sw2 = pinVerifyResult.sW2
            result.setSW(sw1.toInt(), sw2.toInt())
            result.result = apduRet.toInt()
        } catch (e: Exception) {
            // handleException(e);
        }
    }

    open fun getPinPublicKey(from: CAPublicKey?): PinPublicKey? {
        if (from == null) {
            return null
        }
        val to = PinPublicKey()
        to.mRid = from.rid
        to.mExp = from.exp
        to.mExpiredDate = from.expDate
        to.mHash = from.hash
        to.mHasHash = from.hashFlag
        to.mIndex = from.index
        to.mMod = from.mod
        return to
    }

    protected open fun respondAID(aid: ByteArray?) {
        try {
            System.out.println("Select aid: " + BytesUtil.bytes2HexString(aid))
            val tmAid = TLV.fromData(EMVTag.EMV_TAG_TM_AID, aid)
            println(""+ emv!!.respondEvent(tmAid.toString())+ "...onAppSelect: respondEvent")
        } catch (e: Exception) {
            //handleException(e);
        }
    }

    @Throws(RemoteException::class)
    open fun doCardHolderVerify(cvm: CVMMethod) {
        System.out.println("=> onCardHolderVerify | " + EMVInfoUtil.getCVMDataDesc(cvm))
        val param = Bundle()
        param.putByteArray(PinpadData.PIN_LIMIT, byteArrayOf(0, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        val listener: OnPinEntryListener = object : OnPinEntryListener.Stub() {
            override fun onInput(arg0: Int, arg1: Int) {}
            override fun onConfirm(data: ByteArray, arg1: Boolean) {
                System.out.println("PinBlock is"+byte2HexStr(data))
                Log.d("PinBlock", "PinPad hex encrypted data ---> " + hexString2String(BytesUtil.bytes2HexString(data)))
                respondCVMResult(1.toByte())

                when (cardProcessedDataModal.getReadCardType()) {
                    DetectCardType.EMV_CARD_TYPE -> {
                        if (cardProcessedDataModal.getIsOnline() == 1) {
                            cardProcessedDataModal.setGeneratePinBlock(BytesUtil.bytes2HexString(data))
                            //insert with pin
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                        } else {
                            cardProcessedDataModal.setGeneratePinBlock("")
                            //off line pin
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_OFFLINE_PIN.posEntry.toString())
                        }
                    }
                    DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                        if (cardProcessedDataModal.getIsOnline() == 1) {
                            cardProcessedDataModal.setGeneratePinBlock(BytesUtil.bytes2HexString(data))
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())
                        } else {
                            cardProcessedDataModal.setGeneratePinBlock("")
                            //  cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                        }
                    }
                    DetectCardType.MAG_CARD_TYPE -> {
                        //   vfIEMV?.importPin(1, data) // in Magnetic pin will not import
                        cardProcessedDataModal.setGeneratePinBlock(BytesUtil.bytes2HexString(data))

                        if (cardProcessedDataModal.getFallbackType() == EFallbackCode.EMV_fallback.fallBackCode)
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_FALL_MAGPIN.posEntry.toString())
                        else
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.POS_ENTRY_SWIPED_NO4DBC_PIN.posEntry.toString())
                        cardProcessedDataModal.setApplicationPanSequenceValue("00")
                    }

                    else -> {
                    }
                }


            }

            override fun onCancel() {
                respondCVMResult(0.toByte())
            }

            override fun onError(error: Int) {
                respondCVMResult(2.toByte())
            }
        }
        when (cvm.cvm) {
            CVMFlag.EMV_CVMFLAG_OFFLINEPIN.toByte() -> pinPad!!.startOfflinePinEntry(param, listener)
            CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> {
                println("=> onCardHolderVerify | onlinpin")
                param.putByteArray(PinpadData.PAN_BLOCK, lastCardRecord!!.pan)
                pinPad!!.startPinEntry(DemoConfig.KEYID_PIN, param, listener)
            }
            else -> {
                println("=> onCardHolderVerify | default")
                respondCVMResult(1.toByte())
            }
        }
    }

    protected open fun respondCVMResult(result: Byte) {
        try {
            val chvStatus = TLV.fromData(EMVTag.DEF_TAG_CHV_STATUS, byteArrayOf(result))
            val ret = emv!!.respondEvent(chvStatus.toString())
            println("...onCardHolderVerify: respondEvent" + ret)
        } catch (e: Exception) {
            //handleException(e);
        }
    }

    @Throws(RemoteException::class)
    open fun doReadRecord(record: CardRecord?) {
        println("=> onReadRecord | " + EMVInfoUtil.getRecordDataDesc(record))
        cardProcessedDataModal.setPanNumberData(EMVInfoUtil.getRecordDataDesc(record))
        System.out.println("Card pannumber data"+EMVInfoUtil.getRecordDataDesc(record))
        //val encrptedPan = getEncryptedPanorTrackData(EMVInfoUtil.getRecordDataDesc(record),false)
        // cardProcessedDataModal.setEncryptedPan(encrptedPan)
        manageCAPKey()



        println("...onReadRecord: respondEvent" + emv!!.respondEvent(null))
    }

    open fun doSendOut(ins: Int, data: ByteArray) {
        when (ins) {
            KernelINS.DISPLAY ->            // DisplayMsg: MsgID（1 byte） + Currency（1 byte）+ DataLen（1 byte） + Data（30 bytes）
                if (data[0] == MessageID.ICC_ACCOUNT.toByte()) {
                    val len: Byte = data[2]
                    val account = BytesUtil.subBytes(data, 1 + 1 + 1, len.toInt())
                    val accTLVList = TLVList.fromBinary(account)
                    val track2 = BytesUtil.bytes2HexString(accTLVList.getTLV("57").bytesValue)
                    var field57 =   "35|"+track2.replace("D", "=")?.replace("F", "")
                    println("Field 57 data is"+field57)
                    val encrptedPan = getEncryptedPanorTrackData(field57,true)
                    cardProcessedDataModal.setEncryptedPan(encrptedPan)
                    println("=> onSendOut | track2 = $field57")
                }
            KernelINS.DBLOG -> {
                var i = data.size - 1
                while (i >= 0) {
                    if (data[i].toInt() == 0x00) {
                        data[i] = 0x20
                    }
                    i--
                }
                Log.d("DBLOG", String(data))
            }
            KernelINS.CLOSE_RF -> {
                println("=> onSendOut: Notify the application to halt contactless module")
                emv!!.halt()
            }
            else -> println(
                "=> onSendOut: instruction is 0x" + Integer.toHexString(ins) + ", data is " + BytesUtil.bytes2HexString(data)
            )
        }
    }

    lateinit var testCompleteSecondGenAc:CompleteSecondGenAc
    fun getCompleteSecondGenAc(testCompleteSecondGenAc: CompleteSecondGenAc, cardProcessedDataModal: CardProcessedDataModal?){
        this.testCompleteSecondGenAc = testCompleteSecondGenAc
        if (cardProcessedDataModal != null) {
            this.cardProcessedDataModal  = cardProcessedDataModal
        }
    }
}






