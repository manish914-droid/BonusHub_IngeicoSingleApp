package com.bonushub.crdb.india.view.baseemv


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.entity.CardOption
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.transactionprocess.CompleteSecondGenAc
import com.bonushub.crdb.india.type.DemoConfigs
import com.bonushub.crdb.india.utils.*
import com.bonushub.crdb.india.utils.ingenico.DialogUtil
import com.bonushub.crdb.india.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.utils.ingenico.TLVList
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.activity.TransactionActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.vxutils.Utility
import com.bonushub.crdb.india.vxutils.Utility.byte2HexStr
import com.bonushub.crdb.india.vxutils.getEncryptedPanorTrackData
import com.usdk.apiservice.aidl.data.BytesValue
import com.usdk.apiservice.aidl.emv.*
import com.usdk.apiservice.aidl.pinpad.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


open class EmvHandler constructor(): EMVEventHandler.Stub() {

    private var defaultScope = CoroutineScope(Dispatchers.Default)
    //for offline Pin
    var pinTryRemainingTimes = 0
    var pinTryCounter = -1
    var maxPin =0;

//result: Int, transData: TransData?
    lateinit var onEndProcessCallback: (Int,CardProcessedDataModal) -> Unit

    private var lastCardRecord: CardRecord? = null

    private var pinPad: UPinpad? = null
    private var emv: UEMV? = null
    private lateinit var activity: TransactionActivity
    private lateinit var cardProcessedDataModal: CardProcessedDataModal
    private lateinit var testCompleteSecondGenAc:CompleteSecondGenAc
    lateinit var vfEmvHandlerCallback: (CardProcessedDataModal) -> Unit
    lateinit var vfFallbackCallback: (CardProcessedDataModal) -> Unit

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
        this.vfFallbackCallback   = vfEmvHandlerCallback
    }

    //1
    @Throws(RemoteException::class)
    override fun onInitEMV() {
        Log.e("VFEmvHandler","onInitEMV")
        doInitEMV()

    }

    @Throws(RemoteException::class)
    override fun onWaitCard(flag: Int) {
    Log.e("VFEmvHandler","onWaitCard  --->   $flag")
       // WaitCardFlag.EXECUTE_CDCVM

        when (flag) {
            WaitCardFlag.NORMAL -> {
                defaultScope.launch {
                    println("Searching card...")
                    try {
                        val cardOption = CardOption.create().apply {
                            supportICCard(true)
                            supportMagCard(true)
                            supportRFCard(true)
                        }
                        DeviceHelper.getEMV()?.searchCard(
                            cardOption.toBundle(),
                            DemoConfigs.TIMEOUT,
                            object : SearchCardListener.Stub() {
                                override fun onCardPass(cardType: Int) {
                                    println("=> onCardPass | cardType = $cardType")
                                    cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                                    // transactionCallback(cardProcessedDataModal)
                                    emv?.respondCard()
                                }

                                override fun onCardInsert() {
                                    println("=> onCardInsert")
                                    cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                                    //  transactionCallback(cardProcessedDataModal)

                                }

                                override fun onCardSwiped(track: Bundle) {

                                }

                                override fun onTimeout() {
                                    println("=> onTimeout")
                                    cardProcessedDataModal.setReadCardType(DetectCardType.TIMEOUT)
                                    // onEndProcess()
                                }

                                override fun onError(code: Int, message: String) {
                                    println("Code: $code")
                                    println("message: $message")
                                    println(String.format("=> onError | %s[0x%02X]", message, code))
                                }
                            })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            WaitCardFlag.EXECUTE_CDCVM -> {
                defaultScope.launch(Dispatchers.Main) {
                    (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                        activity?.getString(R.string.see_Phone),
                        "Execute CDCVM",
                        R.drawable.ic_txn_declined,
                        activity?.getString(R.string.positive_button_ok),
                        "", false, false,
                        { alertPositiveCallback ->
                            if (alertPositiveCallback) {
                                try {
                                    val cardOption = CardOption.create().apply {
                                        supportICCard(true)
                                        supportMagCard(true)
                                        supportRFCard(true)
                                    }
                                    DeviceHelper.getEMV()?.searchCard(
                                        cardOption.toBundle(),
                                        DemoConfigs.TIMEOUT,
                                        object : SearchCardListener.Stub() {
                                            override fun onCardPass(cardType: Int) {
                                                println("=> onCardPass | cardType = $cardType")
                                                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                                                // transactionCallback(cardProcessedDataModal)
                                                emv?.respondCard()
                                            }

                                            override fun onCardInsert() {
                                                println("=> onCardInsert")
                                                cardProcessedDataModal.setReadCardType(DetectCardType.EMV_CARD_TYPE)
                                                //  transactionCallback(cardProcessedDataModal)

                                            }

                                            override fun onCardSwiped(track: Bundle) {

                                            }

                                            override fun onTimeout() {
                                                println("=> onTimeout")
                                                cardProcessedDataModal.setReadCardType(DetectCardType.TIMEOUT)
                                                // onEndProcess()
                                            }

                                            override fun onError(code: Int, message: String) {
                                                println("Code: $code")
                                                println("message: $message")
                                                println(String.format("=> onError | %s[0x%02X]", message, code))
                                            }
                                        })
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        {})

                }


            }
        }


    }

    @Throws(RemoteException::class)
    override fun onCardChecked(cardType: Int) {
        // Only happen when use startProcess()
        Log.e("VFEmvHandler","onCardChecked")
    }

    //2
    @Throws(RemoteException::class)
    override fun onAppSelect(reSelect: Boolean, list: List<CandidateAID>) {
        Log.e("VFEmvHandler","onAppSelect")
        doAppSelect(reSelect, list)
    }

    //3
    @Throws(RemoteException::class)
    override fun onFinalSelect(finalData: FinalData?) {
        Log.e("VFEmvHandler","onFinalSelect")
        if (finalData != null) {
          createAndSetCDOL1ForFirstGenAC(finalData,cardProcessedDataModal,emv)
         //   doFinalSelect(finalData)
        }
    }

    //5
    @Throws(RemoteException::class)
    override fun onReadRecord(cardRecord: CardRecord) {
        Log.e("VFEmvHandler","onReadRecord")
        lastCardRecord = cardRecord
        doReadRecord(cardRecord)
    }
// 6
    @Throws(RemoteException::class)
    override fun onCardHolderVerify(cvmMethod: CVMMethod?) {
        Log.e("VFEmvHandler","onCardHolderVerify")
        if (cvmMethod != null) {
            doCardHolderVerify(cvmMethod)
        }
    }

    @Throws(RemoteException::class)
    override fun onOnlineProcess(transData: TransData?) {
        Log.e("VFEmvHandler","onOnlineProcess")
        if (transData != null) {
            doOnlineProcess(transData)
        }

    }

    @Throws(RemoteException::class)
    override fun onEndProcess(result: Int, transData: TransData?) {
        Log.e("VFEmvHandler","onEndProcess")
        doEndProcess(result, transData)
    }

    @Throws(RemoteException::class)
    override fun onVerifyOfflinePin(flag: Int, random: ByteArray?, caPublicKey: CAPublicKey?, offlinePinVerifyResult: OfflinePinVerifyResult?) {
        Log.e("VFEmvHandler","onVerifyOfflinePin")
        if (offlinePinVerifyResult != null) {
            doVerifyOfflinePin(flag, random, caPublicKey, offlinePinVerifyResult)
        }
    }

    @Throws(RemoteException::class)
    override fun onObtainData(ins: Int, data: ByteArray?) {
        Log.e("VFEmvHandler","onObtainData")
        //	outputText("=> onObtainData: instruction is 0x" + Integer.toHexString(ins) + ", data is " + BytesUtil.bytes2HexString(data));
    }

    //4
    @Throws(RemoteException::class)
    override fun onSendOut(ins: Int, data: ByteArray?) {
        Log.e("VFEmvHandler","onSendOut")
        doSendOut(ins, data!!)

    }


    // 1 calling
    @Throws(RemoteException::class)
    fun doInitEMV() {
        println("=> onInitEMV ")
        settingAids(emv)
        settingCAPkeys(emv)
    }

    @Throws(RemoteException::class)
    protected fun manageAID() {
        println("****** manage AID ******")
        emv!!.manageAID(ActionFlag.CLEAR, null,false)
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
            "A0000000031010",
            // rupay
            "A0000005241010",
            // dinners
            "A0000001524010",
            "A0000001523010"
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
   "9F0605A000000025" +
           "9F2201C8" +
           "DF0503201231DF060101DF070101DF028190BF0CFCED708FB6B048E3014336EA24AA007D7967B8AA4E613D26D015C4FE7805D9DB131CED0D2A8ED504C3B5CCD48C33199E5A5BF644DA043B54DBF60276F05B1750FAB39098C7511D04BABC649482DDCF7CC42C8C435BAB8DD0EB1A620C31111D1AAAF9AF6571EEBD4CF5A08496D57E7ABDBB5180E0A42DA869AB95FB620EFF2641C3702AF3BE0B0C138EAEF202E21DDF040103DF031433BD7A059FAB094939B90A8F35845C9DC779BD50", //c8
"9F0605A000000025" +
        "9F2201C9" +
        "DF0503201231DF060101DF070101DF0281B0B362DB5733C15B8797B8ECEE55CB1A371F760E0BEDD3715BB270424FD4EA26062C38C3F4AAA3732A83D36EA8E9602F6683EECC6BAFF63DD2D49014BDE4D6D603CD744206B05B4BAD0C64C63AB3976B5C8CAAF8539549F5921C0B700D5B0F83C4E7E946068BAAAB5463544DB18C63801118F2182EFCC8A1E85E53C2A7AE839A5C6A3CABE73762B70D170AB64AFC6CA482944902611FB0061E09A67ACB77E493D998A0CCF93D81A4F6C0DC6B7DF22E62DBDF040103DF03148E8DFF443D78CD91DE88821D70C98F0638E51E49", //c9
"9F0605A000000025" +
        "9F2201CA" +
        "DF0503201231DF060101DF070101DF0281F8C23ECBD7119F479C2EE546C123A585D697A7D10B55C2D28BEF0D299C01DC65420A03FE5227ECDECB8025FBC86EEBC1935298C1753AB849936749719591758C315FA150400789BB14FADD6EAE2AD617DA38163199D1BAD5D3F8F6A7A20AEF420ADFE2404D30B219359C6A4952565CCCA6F11EC5BE564B49B0EA5BF5B3DC8C5C6401208D0029C3957A8C5922CBDE39D3A564C6DEBB6BD2AEF91FC27BB3D3892BEB9646DCE2E1EF8581EFFA712158AAEC541C0BBB4B3E279D7DA54E45A0ACC3570E712C9F7CDF985CFAFD382AE13A3B214A9E8E1E71AB1EA707895112ABC3A97D0FCB0AE2EE5C85492B6CFD54885CDD6337E895CC70FB3255E3DF040103DF03146BDA32B1AA171444C7E8F88075A74FBFE845765F", //CA,

            //region =========================================================================== Visa Test cap keys Starts==================================================================================================================================================================================================================================
                    //Visa Test cap Keys 92
                    "9F0605A000000003" +
                            "9F220192" +
                            "DF050420291231" +
                            "DF0281B0996AF56F569187D09293C14810450ED8EE3357397B18A2458EFAA92DA3B6DF6514EC060195318FD43BE9B8F0CC669E3F844057CBDDF8BDA191BB64473BC8DC9A730DB8F6B4EDE3924186FFD9B8C7735789C23A36BA0B8AF65372EB57EA5D89E7D14E9C7B6B557460F10885DA16AC923F15AF3758F0F03EBD3C5C2C949CBA306DB44E6A2C076C5F67E281D7EF56785DC4D75945E491F01918800A9E2DC66F60080566CE0DAF8D17EAD46AD8E30A247C9F" + //Module
                            "DF040103" +
                            "DF0314429C954A3859CEF91295F663C963E582ED6EB253" + //checsum
                            "BF010131" +
                            "DF070101", //ARITH ID
                    //Visa Test cap Keys 94
                    "9F0605A000000003" +
                            "9F220194" +
                            "DF050420291231" +
                            "DF0281F8ACD2B12302EE644F3F835ABD1FC7A6F62CCE48FFEC622AA8EF062BEF6FB8BA8BC68BBF6AB5870EED579BC3973E121303D34841A796D6DCBC41DBF9E52C4609795C0CCF7EE86FA1D5CB041071ED2C51D2202F63F1156C58A92D38BC60BDF424E1776E2BC9648078A03B36FB554375FC53D57C73F5160EA59F3AFC5398EC7B67758D65C9BFF7828B6B82D4BE124A416AB7301914311EA462C19F771F31B3B57336000DFF732D3B83DE07052D730354D297BEC72871DCCF0E193F171ABA27EE464C6A97690943D59BDABB2A27EB71CEEBDAFA1176046478FD62FEC452D5CA393296530AA3F41927ADFE434A2DF2AE3054F8840657A26E0FC617" +
                            "DF040103" + //exponent
                            "DF0314C4A3C43CCF87327D136B804160E47D43B60E6E0F" +
                            "BF010131" +
                            "DF070101",
                    //Visa Test cap Keys 95
                    "9F0605A000000003" +
                            "9F220195" +
                            "DF050420291231" +
                            "DF028190BE9E1FA5E9A803852999C4AB432DB28600DCD9DAB76DFAAA47355A0FE37B1508AC6BF38860D3C6C2E5B12A3CAAF2A7005A7241EBAA7771112C74CF9A0634652FBCA0E5980C54A64761EA101A114E0F0B5572ADD57D010B7C9C887E104CA4EE1272DA66D997B9A90B5A6D624AB6C57E73C8F919000EB5F684898EF8C3DBEFB330C62660BED88EA78E909AFF05F6DA627BDF040103" +
                            "DF040103" + //exponent
                            "DF0314EE1511CEC71020A9B90443B37B1D5F6E703030F6" +
                            "BF010131",
      //endregion =========================================================================== Visa Test cap keys ends==================================================================================================================================================================================================================================
//region =========================================================================== Visa Live cap keys start==================================================================================================================================================================================================================================
            //Visa Live cap Keys 08
            "9F0605A000000003" + //AID
                    "9F220108" + //Index
                    "DF050420241231" + //Expiry Date
                    "DF0281B0"+"D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0B"+ //module
                    "DF040103" + //exponent
                    "DF0314"+"20D213126955DE205ADC2FD2822BD22DE21CF9A8", //exponent

            //Visa Live cap Keys 09
            "9F0605A000000003" + //AID
                    "9F220109" + //Index
                    "DF050420241231" + //Expiry Date
                    "DF0281F8"+"9D912248DE0A4E39C1A7DDE3F6D2588992C1A4095AFBD1824D1BA74847F2BC4926D2EFD904B4B54954CD189A54C5D1179654F8F9B0D2AB5F0357EB642FEDA95D3912C6576945FAB897E7062CAA44A4AA06B8FE6E3DBA18AF6AE3738E30429EE9BE03427C9D64F695FA8CAB4BFE376853EA34AD1D76BFCAD15908C077FFE6DC5521ECEF5D278A96E26F57359FFAEDA19434B937F1AD999DC5C41EB11935B44C18100E857F431A4A5A6BB65114F174C2D7B59FDF237D6BB1DD0916E644D709DED56481477C75D95CDD68254615F7740EC07F330AC5D67BCD75BF23D28A140826C026DBDE971A37CD3EF9B8DF644AC385010501EFC6509D7A41"+ //module
                    "DF040103" + //exponent
                    "DF0314"+"1FF80A40173F52D7D27E0F26A146A1C8CCB29046", //exponent

//endregion =========================================================================== Visa Live cap keys ends==================================================================================================================================================================================================================================


/*
//region =========================================================================== Master Testcap keys starts==================================================================================================================================================================================================================================
                // MasterCard
                // MasterCard
                "9F0605A000000004" +
                        "9F2201EF" + //Test
                        "DF050420291231" +
                        "DF0281F8A191CB87473F29349B5D60A88B3EAEE0973AA6F1A082F358D849FDDFF9C091F899EDA9792CAF09EF28F5D22404B88A2293EEBBC1949C43BEA4D60CFD879A1539544E09E0F09F60F065B2BF2A13ECC705F3D468B9D33AE77AD9D3F19CA40F23DCF5EB7C04DC8F69EBA565B1EBCB4686CD274785530FF6F6E9EE43AA43FDB02CE00DAEC15C7B8FD6A9B394BABA419D3F6DC85E16569BE8E76989688EFEA2DF22FF7D35C043338DEAA982A02B866DE5328519EBBCD6F03CDD686673847F84DB651AB86C28CF1462562C577B853564A290C8556D818531268D25CC98A4CC6A0BDFFFDA2DCCA3A94C998559E307FDDF915006D9A987B07DDAEB3B" + //checksum
                        "DF040103" + // exponent
                        "DF031421766EBB0EE122AFB65D7845B73DB46BAB65427A" + //Module
                        "BF010131" +
                        "DF070101", //ARITH ID



                "9F0605A000000004" +
                        "9F220104" + //Live
                        "DF050420291231DF028190A6DA428387A502D7DDFB7A74D3F412BE762627197B25435B7A81716A700157DDD06F7CC99D6CA28C2470527E2C03616B9C59217357C2674F583B3BA5C7DCF2838692D023E3562420B4615C439CA97C44DC9A249CFCE7B3BFB22F68228C3AF13329AA4A613CF8DD853502373D62E49AB256D2BC17120E54AEDCED6D96A4287ACC5C04677D4A5A320DB8BEE2F775E5FEC5DF040103DF0314381A035DA58B482EE2AF75F4C3F2CA469BA4AA6CBF010131DF070101",
                "9F0605A000000004" +
                        "9F220105" +//Test
                        "DF050420291231DF0281B0B8048ABC30C90D976336543E3FD7091C8FE4800DF820ED55E7E94813ED00555B573FECA3D84AF6131A651D66CFF4284FB13B635EDD0EE40176D8BF04B7FD1C7BACF9AC7327DFAA8AA72D10DB3B8E70B2DDD811CB4196525EA386ACC33C0D9D4575916469C4E4F53E8E1C912CC618CB22DDE7C3568E90022E6BBA770202E4522A2DD623D180E215BD1D1507FE3DC90CA310D27B3EFCCD8F83DE3052CAD1E48938C68D095AAC91B5F37E28BB49EC7ED597DF040103DF0314EBFA0D5D06D8CE702DA3EAE890701D45E274C845BF010131DF070101",
                "9F0605A000000004" +
                        "9F220106" +//Live
                        "DF050420291231DF0281F8CB26FC830B43785B2BCE37C81ED334622F9622F4C89AAE641046B2353433883F307FB7C974162DA72F7A4EC75D9D657336865B8D3023D3D645667625C9A07A6B7A137CF0C64198AE38FC238006FB2603F41F4F3BB9DA1347270F2F5D8C606E420958C5F7D50A71DE30142F70DE468889B5E3A08695B938A50FC980393A9CBCE44AD2D64F630BB33AD3F5F5FD495D31F37818C1D94071342E07F1BEC2194F6035BA5DED3936500EB82DFDA6E8AFB655B1EF3D0D7EBF86B66DD9F29F6B1D324FE8B26CE38AB2013DD13F611E7A594D675C4432350EA244CC34F3873CBA06592987A1D7E852ADC22EF5A2EE28132031E48F74037E3B34AB747FDF040103DF0314F910A1504D5FFB793D94F3B500765E1ABCAD72D9BF010131DF070101",

                "9F0605A000000004" +
                        "9F2201F1" + //Test
                        "DF050420231231" +
                        "DF0281B0A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C5A834D73AAE27C99B0B053A90278007239B6459FF0BBCD7B4B9C6C50AC02CE91368DA1BD21AAEADBC65347337D89B68F5C99A09D05BE02DD1F8C5BA20E2F13FB2A27C41D3F85CAD5CF6668E75851EC66EDBF98851FD4E42C44C1D59F5984703B27D5B9F21B8FA0D93279FBBF69E090642909C9EA27F898959541AA6757F5F624104F6E1D3A9532F2A6E51515AEAD1B43B3D7835088A2FAFA7BE7" + //Checksum
                        "DF040103" +
                        "DF0314D8E68DA167AB5A85D8C3D55ECB9B0517A1A5B4BB" + //Module
                        "BF010131" +
                        "DF070101",

                "9F0605A000000004" +
                        "9F220103" + //Live
                        "DF050420291231" +
                        "DF028180" +
                        "C2490747FE17EB0584C88D47B1602704150ADC88C5B998BD59CE043EDEBF0FFEE3093AC7956AD3B6AD4554C6DE19A178D6DA295BE15D5220645E3C8131666FA4BE5B84FE131EA44B039307638B9E74A8C42564F892A64DF1CB15712B736E3374F1BBB6819371602D8970E97B900793C7C2A89A4A1649A59BE680574DD0B60145" +
                        "DF040103DF03145ADDF21D09278661141179CBEFF272EA384B13BBBF010131DF070101",
                "9F0605A000000004" +
                        "9F220109" + //
                        "DF050420291231DF028180C132F436477A59302E885646102D913EC86A95DD5D0A56F625F472B67F52179BC8BD258A7CD43EF1720AC0065519E3FFCECC26F978EDF9FB8C6ECDF145FDCC697D6B72562FA2E0418B2B80A038D0DC3B769EB027484087CCE6652488D2B3816742AC9C2355B17411C47EACDD7467566B302F512806E331FAD964BF000169F641" +
                        "DF040103DF0300BF010131DF070101",
 //endregion =========================================================================== Master Test cap keys ends==================================================================================================================================================================================================================================*/


            //region =========================================================================== Master Live cap keys starts==================================================================================================================================================================================================================================
            //Master Live cap Keys 05
            "9F0605A000000004" + //AID
                    "9F220105" + //Index
                    "DF050420241231" + //Expiry Date
                    "DF0281B0"+"B8048ABC30C90D976336543E3FD7091C8FE4800DF820ED55E7E94813ED00555B573FECA3D84AF6131A651D66CFF4284FB13B635EDD0EE40176D8BF04B7FD1C7BACF9AC7327DFAA8AA72D10DB3B8E70B2DDD811CB4196525EA386ACC33C0D9D4575916469C4E4F53E8E1C912CC618CB22DDE7C3568E90022E6BBA770202E4522A2DD623D180E215BD1D1507FE3DC90CA310D27B3EFCCD8F83DE3052CAD1E48938C68D095AAC91B5F37E28BB49EC7ED597"+ //module
                    "DF040103" + //exponent
                    "DF0314"+"EBFA0D5D06D8CE702DA3EAE890701D45E274C845", //exponent

            //Master Live cap Keys 06
            "9F0605A000000004" + //AID
                    "9F220106" + //Index
                    "DF050420241231" + //Expiry Date
                    "DF0281F8"+"CB26FC830B43785B2BCE37C81ED334622F9622F4C89AAE641046B2353433883F307FB7C974162DA72F7A4EC75D9D657336865B8D3023D3D645667625C9A07A6B7A137CF0C64198AE38FC238006FB2603F41F4F3BB9DA1347270F2F5D8C606E420958C5F7D50A71DE30142F70DE468889B5E3A08695B938A50FC980393A9CBCE44AD2D64F630BB33AD3F5F5FD495D31F37818C1D94071342E07F1BEC2194F6035BA5DED3936500EB82DFDA6E8AFB655B1EF3D0D7EBF86B66DD9F29F6B1D324FE8B26CE38AB2013DD13F611E7A594D675C4432350EA244CC34F3873CBA06592987A1D7E852ADC22EF5A2EE28132031E48F74037E3B34AB747F"+ //module
                    "DF040103" + //exponent
                    "DF0314"+"F910A1504D5FFB793D94F3B500765E1ABCAD72D9", //exponent
//endregion =========================================================================== Master Live cap keys ends==================================================================================================================================================================================================================================


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

    // 2 calling
    open fun doAppSelect(reSelect: Boolean, candList: List<CandidateAID>) {
        println("=> onAppSelect: cand AID size = " + candList.size)
        if (candList.size > 1) {
            selectApp(candList, object : DialogUtil.OnSelectListener {
                override fun onCancel() {
                    try {
                        emv!!.stopEMV()
                        val intent = Intent(activity, NavigationActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        activity.startActivity(intent)

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

    private fun selectApp(candList: List<CandidateAID>, listener: DialogUtil.OnSelectListener?) {
        activity.runOnUiThread {
            DialogUtil.showSelectDialog(
                activity,
                "Please select app",
                candList,
                0,
                listener
            )
        }
    }

    // 3 calling
    @SuppressLint("NewApi")
    @Throws(RemoteException::class)
    open fun doFinalSelect(finalData: FinalData) {
        println("=> onFinalSelect | " + EMVInfoUtil.getFinalSelectDesc(finalData))

        val datetime: String = DeviceHelper.getCurentDateTime()
        val splitStr = datetime.split("\\s+".toRegex()).toTypedArray()
        val txnAmount = addPad(cardProcessedDataModal.getTransactionAmount().toString(), "0", 12, true)

        var aidstr = BytesUtil.bytes2HexString(finalData.aid).subSequence(0, 10).toString()

        var tlvList: String? = null
        when (finalData.kernelID) {
            // for EMV this call is common for every payment scheme
            KernelID.EMV.toByte() -> {
            //    if (aidstr == "A000000025" || aidstr == "A000000003") {
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
              //  }
            }

            KernelID.AMEX.toByte() -> {
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

            }
            KernelID.PBOC.toByte() -> {              // if suport PBOC Ecash，see transaction parameters of PBOC Ecash in《UEMV develop guide》.
                // If support qPBOC, see transaction parameters of QuickPass in《UEMV develop guide》.
                // For reference only below
                tlvList =
                    "9F02060000000001009F03060000000000009A031710209F21031505129F4104000000019F660427004080"
            }
            KernelID.VISA.toByte() -> {               // Parameter settings, see transaction parameters of PAYWAVE in《UEMV develop guide》.
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

            }
            KernelID.MASTER.toByte() -> {            // Parameter settings, see transaction parameters of PAYPASS in《UEMV develop guide》.
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
            }

            KernelID.DISCOVER.toByte() -> {}
            KernelID.JCB.toByte() -> {}
            else -> {}
        }

        println(""+emv!!.setTLVList(finalData.kernelID.toInt(),tlvList) +"...onFinalSelect: setTLVList")

        println("...onFinalSelect: respondEvent" + emv!!.respondEvent(null))
    }

    @Throws(RemoteException::class)
    open fun doOnlineProcess(transData: TransData) {
        System.out.println("=> onOnlineProcess | TLVData for online:" + BytesUtil.bytes2HexString(transData.tlvData))

        println("EMV Balance is" + emv!!.balance)
        println("TLV data is" + emv!!.getTLV("9F02"))
    /*    val tagList = arrayOf(
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
            0x9F10,
            0x8F01
        )
        val out = BytesValue()
        val tagOfF55 = SparseArray<String>()
        for (tag in tagList) {
            //  System.out.println("TLV data is "+emv.getTLV(Integer.toHexString(tag)));
            val tlv = emv!!.getTLV(Integer.toHexString(tag))
            if (null != tlv && !tlv.isEmpty()) {
                Log.d("EmvHelper -> " + Integer.toHexString(tag), tlv)
                //   String length = Integer.toHexString(tlv.size);
                tagOfF55.put(tag, tlv)
                if ("84" == Integer.toHexString(tag)) {
                    println("Aid value with Tag is ---> " + Integer.toHexString(tag) + tlv)
                    //  cardProcessedDataModal.setAID(Utility.byte2HexStr(tlv))
                }
            } else {
                Log.e("EmvHelper", "getEmvData:" + Integer.toHexString(tag) + ", fails")
            }
        }
*/
       cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())
        when (cardProcessedDataModal.getReadCardType()) {
            DetectCardType.EMV_CARD_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())
                }
            }
            DetectCardType.CONTACT_LESS_CARD_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                }
            }
            DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE -> {
                when (cardProcessedDataModal.getIsOnline()) {
                    0 -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())
                }
            }

            DetectCardType.MAG_CARD_TYPE -> cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())

            else -> {
            }
        }

        //   cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_NO_PIN.posEntry.toString())

        val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))

        cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

        // val onlineResult: String = doOnlineProcess()
        // val ret = emv!!.respondEvent(onlineResult)
        // println("...onOnlineProcess: respondEvent" + ret)
      //  tagOfF55.toString()

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
        for (tag in tagList) {
       //   println(  "9F34 --->  "+    emv!!.getTLV(Integer.toHexString(f).toUpperCase(Locale.ROOT)))
if(emv!!.getTLV(Integer.toHexString(tag).toUpperCase(Locale.ROOT)).isEmpty())
continue

            val v1 = emv!!.getTLV(Integer.toHexString(tag).toUpperCase(Locale.ROOT))
            val v = BytesUtil.hexString2Bytes(v1)
            if (v != null) {
                sb.append(Integer.toHexString(tag))
                var l = Integer.toHexString(v.size)
                if (l.length < 2) {
                    l = "0$l"
                }

                if (false){//f == 0x9F10 /*&& CardAid.AMEX.aid == cardProcessedDataModal.getAID()*/) {
                   /* val c = l + BytesUtil.bytes2HexString(v)
                    var le = Integer.toHexString(c.length / 2)
                    if (le.length < 2) {
                        le = "0$le"
                    }

                    sb.append(le)
                    sb.append(c)*/

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
            } else if (tag == 0x9F03) {
                sb.append(Integer.toHexString(tag))
                sb.append("06")
                sb.append("000000000000")
            } else if (tag == 0x5f34 /*&& CardAid.Rupay.aid.equals(cardProcessedDataModal.getAID())*/) {
                sb.append(Integer.toHexString(tag))
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
            println("=> onEndProcess | " + EMVInfoUtil.getErrorMessage(result))
            if(cardProcessedDataModal.getSuccessResponseCode() == "00"){
                if(this::testCompleteSecondGenAc.isInitialized){
                    testCompleteSecondGenAc.getEndProcessData(result,transData)
                }else{
                    logger("testCompleteSecondGenAc","uninitialized","e")
                }
            }
            else{
                onEndProcessCallback(result,cardProcessedDataModal)
            }

        }
        else {
            println("=> onEndProcess | EMV_RESULT_NORMAL | " + EMVInfoUtil.getTransDataDesc(transData))
            println(transData?.cvm?.let { EMVInfoUtil.getCVMDesc(it) })
            if (transData != null) {
                when(transData.acType.toInt()){
                    // Transaction declined offline
                    ACType.EMV_ACTION_AAC->{
                        println("ACType.EMV_ACTION_AAC")
                        emv?.stopProcess()
                        activity.txnDeclinedDialog()
                    }
                    // Transaction go online form authorization
                    ACType.EMV_ACTION_ARQC->{
                        println("ACType.EMV_ACTION_ARQC")
                        doFlowTypeAction(transData.flowType,result, transData)
                    }
                    // Transaction approved by the terminal itself
                    ACType.EMV_ACTION_TC->{
                        println("ACType.EMV_ACTION_TC")
                        if(this::testCompleteSecondGenAc.isInitialized){
                            doFlowTypeAction(transData.flowType,result, transData)
                        }else{
                            emv?.stopProcess()
                            activity.txnDeclinedDialog()
                            println("testCompleteSecondGenAc is null , EMV declined --> Terminal declined the txn")
                        }


                    }
                }
            }
        }
    }

    open fun getACTypeDesc(acType: Byte): String? {
        val desc: String
        desc = when (acType) {
            ACType.EMV_ACTION_AAC.toByte() -> "AAC"
            ACType.EMV_ACTION_ARQC.toByte() -> "ARQC"
            ACType.EMV_ACTION_TC.toByte() -> "TC"
            else -> "Unkown type"
        }
        return desc + String.format("[0x%02X]", acType)
    }

    open fun getCVMDesc(cvm: Byte): String? {
        val desc: String
        desc = when (cvm) {
            CVMFlag.EMV_CVMFLAG_NOCVM.toByte() -> "No CVM verification required"
            CVMFlag.EMV_CVMFLAG_OFFLINEPIN.toByte() -> "Offline PIN"
            CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> "Online PIN"
            CVMFlag.EMV_CVMFLAG_SIGNATURE.toByte() -> "Signature"
            CVMFlag.EMV_CVMFLAG_OLPIN_SIGN.toByte() -> "Online PIN plus signature"
            CVMFlag.EMV_CVMFLAG_CDV.toByte() -> "Consumer Device Verification(qVSDC/qPBOC)"
            CVMFlag.EMV_CVMFLAG_CCV.toByte() -> "Confirmation Code Verified(PayPass)"
            CVMFlag.EMV_CVMFLAG_CERTIFICATE.toByte() -> "Certificate verification"
            CVMFlag.EMV_CVMFLAG_ECASHPIN.toByte() -> "Electronic cash recharge PIN"
            else -> "Unknown type"
        }
        return desc + String.format("[0x%02X]", cvm)
    }


    private fun doFlowTypeAction(flowType: Byte, result: Int, transData: TransData) {
        val desc: String
       Log.e("getFlow",transData.flowType.toString())
        when (flowType) {
            FlowType.EMV_FLOWTYPE_EMV.toByte() -> {
                if(this::testCompleteSecondGenAc.isInitialized){
                    testCompleteSecondGenAc.getEndProcessData(result,transData)
                }else{
                    println("doFlowTypeAction  ... testCompleteSecondGenAc is null , EMV declined --> Terminal declined ")
                }

            }
            FlowType.EMV_FLOWTYPE_ECASH.toByte() -> println("ECASH")
            FlowType.EMV_FLOWTYPE_QPBOC.toByte() ->println( "QPBOC")
           /* FlowType.EMV_FLOWTYPE_QVSDC.toByte() -> {

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
            }*/
            FlowType.EMV_FLOWTYPE_PBOC_CTLESS.toByte() -> println("PBOC_CTLESS")
            FlowType.EMV_FLOWTYPE_M_STRIPE.toByte() -> println("M_STRIPE")
            FlowType.EMV_FLOWTYPE_MSD.toByte() ->println( "MSD")
            FlowType.EMV_FLOWTYPE_MSD_LEGACY.toByte() -> println("MSD_LEGACY")
            FlowType.EMV_FLOWTYPE_WAVE2.toByte() -> println("WAVE2")
            FlowType.EMV_FLOWTYPE_A_XP2_MS.toByte() -> {

                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_MSD_POS_ENTRY_CODE.posEntry.toString())
                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_WITH_MAG_TYPE)

                val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                val tagDF46 = emv!!.getTLV(Integer.toHexString(0xDF46))
                val tagDf46Str = hexString2String(tagDF46)
                val track2data = tagDf46Str.substring(1,tagDf46Str.length-1)
                val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                println("Field 57 data is"+field57)
                val encrptedPan = getEncryptedPanorTrackData(field57,true)
                cardProcessedDataModal.setEncryptedPan(encrptedPan)

                vfEmvHandlerCallback(cardProcessedDataModal)

            }
            FlowType.EMV_FLOWTYPE_A_XP2_EMV.toByte(),FlowType.EMV_FLOWTYPE_QVSDC.toByte()  -> {

                when (transData.cvm) {
                    CVMFlag.EMV_CVMFLAG_NOCVM.toByte() -> {
                        cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                        cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)

                        val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                        cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                        val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                        val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                        println("Field 57 data is"+field57)
                        val encrptedPan = getEncryptedPanorTrackData(field57,true)
                        cardProcessedDataModal.setEncryptedPan(encrptedPan)

                        val field55: String = getFields55()
                        println("Field 55 is $field55")

                        cardProcessedDataModal.setField55(field55)
                        vfEmvHandlerCallback(cardProcessedDataModal)

                    }
                    CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> {

                        val param = Bundle()
                        //optional Pin Block format by default its 0
                        param.putByte(PinpadData.PIN_BLOCK_FORMAT,0)
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
                                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
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
                                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
                                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())

                                            val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                                            cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                                            val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                                            val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                                            println("Field 57 data is"+field57)
                                            val encrptedPan = getEncryptedPanorTrackData(field57,true)
                                            cardProcessedDataModal.setEncryptedPan(encrptedPan)

                                            val field55: String = getFields55()
                                            println("Field 55 is $field55")

                                            cardProcessedDataModal.setField55(field55)
                                            vfEmvHandlerCallback(cardProcessedDataModal)

                                        } else {
                                            cardProcessedDataModal.setGeneratePinBlock("")
                                            //  cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                                        }
                                    }
                                    DetectCardType.MAG_CARD_TYPE -> {
                                        //   vfIEMV?.importPin(1, data) // in Magnetic pin will not import
                                        cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))

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
                                //  respondCVMResult(0.toByte())
                                Log.d("Data", "PinPad onCancel")
                                try {
                                    //  GlobalScope.launch(Dispatchers.Main) {
                                    activity.declinedTransaction()
                                    //    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }

                            override fun onError(error: Int) {
                                respondCVMResult(2.toByte())
                                Log.d("Data", "PinPad onError, code:$error")
                                try {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        (activity as TransactionActivity).declinedTransaction()
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                        }

                        println("=> onCardHolderVerify | onlinpin")
                        cardProcessedDataModal.setIsOnline(1)


                       // byte2HexStr(lastCardRecord!!.pan)
                    //    println("Pan block is "+byte2HexStr(lastCardRecord!!.pan))
                     //   addPad("374245001751006", "0", 16, true)

                        println("CardPanNumber data is "+cardProcessedDataModal.getPanNumberData() ?: "")

                        Utility.hexStr2Byte(addPad("374245001751006", "0", 16, true))
                        param.putByteArray(PinpadData.PAN_BLOCK, Utility.hexStr2Byte(addPad(cardProcessedDataModal.getPanNumberData() ?: "", "0", 16, true)))
                        pinPad!!.startPinEntry(DemoConfig.KEYID_PIN, param, listener)


                    }

                    else -> "Unknown type"
                }

                //    cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                //   cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)



            }
            FlowType.EMV_FLOWTYPE_M_CHIP.toByte()->{
                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_ENTRY_CODE.posEntry.toString())
                cardProcessedDataModal.setReadCardType(DetectCardType.CONTACT_LESS_CARD_TYPE)
                val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)
                val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                println("Field 57 data is"+field57)
                val encrptedPan = getEncryptedPanorTrackData(field57,true)
                cardProcessedDataModal.setEncryptedPan(encrptedPan)

                val field55: String = getFields55()
                println("Field 55 is $field55")

                cardProcessedDataModal.setField55(field55)
                vfEmvHandlerCallback(cardProcessedDataModal)
            }
            else -> {
                println("Unkown Flow Type")
                //  cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
                cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())

                val applicationsquence = emv!!.getTLV(Integer.toHexString(0x5F34))
                cardProcessedDataModal.setApplicationPanSequenceValue(applicationsquence)

                /*   val tagDF46 = emv!!.getTLV(Integer.toHexString(0xDF46))
                   val tagDf46Str = hexString2String(tagDF46)
                   val track2data = tagDf46Str.substring(1,tagDf46Str.length-1)
                   println("Field D46 data is"+track2data)*/

                val track2data = emv!!.getTLV(Integer.toHexString(0x57))
                val field57 =   "35|"+ track2data.replace("D", "=").replace("F", "")
                println("Field 57 data is"+field57)
                val encrptedPan = getEncryptedPanorTrackData(field57,true)
                cardProcessedDataModal.setEncryptedPan(encrptedPan)

                val field55: String = getFields55()
                println("Field 55 is $field55")

                cardProcessedDataModal.setField55(field55)
                vfEmvHandlerCallback(cardProcessedDataModal)
            }
        }
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
            e.printStackTrace()
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
            println("Select aid: " + BytesUtil.bytes2HexString(aid))
            val tmAid = TLV.fromData(EMVTag.EMV_TAG_TM_AID, aid)
            println(""+ emv!!.respondEvent(tmAid.toString())+ "...onAppSelect: respondEvent")
        } catch (e: Exception) {
            //handleException(e);
        }
    }

    // CVM rule
    @Throws(RemoteException::class)
    open fun doCardHolderVerify(cvm: CVMMethod) {
        System.out.println("=> onCardHolderVerify | " + EMVInfoUtil.getCVMDataDesc(cvm))
        val param = Bundle()
        //optional Pin Block format by default its 0
        param.putByte(PinpadData.PIN_BLOCK_FORMAT,0)
        param.putByteArray(PinpadData.PIN_LIMIT, byteArrayOf( 4, 5, 6, 7, 8, 9, 10, 11, 12))

        val listener: OnPinEntryListener = object : OnPinEntryListener.Stub() {
            override fun onInput(arg0: Int, arg1: Int) {}
            override fun onConfirm(data: ByteArray, arg1: Boolean) {
                System.out.println("PinBlock is"+byte2HexStr(data))
                Log.d("PinBlock", "PinPad hex encrypted data ---> " + hexString2String(BytesUtil.bytes2HexString(data)))
                respondCVMResult(1.toByte())

                when (cardProcessedDataModal.getReadCardType()) {
                    DetectCardType.EMV_CARD_TYPE -> {
                        if (cardProcessedDataModal.getIsOnline() == 1) {
                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
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
                            cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))
                            cardProcessedDataModal.setPosEntryMode(PosEntryModeType.CTLS_EMV_POS_WITH_PIN.posEntry.toString())
                        } else {
                            cardProcessedDataModal.setGeneratePinBlock("")
                            //  cardProcessedDataModal.setPosEntryMode(PosEntryModeType.EMV_POS_ENTRY_PIN.posEntry.toString())
                        }
                    }
                    DetectCardType.MAG_CARD_TYPE -> {
                        //   vfIEMV?.importPin(1, data) // in Magnetic pin will not import
                        cardProcessedDataModal.setGeneratePinBlock(hexString2String(BytesUtil.bytes2HexString(data)))

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
              //  respondCVMResult(0.toByte())
                Log.d("Data", "PinPad onCancel")
                try {
                  //  GlobalScope.launch(Dispatchers.Main) {
                        activity.declinedTransaction()
                //    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            override fun onError(error: Int) {
                respondCVMResult(2.toByte())
                Log.d("Data", "PinPad onError, code:$error")
                try {
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as TransactionActivity).declinedTransaction()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        when (cvm.cvm) {
           /* CVMFlag.EMV_CVMFLAG_OFFLINEPIN.toByte() ->  {

               // hexString2String(emv!!.getTLV(Integer.toHexString(0x9F17).toUpperCase(Locale.ROOT))  )
                if(pinTryCounter == 0)
                    pinTryCounter = cvm.pinTimes.toInt()//emv!!.getTLV(Integer.toHexString(0x9F17).toUpperCase(Locale.ROOT)).toInt() ?: 0
                // val pinTryLimit = BytesUtil.hexString2Bytes(pinTryCounter)
                println("Pin Try limit is "+pinTryCounter)
                pinTryRemainingTimes = pinTryCounter
                println("Pin Try Remaining is "+pinTryRemainingTimes)

                if (pinTryRemainingTimes >= 3) {
                    pinTryRemainingTimes = 3
                    maxPin = 3
                } else {
                    pinTryRemainingTimes = pinTryCounter
                }
                if (maxPin == 3) {
                    println("Going in first")
                    when (pinTryRemainingTimes) {
                        3 -> {
                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                pinTryCounter--
                                pinTryRemainingTimes--
                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                            }
                        }
                        2 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "Wrong PIN.Please try again",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, false, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        1 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "Wrong PIN.This is your last attempt",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, false, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        else -> {
                        }
                    }
                }
                else if(pinTryRemainingTimes == 2){
                    println("Going in second")

                    when (pinTryRemainingTimes) {
                        2 -> {

                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                pinTryCounter--
                                pinTryRemainingTimes--
                                println("Pin Try Remaining is1 $pinTryRemainingTimes")
                            }
                        }
                        1 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "This is your last attempt",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, false, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        else -> {
                        }
                    }

                }
                else {
                    println("Going in else")
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                            "Invalid PIN",
                            "This is your last PIN attempt",
                            R.drawable.ic_txn_declined,
                            (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                            "", false, false, { alertPositiveCallback ->
                                if (alertPositiveCallback) {
                                    pinPad!!.startOfflinePinEntry(param, listener).also {
                                        pinTryCounter--
                                        pinTryRemainingTimes--
                                        println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                    }

                                }
                            },
                            {})
                    }
                }

            }*/

            CVMFlag.EMV_CVMFLAG_OFFLINEPIN.toByte() ->  {
println("ORIGINAL PIN LIMIT ---> ${cvm.pinTimes.toInt()}")
                // hexString2String(emv!!.getTLV(Integer.toHexString(0x9F17).toUpperCase(Locale.ROOT))  )
                if(pinTryCounter == -1)
                    pinTryCounter = cvm.pinTimes.toInt()//emv!!.getTLV(Integer.toHexString(0x9F17).toUpperCase(Locale.ROOT)).toInt() ?: 0
                // val pinTryLimit = BytesUtil.hexString2Bytes(pinTryCounter)

              if(pinTryCounter>3){
                  pinTryCounter=3
              }
                println("Pin Try limit is "+pinTryCounter)
                pinTryRemainingTimes = pinTryCounter
                println("Pin Try Remaining is "+pinTryRemainingTimes)

                if (pinTryRemainingTimes >= 3) {
                    pinTryRemainingTimes = 3
                    maxPin = 3
                } else {
                    pinTryRemainingTimes = pinTryCounter
                }
                if (maxPin == 3) {
                    println("Going in first")
                    when (pinTryRemainingTimes) {
                        3 -> {
                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                pinTryCounter--
                                pinTryRemainingTimes--
                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                            }
                        }
                        2 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "Wrong PIN.Please try again",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, false, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        1 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "Wrong PIN.This is your last attempt",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, false, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        else -> {
                            activity.txnDeclinedDialog("Wrong Pin Declined")
                            println("Going in first --> ELSE")
                        }
                    }
                }
                else if(pinTryRemainingTimes == 2){
                    println("Going in second")
                    when (pinTryRemainingTimes) {
                        2 -> {
                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                pinTryCounter--
                                pinTryRemainingTimes--
                                println("Pin Try Remaining is1 $pinTryRemainingTimes")
                            }
                        }
                        1 -> {
                            GlobalScope.launch(Dispatchers.Main) {
                                (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                                    "Invalid PIN",
                                    "This is your last attempt",
                                    R.drawable.ic_txn_declined,
                                    (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                                    "", false, false, { alertPositiveCallback ->
                                        if (alertPositiveCallback) {
                                            pinPad!!.startOfflinePinEntry(param, listener).also {
                                                pinTryCounter--
                                                pinTryRemainingTimes--
                                                println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                            }

                                        }
                                    },
                                    {})
                            }

                        }
                        else -> {
                            activity.txnDeclinedDialog("Wrong Pin Declined")
                            println("Going in second------> Else")
                        }
                    }

                }
                else {
                    println("Going in else")
                    GlobalScope.launch(Dispatchers.Main) {
                        (activity as? BaseActivityNew)?.alertBoxWithActionNew(
                            "Invalid PIN",
                            "This is your last PIN attempt",
                            R.drawable.ic_txn_declined,
                            (activity as? BaseActivityNew)!!.getString(R.string.positive_button_ok),
                            "", false, false, { alertPositiveCallback ->
                                if (alertPositiveCallback) {
                                    pinPad!!.startOfflinePinEntry(param, listener).also {
                                        pinTryCounter--
                                        pinTryRemainingTimes--
                                        println("Pin Try Remaining is1 " + pinTryRemainingTimes)
                                    }

                                }
                            },
                            {})
                    }
                }

            }


            CVMFlag.EMV_CVMFLAG_ONLINEPIN.toByte() -> {
                println("=> onCardHolderVerify | onlinpin")
                cardProcessedDataModal.setIsOnline(1)


                byte2HexStr(lastCardRecord!!.pan)
                println("Pan block is "+byte2HexStr(lastCardRecord!!.pan))
                addPad("374245001751006", "0", 16, true)

                println("CardPanNumber data is "+cardProcessedDataModal.getPanNumberData() ?: "")

                Utility.hexStr2Byte(addPad("374245001751006", "0", 16, true))
                param.putByteArray(PinpadData.PAN_BLOCK,
                    Utility.hexStr2Byte(
                        addPad(
                            cardProcessedDataModal.getPanNumberData() ?: "",
                            "0",
                            16,
                            true
                        )
                    )
                )
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
            println("...onCardHolderVerify: respondEvent$ret")
        } catch (e: Exception) {
            //handleException(e);
        }
    }

    // 5 calling
    @Throws(RemoteException::class)
    open fun doReadRecord(record: CardRecord?) {
        println("=> onReadRecord | " + BytesUtil.bytes2HexString(record?.pan))
        var track22: String? = null
        val track2 = BytesUtil.bytes2HexString(record?.pan)

        var a = track2.indexOf('F')
        if (a > 0) {
            track22 = track2.substring(0, a)
        } else {
            a = track2.indexOf('=')

            track22 = if (a > 0) {
                track2.substring(0, a)
            }else{
                track2
            }
        }

        cardProcessedDataModal.setPanNumberData(track22 ?: "")
        System.out.println("Card pannumber data "+cardProcessedDataModal.getPanNumberData())
       // val encrptedPan = getEncryptedPanorTrackData(EMVInfoUtil.getRecordDataDesc(record),false)
        // cardProcessedDataModal.setEncryptedPan(encrptedPan)


        val tvDynamicLimit = emv!!.getTLV(Integer.toHexString(0x9F70))  // card Type TAG
        if (null != tvDynamicLimit && !(tvDynamicLimit.isEmpty())) {
            println("Dynamic Limit  Type ---> " + tvDynamicLimit)
           //cardProcessedDataModal.setcardLabel(hexString2String(tvDynamicLimit))
        }

       record?.pan?.byteArr2HexStr()
       // settingCAPkeys(emv)



        println("...onReadRecord: respondEvent" + emv!!.respondEvent(null))
    }


    // 4 calling
    open fun doSendOut(ins: Int, data: ByteArray) {
        when (ins) {
            KernelINS.DISPLAY ->            // DisplayMsg: MsgID（1 byte） + Currency（1 byte）+ DataLen（1 byte） + Data（30 bytes）
                if (data[0] == MessageID.ICC_ACCOUNT.toByte()) {
                    val len: Byte = data[2]
                    val account = BytesUtil.subBytes(data, 1 + 1 + 1, len.toInt())
                    val accTLVList = TLVList.fromBinary(account)
                    val track2 = BytesUtil.bytes2HexString(accTLVList.getTLV("57").bytesValue)
                    println("Field 57 data is1"+track2)
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

    fun getCompleteSecondGenAc(testCompleteSecondGenAc: CompleteSecondGenAc, cardProcessedDataModal: CardProcessedDataModal?){
        this.testCompleteSecondGenAc = testCompleteSecondGenAc
        logger("vfemvHan",""+this,"e")
        if (cardProcessedDataModal != null) {
            this.cardProcessedDataModal  = cardProcessedDataModal
        }
    }
}






