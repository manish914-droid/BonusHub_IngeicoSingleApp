package com.bonushub.crdb.india.view.baseemv
import android.content.Intent
import android.os.RemoteException
import android.util.Log
import com.bonushub.crdb.india.R
import com.bonushub.crdb.india.model.CardProcessedDataModal
import com.bonushub.crdb.india.utils.BytesUtil
import com.bonushub.crdb.india.utils.DeviceHelper
import com.bonushub.crdb.india.utils.addPad
import com.bonushub.crdb.india.utils.hexString2String
import com.bonushub.crdb.india.utils.ingenico.DialogUtil
import com.bonushub.crdb.india.utils.ingenico.EMVInfoUtil
import com.bonushub.crdb.india.utils.ingenico.TLV
import com.bonushub.crdb.india.utils.ingenico.TLVList
import com.bonushub.crdb.india.view.activity.NavigationActivity
import com.bonushub.crdb.india.view.base.BaseActivityNew
import com.bonushub.crdb.india.vxutils.BhTransactionType
import com.usdk.apiservice.aidl.emv.*
import com.usdk.apiservice.aidl.emv.EMVTag.*

/**
 * Author Lucky Rajput
 *
 */
//=========================================================//

/**
 * settingAids(emv: UEMV?) used for setting the AIDs that are
 * supported by the terminal.
 */



fun settingAids(emv: UEMV?) {
    println("****** Setting AID ******")
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
        val ret = emv?.manageAID(ActionFlag.ADD, aid, true)
        println("$ret=> add AID : $aid")
    }

  // emv?.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PSE_FLAG, "03")
}

/**
 * settingCAPkeys(emv: UEMV?) used for setting the CAP keys
 * for ODA
 */

 fun settingCAPkeys(emv: UEMV?) {
     if(emv?.setEMVProcessOptimization(true) == true) {
         emv.manageCAPubKey(ActionFlag.CLEAR, null)
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

             //region ===========================================Amex Test Cap keys
             "9F0605A000000025" +
                     "9F2201C8" +
                     "DF0503201231DF060101DF070101DF028190BF0CFCED708FB6B048E3014336EA24AA007D7967B8AA4E613D26D015C4FE7805D9DB131CED0D2A8ED504C3B5CCD48C33199E5A5BF644DA043B54DBF60276F05B1750FAB39098C7511D04BABC649482DDCF7CC42C8C435BAB8DD0EB1A620C31111D1AAAF9AF6571EEBD4CF5A08496D57E7ABDBB5180E0A42DA869AB95FB620EFF2641C3702AF3BE0B0C138EAEF202E21DDF040103DF031433BD7A059FAB094939B90A8F35845C9DC779BD50", //c8
             "9F0605A000000025" +
                     "9F2201C9" +
                     "DF0503201231DF060101DF070101DF0281B0B362DB5733C15B8797B8ECEE55CB1A371F760E0BEDD3715BB270424FD4EA26062C38C3F4AAA3732A83D36EA8E9602F6683EECC6BAFF63DD2D49014BDE4D6D603CD744206B05B4BAD0C64C63AB3976B5C8CAAF8539549F5921C0B700D5B0F83C4E7E946068BAAAB5463544DB18C63801118F2182EFCC8A1E85E53C2A7AE839A5C6A3CABE73762B70D170AB64AFC6CA482944902611FB0061E09A67ACB77E493D998A0CCF93D81A4F6C0DC6B7DF22E62DBDF040103DF03148E8DFF443D78CD91DE88821D70C98F0638E51E49", //c9
             "9F0605A000000025" +
                     "9F2201CA" +
                     "DF0503201231DF060101DF070101DF0281F8C23ECBD7119F479C2EE546C123A585D697A7D10B55C2D28BEF0D299C01DC65420A03FE5227ECDECB8025FBC86EEBC1935298C1753AB849936749719591758C315FA150400789BB14FADD6EAE2AD617DA38163199D1BAD5D3F8F6A7A20AEF420ADFE2404D30B219359C6A4952565CCCA6F11EC5BE564B49B0EA5BF5B3DC8C5C6401208D0029C3957A8C5922CBDE39D3A564C6DEBB6BD2AEF91FC27BB3D3892BEB9646DCE2E1EF8581EFFA712158AAEC541C0BBB4B3E279D7DA54E45A0ACC3570E712C9F7CDF985CFAFD382AE13A3B214A9E8E1E71AB1EA707895112ABC3A97D0FCB0AE2EE5C85492B6CFD54885CDD6337E895CC70FB3255E3DF040103DF03146BDA32B1AA171444C7E8F88075A74FBFE845765F", //CA,
// endregion

             //region ========================================== Visa Test cap keys Starts==================================================================================================================================================================================================================================
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

             //region =============================== Visa Live cap keys start==================================================================================================================================================================================================================================
             //Visa Live cap Keys 08
             "9F0605A000000003" + //AID
                     "9F220108" + //Index
                     "DF050420241231" + //Expiry Date
                     "DF0281B0" + "D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0B" + //module
                     "DF040103" + //exponent
                     "DF0314" + "20D213126955DE205ADC2FD2822BD22DE21CF9A8", //exponent

             //Visa Live cap Keys 09
             "9F0605A000000003" + //AID
                     "9F220109" + //Index
                     "DF050420241231" + //Expiry Date
                     "DF0281F8" + "9D912248DE0A4E39C1A7DDE3F6D2588992C1A4095AFBD1824D1BA74847F2BC4926D2EFD904B4B54954CD189A54C5D1179654F8F9B0D2AB5F0357EB642FEDA95D3912C6576945FAB897E7062CAA44A4AA06B8FE6E3DBA18AF6AE3738E30429EE9BE03427C9D64F695FA8CAB4BFE376853EA34AD1D76BFCAD15908C077FFE6DC5521ECEF5D278A96E26F57359FFAEDA19434B937F1AD999DC5C41EB11935B44C18100E857F431A4A5A6BB65114F174C2D7B59FDF237D6BB1DD0916E644D709DED56481477C75D95CDD68254615F7740EC07F330AC5D67BCD75BF23D28A140826C026DBDE971A37CD3EF9B8DF644AC385010501EFC6509D7A41" + //module
                     "DF040103" + //exponent
                     "DF0314" + "1FF80A40173F52D7D27E0F26A146A1C8CCB29046", //exponent

//endregion =========================================================================== Visa Live cap keys ends==================================================================================================================================================================================================================================

          /*   // region ===================Visa CLS cap keys=============
             "9F3303E0F8C8" +
                     "97099F02065F2A029A0390" +
                     "9F40056F00F0F001" + //additional terminal capability
                     "9F0607A0000000038010" +
                     "DF0306009999999999" +
                   //  "DF2006${ctlsVal}" +
                     "DF010100" +
                     "DF14039F3704" +
                     "9F6604F6004000" +
                     "5F2A020356" +
                     "DF170101" +
                     "9F09020096" + //Application version number
                     "DF180101" +
                     "DF1205D84004F800" + //Tac online
                     "9F1B0400000000" +
                     "9F1A020356" +
                    // "DF2106${cvmVal}" +
                     "DF160101" +
                     "DF150400000000" +
                     "DF1105D84004A800" +
                     "DF0406000000000000" +
                     "DF1906000000000000" +
                     "DF13050010000000",

             "9F3303E0F8C8" +
                     "97099F02065F2A029A0390" +
                     "9F40056F00F0F001" + //additional terminal capability
                     "9F0607A0000000032010" +
                     "DF0306009999999999" +
                 //    "DF2006${ctlsVal}" +
                     "DF010100" +
                     "DF14039F3704" +
                     "9F6604F6004000" +
                     "5F2A020356" +
                     "DF170101" +
                     "9F09020096" + //Application version number
                     "DF180101" +
                     "DF1205D84004F800" + //Tac online
                     "9F1B0400000000" +
                     "9F1A020356" +
                   //  "DF2106${cvmVal}" +
                     "DF160101" +
                     "DF150400000000" +
                     "DF1105D84004A800" +
                     "DF0406000000000000" +
                     "DF1906000000000000" +
                     "DF13050010000000",


             "9F3303E0F8C8" +
                     "97099F02065F2A029A0390" +
                     "9F40056F00F0F001" +  //Additional terminal capability
                     "9F0607A0000000031010" +
                     "DF0306009999999999" +
                  //   "DF2006${ctlsVal}" + //Contact less Maximum Transaction Limit
                     "DF010100" +           //Application id
                     "DF14039F3704" +       //DDOL (Dynamic data authetication...)
                     "9F6604F6004000" +           //Teminal transaction qualifier
                     "5F2A020356" +         //  Transaction currency code
                     "DF170101" +           //The target percentage randomly choosen
                     "9F09020096" + //Application version number
                     "DF180101" +           //Online pin
                     "DF1205A0109C9800" +   //TAC online
                     "9F1B0400000000" +      //Minimum Limit //floor limit
                     "9F1A020356" +          //Terminal Country code
                   //  "DF2106${cvmVal}" +  //Terminal cvm(cardholder verification methods) quota
                     "DF160101" +             //Bias select the maximum percentage of target
                     "DF150400000000" +       //offset Randomly selected thresold
                     "DF1105A4109C0000" +     //TAC Default
                     "DF0406000000000000" +
                     "DF1906000000000000" +    //Contact less offline minimum
                     "DF13055C40000000",       //TAC Refuse

             "9F3303E0F8C8" +
                     "97099F02065F2A029A0390" +
                     "9F40056F00F0F001" +  //Additional terminal capability
                     "9F0608A000000003101001" +
                   //  "DF2006${ctlsVal}" +
                     "DF010100" +
                     "DF14039F3704" +
                     "9F6604F6004000" +
                     "5F2A020156" +
                     "DF170101" +
                     "9F0902008D" + //Application version number
                     "DF180101" +
                     "DF1205A0109C9800" +
                     "9F1B0400000000" +
                     "9F1A020356" +
                   //  "DF2106${cvmVal}" +
                     "DF160101" +
                     "DF150400000000" +
                     "DF1105A4109C0000" +
                     "DF0406000000000000" +
                     "DF1906000000000000" +
                     "DF13055C40000000",

             "9F3303E0F8C8" +
                     "97099F02065F2A029A0390" +
                     "9F40056F00F0F001" +  //Additional terminal capability
                     "9F0608A000000003101002" +
                   //  "DF2006${ctlsVal}" +
                     "DF010100" +
                     "DF14039F3704" +
                     "9F6604F6004000" +
                     "5F2A020156" +
                     "DF170101" +
                     "9F0902008D" + //Application version number
                     "DF180101" +
                     "DF1205A0109C9800" +
                     "9F1B0400000000" +
                     "9F1A020356" +
                 //    "DF2106${cvmVal}" +
                     "DF160101" +
                     "DF150400000000" +
                     "DF1105A4109C0000" +
                     "DF0406000000000000" +
                     "DF1906000000000000" +
                     "DF13055C40000000",

             // endregion =============*/

/*
//region ======================= Master Testcap keys starts=======================================================
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
                     "DF0281B0" + "B8048ABC30C90D976336543E3FD7091C8FE4800DF820ED55E7E94813ED00555B573FECA3D84AF6131A651D66CFF4284FB13B635EDD0EE40176D8BF04B7FD1C7BACF9AC7327DFAA8AA72D10DB3B8E70B2DDD811CB4196525EA386ACC33C0D9D4575916469C4E4F53E8E1C912CC618CB22DDE7C3568E90022E6BBA770202E4522A2DD623D180E215BD1D1507FE3DC90CA310D27B3EFCCD8F83DE3052CAD1E48938C68D095AAC91B5F37E28BB49EC7ED597" + //module
                     "DF040103" + //exponent
                     "DF0314" + "EBFA0D5D06D8CE702DA3EAE890701D45E274C845", //exponent

             //Master Live cap Keys 06
             "9F0605A000000004" + //AID
                     "9F220106" + //Index
                     "DF050420241231" + //Expiry Date
                     "DF0281F8" + "CB26FC830B43785B2BCE37C81ED334622F9622F4C89AAE641046B2353433883F307FB7C974162DA72F7A4EC75D9D657336865B8D3023D3D645667625C9A07A6B7A137CF0C64198AE38FC238006FB2603F41F4F3BB9DA1347270F2F5D8C606E420958C5F7D50A71DE30142F70DE468889B5E3A08695B938A50FC980393A9CBCE44AD2D64F630BB33AD3F5F5FD495D31F37818C1D94071342E07F1BEC2194F6035BA5DED3936500EB82DFDA6E8AFB655B1EF3D0D7EBF86B66DD9F29F6B1D324FE8B26CE38AB2013DD13F611E7A594D675C4432350EA244CC34F3873CBA06592987A1D7E852ADC22EF5A2EE28132031E48F74037E3B34AB747F" + //module
                     "DF040103" + //exponent
                     "DF0314" + "F910A1504D5FFB793D94F3B500765E1ABCAD72D9", //exponent
//endregion =========================================================================== Master Live cap keys ends==================================================================================================================================================================================================================================

             // region =========================================================================== Rupay Cap keys Starts==================================================================================================================================================================================================================================
             //Rupay Live cap keys 01
             "9F0605A000000524" + //Aids
                     "9F220101" + //Key Id
                     "DF050420241231" + //expiry Date
                     "DF02" + "8180" + "ED2CBDC0E377CFE24640B819AAA606EF9B177F50A4C55917A53266CFB90DCA7C39D0C429207BFC29786945F912D59AA67508C62FA5B19E187D9FAEFB718084E14E7ADDBCC69211D4418CDE26FD10FCE8BFFFC46877768FED780615E74AA162EC823CF548FFEE5B263A9FE56FF42DE215A7112F9A1878DF81178AD1D69F0D47F5" + //Module
                     "DF040103" + //exponent
                     "DF03" + "14" + "6E57B86C9C10A3EB7AA4BB4342F902D58444D69B" + //checksum
                     "DF060101" + // Hash Ind
                     "DF070101",// ARITH_IND

             //Rupay Live cap keys 02
             "9F0605A000000524" + //Aids
                     "9F220102" + //Key Id
                     "DF050420241231" + //expiry Date
                     "DF02" + "8180" + "B628CAB46EC37E24B0EFE78FD651BADC2545958B4D088FE8DA8CF7F04CF1916EF894386376608814C2F6F1662D964D0F7CA34C29E2FFEBF4AB4AB8E35BACCCF6D653EBB06AA1FCA3A877A6644B54894B9E1FAFBA7206F40BB4AF2EE7F9964F8259CE8A82DB00460B5820FA4CDF0D213E3710F5CA40838D57FC956B134760A0B1" + //Module
                     "DF040103" + //exponent
                     "DF03" + "14" + "52A1FE6E066F82CD484F1E7FEA10B7E5F02CEC8B" + //checksum
                     "DF060101" + // Hash Ind
                     "DF070101",// ARITH_IND

             //Rupay Live cap keys 03
             "9F0605A000000524" + //Aids
                     "9F220103" + //Key Id
                     "DF050420241231" + //expiry Date
                     "DF02" + "8190" + "E703A908FFAE3730F82E550869A294C1FF1DA25F2B53D2C8BB18F770DAD505135D03D5EC8EE3926550051C3D4857F6FEDB882C2889E0B25F389F78741F2931A92D45D3A47E62810D3253653AB0AB3570C35DFD08D3167B6DB42ED28F765186F4287CDAF9D9BAD20BCE2C4ECFECDD218E50F1FCC718878882F3934A6FEB502CFCAD615A2B2E279A0868DDA9489DFA9CD9" + //Module
                     "DF040103" + //exponent
                     "DF03" + "14" + "4B93D1E1F57CFA16970501F17D3E06411043F1D5" + //checksum
                     "DF060101" + // Hash Ind
                     "DF070101",// ARITH_IND

             //Rupay Live cap keys 04
             "9F0605A000000524" + //Aids
                     "9F220104" + //Key Id
                     "DF050420241231" + //expiry Date
                     "DF02" + "81B0" + "AC0019624FC0A72270C6885CC0B3C9140C351FCFE6F8145881A27750393453D3265F69E7658132D8D253EDF8991E2BA32B782D39ADE1FF1FC8F211F5DF51A0007C761AD9882587BD6A36AECD3ABBF944307AC97A2D905FAB489C3E1CCD76DE9EB93ECFAB2BB84F34E770119E356DC6372D8685DA8EB92FCAC7B53C0167100E4CDFB9830D1C45E787E44C9F6A42EC131A6A4CD66BBE4F93CA91FDF157C7B22FC7221A6348F0EDA6151302A80EF77D6CA5" + //Module
                     "DF040103" + //exponent
                     "DF03" + "14" + "6F843CE765B9144CE1A6BFEA46BC37B65081CE7F" + //checksum
                     "DF060101" + // Hash Ind
                     "DF070101",// ARITH_IND

             //Rupay Live cap keys 05
             "9F0605A000000524" + //Aids
                     "9F220105" + //Key Id
                     "DF050420241231" + //expiry Date
                     "DF02" + "81F8" + "C04E80180369898AAEF6EE7741EDED25239D765301614B5B41A008CA3009358D626D828BC5F1B1E04A2DC1367101266905D262003BE747FD231C9B0011F2F2B21BA8E4C0F4CA5E93ED9DBB2E92ABC450576A4EB59AD00DCA59C8BF3230E4B19D43452871C6215D837663310DF43CAEA1B9B08C1F500AF1B550F62E18D70EEE9E9475321BCD1799AB193E0BC849DACE892A0E6A1F42FE0786DB30345AE1A0E7E4C4B71640E03BFD2832C491A7D83F3B4EF4D388CDDBB748C2FD1D9D4A9BF52FC856CBA088D4B274846002C23CDA722C5CFF3B1F8218A1843B0426474BDC92F2F5E31FBF321CC17480AD069DF55381F2E601D5CBA7B871253F" + //Module
                     "DF040103" + //exponent
                     "DF03" + "14" + "7081DF2A0C36360F24C122C574F0AD2E57893DD2" + //checksum
                     "DF060101" + // Hash Ind
                     "DF070101",// ARITH_IND

             //Rupay Live cap keys 06
             "9F0605A000000524" + //Aids
                     "9F220106" + //Key Id
                     "DF050420241231" + //expiry Date
                     "DF02" + "81F8" + "9D8A75B36BCBDF250B87615A46F6EA35DE35226EEAB7B473D7DC0A28B5DF075C83B2775F23337E6CEE36CCFE3A6568C9C822D6DE81299565A829348E03D479B631BB18A2429A8590C597F446A3CEA3BE2E822106F43DFBB981EC0F1121919CB35F85DBA3355C5E7FF35F2B221FD65EDBEA41F23A7A109FBBC4A774A756D89B593B199E1E9DA9A99217D4BF31F67CDA8C4E1B81FA2A377C83B5D1CD6AF1F1880448CFF48D3A4ADBBC7FBD730061508A6EA8FDFC5BD66A2E94E33B83F81E0E56CF1C9473E4426EE435F9E80136760D8F4AD946805B03A67C55361582F5AD8F40404392FA4CB4F5C2BAF6E26857A1D60941E3D055ACD9AC0BEF" + //Module
                     "DF040103" + //exponent
                     "DF03" + "14" + "E98F4134E1949A9A054E4679AC9A7EC83969E209" + //checksum
                     "DF060101" + // Hash Ind
                     "DF070101",// ARITH_IND
//endregion =========================================================================== Rupay Live Cap keys ends==================================================================================================================================================================================================================================

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
             val ret = emv.manageCAPubKey(ActionFlag.ADD, capKey)
             println(
                 "=> add CAPKey rid = : " + BytesUtil.bytes2HexString(rid)
                     .toString() + ", index = " + index + "return type " + ret
             )
         }
     }
 }

 fun doEmvAppSelection(reSelect: Boolean, candList: MutableList<CandidateAID>,emv: UEMV?,activity: BaseActivityNew) {
    println("=> onAppSelect: cand AID size = " + candList.size)
    if (candList.size > 1) {
        openEmvAppSelectionDialog(candList, object : DialogUtil.OnSelectListener {
            override fun onCancel() {
                try {
                    emv?.stopEMV()
                    val intent = Intent(activity, NavigationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)

                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun onSelected(item: Int) {
                setSelectedEmvApp(candList[item].aid,emv)
            }
        },activity)
    } else {
        setSelectedEmvApp(candList[0].aid,emv)
    }
}

private fun openEmvAppSelectionDialog(candList: List<CandidateAID>, listener: DialogUtil.OnSelectListener?,activity: BaseActivityNew) {
    activity.runOnUiThread {
        DialogUtil.showSelectDialog(
            activity,
            activity.getString(R.string.please_select_app),
            candList,
            0,
            listener
        )
    }
}

//respondAID
fun setSelectedEmvApp(aid: ByteArray?, emv: UEMV?){
    try {
        println("Select aid: " + BytesUtil.bytes2HexString(aid))
        val tmAid = TLV.fromData(EMVTag.EMV_TAG_TM_AID, aid)
        println(""+ emv?.respondEvent(tmAid.toString())+ "...onAppSelect: respondEvent")
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
}

//doFinalSelect
 fun createAndSetCDOL1ForFirstGenAC(finalData: FinalData, cardProcessedDataModal: CardProcessedDataModal, emv: UEMV?) {
    println("=> onFinalSelect | " + EMVInfoUtil.getFinalSelectDesc(finalData))
    val datetime: String = DeviceHelper.getCurentDateTime()
    val splitStr = datetime.split("\\s+".toRegex()).toTypedArray()
   val txnDate=splitStr[0]
    val txnTime=splitStr[1]


    val txnAmount = addPad(cardProcessedDataModal.getTransactionAmount().toString(), "0", 12, true)
   val otherAmount= addPad(cardProcessedDataModal.getOtherAmount().toString(), "0", 12, true)

    var aidstr = BytesUtil.bytes2HexString(finalData.aid).subSequence(0, 10).toString()
    var tlvList: String? = null
    when (finalData.kernelID) {
      //  emv?.setTLV(KernelID.AMEX, EMVTag.DEF_TAG_PSE_FLAG, "03")
        // for EMV this call is common for every payment scheme
        KernelID.EMV.toByte() -> {
            tlvList = StringBuilder()
                .append("9F350122")
              //  .append("9F3303E0E8C8")
                .append("9F3303E0F0C8") // Terminal Capability // value is changed for support Enciphered
                .append("9F40056000F0B001")
                .append("9F1A020356")
                .append("5F2A020356")
                .append("9F09020001")
                .append("9A03").append(splitStr[0])   //Txn Date - M
                .append("9F2103").append(splitStr[1]) //Txn Time - M

                .append("9F410400000001")  //Transaction Sequence Counter - 0
                .append("DF918111050010000000") // Terminal action code(decline)
                    //D84000F800
                .append("DF91811205D84000F800") // Terminal action code(online)

                    //D84000A812
                .append("DF91811005D84000A812")  // Terminal action code(default)

                .append("9F6D01C0")              // Contactless Reader Capabilities
                .append("9F6E04D8E00000")      //  Enhanced Contactless Reader Capabilities
                .append("DF812406000000010000") //Terminal Contactless Transaction Limit
                .append("DF812606000000000100") // Terminal CVM Required Limit
                .append("DF812306000000000000")  //Terminal Contactless Floor Limit
                .append("DF81300100")            //Try Again Flag
                .toString()

            //0010000000
         //   emv?.setTLV(finalData.kernelID.toInt(),EMVTag.DEF_TAG_TAC_DECLINE,cvmTransLimit)//DF8124

  //    tlvList=tlvList+"9C01"+tag9CData +"9F0206"+tag9f02Data+"9F0306"+tag9f03Data
Log.e("TLV LIST --> ",tlvList)

        }
        KernelID.AMEX.toByte() -> {
            tlvList = StringBuilder()
             //   .append("9F6604A6004000") ///terminal transaction attribute 86004000  // "9F660426000080 //TTQ
               // .append("DF812304A6004000")
                .append("9F350122")
                .append("9F3303E0E8C8")
                .append("9F40056000F0B001")
                .append("9F1A020356")
                .append("5F2A020356")
                .append("9F09020001")
              //  .append("9C0100")
             //   .append("9F0206").append(txnAmount) //Txn Amount
            //    .append("9F0306000000000000")
             //   .append("9A03").append(splitStr[0])   //Txn Date - M
             //   .append("9F2103").append(splitStr[1]) //Txn Time - M
                .append("9F410400000001")
                .append("DF918111050000000000") // Terminal action code(decline)
                .append("DF918112050000000000") // Terminal action code(online)
                .append("DF918110050000000000")  // Terminal action code(default)

                .append("9F6D01C0")              // Contactless Reader Capabilities
                .append("9F6E04D8E00000")      //  Enhanced Contactless Reader Capabilities
                .append("DF812406000000010000") //Terminal Contactless Transaction Limit
                .append("DF812606000000000500") // Terminal CVM Required Limit
                .append("DF812306000000000000")  //Terminal Contactless Floor Limit
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
                .append("9F410400000001")
                .append("9F350122")
                .append("9F3303E0F0C8")
             //   .append("9F3303E0E8C8")
                .append("9F40056000F0B001")
                .append("9F1A020356")
                .append("5F2A020356")
                .append("9F09020001")

                .append("9F410400000001")  //Transaction Sequence Counter - 0

                .append("9F1B0400003A98")

               // .append("9F660436004000") // TTQ
                .append("9F6604F6004000") // TTQ
                .append("DF06027C00")

                .append("DF918111050010000000") // Terminal action code(decline)
                //D84000F800
                .append("DF91811205D84000F800") // Terminal action code(online)
                //D84000A812
                .append("DF91811005D84000A812")  // Terminal action code(default)

              //  .append("9F6D01C0")              // Contactless Reader Capabilities
            //    .append("9F6E04D8E00000")      //  Enhanced Contactless Reader Capabilities

                .append("DF812306000000000000")  //Terminal Contactless Floor Limit
               // .append("DF81300100")            //Try Again Flag

             //   .append("DF812306000000100000")
              //  .append("DF812606000000100000")
                    
             //   .append("DF918165050100000000") --->
                .append("DF040102")
                .append("DF810602C000")
                .append("DF9181040100").toString()


            val cvmTransLimit="000000200000"
            val limitCvm="000000050000"

            emv?.setTLV(finalData.kernelID.toInt(),EMVTag.V_TAG_TM_TRANS_LIMIT,cvmTransLimit)//DF8124
            emv?.setTLV(finalData.kernelID.toInt(),EMVTag.V_TAG_TM_CVM_LIMIT,limitCvm)//DF8126
          //  emv?.setTLV(finalData.kernelID.toInt(),EMVTag.EMV_TAG_TM_CVMRESULT,"3F0000")//9F34

        }
        KernelID.MASTER.toByte() -> {            // Parameter settings, see transaction parameters of PAYPASS in《UEMV develop guide》.

            tlvList = StringBuilder()
                .append("9F350122")
                .append("9F3303E0F8C8")
                .append("9F40056000F0A001")
                .append("9A03171020")
                .append("9F2103150512")
                //    .append("9F0206").append(txnAmount)
                .append("9F1A020156")
                .append("5F2A020156")


                .append("DF918111050000000000")
                .append("DF91811205FFFFFFFFFF")
                .append("DF91811005FFFFFFFFFF")
                .append("DF9182010102")
                .append("DF9182020100")
                .append("DF9181150100")
                .append("DF9182040100")



                //  .append("DF812406000000030000") //Terminal Contactless Transaction Limit --> working
                .append("DF812506000000010000")
                //    .append("DF812606000000010000")// Terminal CVM Required Limit
                .append("DF812306000000000000")// //Terminal Contactless Floor Limit

                .append("DF9182050160")
                .append("DF9182060160")
                .append("DF9182070120")
                .append("DF9182080120").toString()


            val limitCvm="000000010000"
            val cvmTransLimit="000000030000"

            emv?.setTLV(finalData.kernelID.toInt(),EMVTag.M_TAG_TM_TRANS_LIMIT,cvmTransLimit)//DF8124
              emv?.setTLV(finalData.kernelID.toInt(),EMVTag.M_TAG_TM_CVM_LIMIT,limitCvm)//DF8126

            //    emv?.setTLV(finalData.kernelID.toInt(),EMVTag.R_TAG_TM_CVM_LIMIT,limitCvm)// DF48
            //    emv?.setTLV(finalData.kernelID.toInt(),EMVTag.DEF_TAG_J_CVM_LIMIT,limitCvm)//DF918403
            //  M_TAG_TM_FLOOR_LIMIT
        //    emv?.setTLV(finalData.kernelID.toInt(),EMVTag.M_TAG_TM_TRANS_LIMIT_CDV,limitCvm)//DF8125
        }
        KernelID.RUPAY.toByte() , KernelID.DISCOVER.toByte()-> {
            tlvList = StringBuilder()
               // .append("9F0206000000000100")
                .append("9F3303E06840")
                .append("DF4C06000000015000")
                .append("DF812406000000015000")
                .append("DF8142011E")
                .append("9F350122")
                .append("9F4005F040F0B001")
                .append("9F1A020156")
                .append("5F2A020156")
                .append("9F09020002")
                .append("9C0100")
                .append("9A03171020")
                .append("9F2103150512")
                .append("9F410400001234")
                .append("9F1B0400002710")
                .append("DF918111050410000000")
                .append("DF918112059060009000")
                .append("DF918110059040008000")
                .append("DF814002002C")
                .append("DF16020015")
                .append("DF3A050040000000")
                .append("DF4D06000000010000")
            //    .append("DF812406000000020000")//Trans Limit
                .append("DF812606000000010000")//Terminal CVM Required Limit

                .append("DF812306000000008000")
                .append("DF9181050100")
                .append("DF9181020100").toString()



         /*
         ========= VERIFONE ====
         tlvList=    "9F3303E0F8C8" + // terminal capability
                    "97099F02065F2A029A0390" +
                    "9F40056F00F0F001" + //additional terminal capability
                    "9f0607A0000005241010" +
                    "DF0306009999999999" +
                  // "DF2006${ctlsVal}" +
                  "DF2006000000009999" +
                    "DF010100" +
                    "DF14039F3704" +
                    "9F6604F6004000" + //TTQ
                    "5F2A020356" +
                    "DF170101" +
                    "9F09020002" +
                    "DF180101" +
                    "DF1205D84004F800" +
                    "9F1B0400000000" +
                    "9F1A020356" +
                   // "DF2106${cvmVal}" +   //CVm Limit
                  "DF2106000000050000" +   //CVm Limit
                    "DF160101" +
                    "DF150400000000" +
                    "DF1105D84004A800" +
                    "DF0406000000000000" +
                    "DF1906000000000000" +
                    "DF13050010000000"*/

        }
       // KernelID.DISCOVER.toByte() -> {}
        KernelID.JCB.toByte() -> {}
        else -> {}
    }

    val tag9CData: String // Txn Type
    val tag9f02Data:String //Txn Amount
    val tag9f03Data:String //Other Amount


    when(cardProcessedDataModal.getTransType()){

        BhTransactionType.SALE_WITH_CASH.type->{
            tag9CData= "09"
            tag9f02Data=addPad((txnAmount.toLong()+otherAmount.toLong()).toString(), "0", 12, true)
            tag9f03Data=otherAmount
        }
        BhTransactionType.CASH_AT_POS.type->{
            tag9CData= "01"
            tag9f02Data=txnAmount
            tag9f03Data=otherAmount
        }
        BhTransactionType.REFUND.type->{
            tag9CData= "20"
            tag9f02Data=txnAmount
            tag9f03Data=otherAmount
        }
        else->{
            tag9CData= "00"
            tag9f02Data=txnAmount
            tag9f03Data=otherAmount
        }
    }

    emv?.setTLV(finalData.kernelID.toInt(),EMV_TAG_TM_AUTHAMNTN,tag9f02Data)
    emv?.setTLV(finalData.kernelID.toInt(),EMV_TAG_TM_OTHERAMNTN,tag9f03Data)
    emv?.setTLV(finalData.kernelID.toInt(),EMV_TAG_TM_TRANSTYPE,tag9CData)
    emv?.setTLV(finalData.kernelID.toInt(),EMV_TAG_TM_TRANSDATE,txnDate)
    emv?.setTLV(finalData.kernelID.toInt(),EMV_TAG_TM_TRANSTIME,txnTime)

    println(""+emv?.setTLVList(finalData.kernelID.toInt(),tlvList) +"...onFinalSelect: setTLVList")
    println("...onFinalSelect: respondEvent" + emv?.respondEvent(null))
}

fun utilityFunctionForCardDataSetting(cardProcessedDataModal: CardProcessedDataModal, emv: UEMV) {

    try {
        val tlvcardTypeLabel = emv.getTLV(Integer.toHexString(0x50))  // card Type TAG //50
        val cardTypeLabel = if (tlvcardTypeLabel?.isNotEmpty() == true) {
            tlvcardTypeLabel
        }
        else {
            ""
        }
        Log.d(EmvHandler.TAG,"Card Type ->${hexString2String(cardTypeLabel)}")
        cardProcessedDataModal.setcardLabel(hexString2String(cardTypeLabel))

    } catch (ex: Exception) {
        Log.e(EmvHandler.TAG, ex.message ?: "")
    }

    try {
        val tlvcardHolderName = emv?.getTLV(Integer.toHexString(0x5F20))   // CardHolder Name TAG //5F20
        val cardHolderName = if (tlvcardHolderName?.isNotEmpty() == true) {
            tlvcardHolderName
        }
        else {""}
        Log.d(EmvHandler.TAG,"Card Holder Name ---> ${hexString2String(cardHolderName)}")

        cardProcessedDataModal.setCardHolderName(hexString2String(cardHolderName))

    } catch (ex: Exception) {
        Log.e(EmvHandler.TAG, ex.message ?: "")
    }

    try {
        val tlvAppLabel = emv?.getTLV(Integer.toHexString(0x9F12))   // application label  TAG // 9F12
        val appLabel = if (tlvAppLabel?.isNotEmpty() == true) {
            val removespace = hexString2String(tlvAppLabel)
            val finalstr = removespace.trimEnd()
            finalstr
        }
        else {
            ""
        }
        Log.d(EmvHandler.TAG,"Application label ->${appLabel}")
        cardProcessedDataModal.setApplicationLabel(appLabel)
    } catch (ex: Exception) {
        Log.e(EmvHandler.TAG, ex.message ?: "")
    }

    try {
        val tlvissuerCounTryCode = emv?.getTLV(Integer.toHexString(0x5F2A))    //issuer country code 5F2A
        val issuerCountryCode = if (tlvissuerCounTryCode?.isNotEmpty() == true) {
            tlvissuerCounTryCode
        }
        else {
            ""
        }
        Log.d(EmvHandler.TAG,"Card issuer country code ---> $issuerCountryCode")
        cardProcessedDataModal.setCardIssuerCountryCode(issuerCountryCode)
    } catch (ex: Exception) {
        Log.e(EmvHandler.TAG, ex.message ?: "")
    }

    try {
        val tlvAid = emv?.getTLV(Integer.toHexString(0x84))     // AID  TAG //84
        val aidStr = if (tlvAid?.isNotEmpty() == true) {
            val aidstr = tlvAid.take(10)
            aidstr
        }
        else {
            ""
        }
        Log.d(EmvHandler.TAG,"Aid  code ---> $aidStr")
        cardProcessedDataModal.setAID(aidStr)

    } catch (ex: Exception) {
        Log.e(EmvHandler.TAG, ex.message ?: "")
    }

}