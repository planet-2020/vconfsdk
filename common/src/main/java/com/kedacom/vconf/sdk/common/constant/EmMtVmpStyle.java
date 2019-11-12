package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/** 画面合成风格定义 */

@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtVmpStyle
{
	emInvalid_Style_Api ,            ///<非法值
	emMt_VMP_STYLE_DYNAMIC_Api ,		///<动态分屏(仅自动合成时有效)
	emMt_VMP_STYLE_1_Api ,			///<一画面全屏
	emMt_VMP_STYLE_2_1X2_Api ,		///<两画面: 2等大，居中(1行2列)
	emMt_VMP_STYLE_2_B1_S1RD_Api ,	///<两画面: 1大1小，1大全屏，1小右下
	emMt_VMP_STYLE_3_1U_2D1X2_Api ,	///<三画面: 等大，1上，2下(1行2列)
	emMt_VMP_STYLE_3_B1L_S2R2X1_Api ,	///<三画面: 1大2小，1大左，2小右(2行1列)
	emMt_VMP_STYLE_3_1L_2R2X1_Api ,	    ///<三画面: 等大，1左，2右(2行1列)
	emMt_VMP_STYLE_3_B1_S2LD1X2_Api ,	///<三画面: 1大2小，1大全屏，2小左下(1行2列)
	emMt_VMP_STYLE_3_3X1_Api ,		    ///<三画面: 等大，3行1列
	emMt_VMP_STYLE_4_2X2_Api ,			///<四画面: 等大，2行2列
	emMt_VMP_STYLE_4_B1L_S3R3X1_Api ,	///<四画面: 1大3小，1大左，3小右(3行1列)
	emMt_VMP_STYLE_4_B1U_S3D1X3_Api,	///<四画面: 1大3小，1大上，3小下(1行3列)
	emMt_VMP_STYLE_5_B1L_S4R4X1_Api,	///<五画面: 1大4小，1大左，4小右(4行1列)
	emMt_VMP_STYLE_5_B1U_S4D1X4_Api,	///<五画面: 1大4小，1大上，4小下(1行4列)
	emMt_VMP_STYLE_5_B2U1X2_S3D1X3_Api,	    ///<五画面: 2大3小，2大上(1行2列)，3小下(1行3列)
	emMt_VMP_STYLE_6_B1LU_S2RU2X1_S3D1X3_Api ,	///<六画面: 1大5小，1大左上，2小右上(2行1列)，3小下(1行3列)
	emMt_VMP_STYLE_6_B2U1X2_S4D1X4_Api,	    ///<六画面: 2大4小，2大上(1行2列), 4小下(1行4列)
	emMt_VMP_STYLE_6_2X3_Api ,					///<六画面: 等大，2行3列
	emMt_VMP_STYLE_6_B1U_S5D1X5_Api ,			///<六画面: 1大5小，1大上，5小下(1行5列)
	emMt_VMP_STYLE_7_B2U1X2_B1LD_S4RD2X2_Api ,			///<七画面: 3大4小，2大上(1行2列)，1大左下，4小右下(2行2列)
	emMt_VMP_STYLE_7_S2LU2X1_B1MU_S2RU2X1_B2D1X2_Api ,	///<七画面: 3大4小，2小左上(2行1列)，1大中上，2小右上(2行1列)，2大下(1行2列)
	emMt_VMP_STYLE_7_B1U_S6D1X6_Api ,					///<七画面: 1大6小，1大上，6小下(1行6列)
	emMt_VMP_STYLE_8_B1LU_S3RU3X1_S4D1X4_Api,			///<八画面: 1大7小，1大左上，3小右上(3行1列)，4小下(1行4列)
	emMt_VMP_STYLE_8_B4L2X2_S4R4X1_Api ,			    ///<八画面: 4大4小，4大左(2行2列)，4小右(4行1列)
	emMt_VMP_STYLE_9_3X3_Api ,					        ///<九画面: 等大，3行3列
	emMt_VMP_STYLE_9_S4U1X4_B1M_S4D1X4_Api ,		    ///<九画面: 1大8小，4小上(1行4列)，1大中，4小下(1行4列)
	emMt_VMP_STYLE_10_B2L2X1_S8R4X2_Api ,		        ///<十画面: 2大8小，2大左(2行1列)，8小右(4行2列)
	emMt_VMP_STYLE_10_B2U1X2_S8D2X4_Api ,		        ///<十画面: 2大8小，2大上(1行2列)，8小下(2行4列)
	emMt_VMP_STYLE_10_S4U1X4_B2M1X2_S4D1X4_Api ,	    ///<十画面: 2大8小，4小上(1行4列)，2大中(1行2列)，4小下(1行4列)
	emMt_VMP_STYLE_10_S4L4X1_B2M2X1_S4R4X1_Api ,	    ///<十画面: 2大8小，4小左(4行1列)，2大中(2行1列)，4小右(4行1列)
	emMt_VMP_STYLE_11_S5U1X5_B1M_S5D1X5_Api ,	///<十一画面: 1大10小，5小上(1行5列)，大1中，5小下(1行5列)
	emMt_VMP_STYLE_11_B1U_S10D2X5_Api ,			///<十一画面: 1大10小，1大上，10小下(2行5列)
	emMt_VMP_STYLE_12_B2U1X2_B1LD_S9RD3X3_Api ,	///<十二画面: 3大9小，2大上(1行2列)，1大左下，9小右下(3行3列)
	emMt_VMP_STYLE_12_B1LU_S5RU4X1_S6D1X6_Api ,	///<十二画面: 1大11小，大1左上，5小右上(4行1列)，5小下(1行5列)
	emMt_VMP_STYLE_13_B1LU_S4RU2X2_S8D2X4_Api ,	///<十三画面: 1大12小，大1左上，4小右上(2行2列)，8小下(2行4列)
	emMt_VMP_STYLE_13_S4U1X4_S2LM2X1_B1MM_S2LM2X1_S4D1X4_Api ,
	///<十三画面: 1大12小，4小上(1行4列)，2小左中(2行1列)，1大中中，2小右中(2行1列), 4小下(1行4列)

	emMt_VMP_STYLE_13_B4LU2X2_S4RU4X1_S5D1X5_Api ,				///<十三画面: 4大9小，4大左上(2行2列)，4小右上(4行1列)，5小下(1行5列)
	emMt_VMP_STYLE_14_B2LU1X2_S2RU2X1_S10D2X5_Api ,				///<十四画面: 2大12小，2大左上(1行2列)，2小右上(2行1列)，10小下(2行5列)
	emMt_VMP_STYLE_14_S5U1X5_B1LM_S2MM2X1_B1RM_S5D1X5_Api ,		///<十四画面: 2大12小，5小上(1行5列)，1大左中，2小中中(2行1列)，1大右中，5小下(1行5列)
	emMt_VMP_STYLE_15_B3U1X3_S12D2X6_Api ,						///<十五画面: 3大12小，3大上(1行3列)，12小下(2行6列)
	emMt_VMP_STYLE_15_S4U1X4_S3LM3X1_B1MM_S3RM3X1_S4D1X4_Api ,
	///<十五画面: 1大14小，4小上(1行4列)，3小左中(3行1列)，1大中中，3小右中(3行1列)，4小下(1行4列)
	
	emMt_VMP_STYLE_16_4X4_Api ,									///<十六画面: 16等分，4x4
	emMt_VMP_STYLE_16_B1LU_S7RU7X1_S8D1X8_Api ,					///<十六画面: 1大15小，1大左上，7小右上(7行1列)，8小下(1行8列)
	emMt_VMP_STYLE_17_S5U1X5_S3LM3X1_B1MM_S3RM3X1_S5D1X5_Api ,
	///<十七画面: 1大16小，5小上(1行5列)，3小左中(3行1列)，1大中中，3小右中(3行1列)，5小下(1行5列)
    
	emMt_VMP_STYLE_17_B1LU_S6RU3X2_S10D2X5_Api ,	    ///<十七画面: 1大16小，1大左上，6小右上(3行2列)，10小下(2行5列)
	emMt_VMP_STYLE_17_B2U1X2_S15D3X5_Api ,			    ///<十七画面: 2大15小，2大上(1行2列)，15小下(5列3行)
	emMt_VMP_STYLE_18_S6U1X6_B6M2X3_S6D1X6_Api ,		///<十八画面: 6大12小，6小上(1行6列)，6大居中(2行3列)，6小下(1行6列)
	emMt_VMP_STYLE_18_B6U2X3_S12D2X6_Api ,			    ///<十八画面: 6大12小，6大上(2行3列)，12小下(2行6列)
	emMt_VMP_STYLE_18_S6L6X1_B6M3X2_S6R6X1_Api ,		///<十八画面: 6大12小，6小左(6行1列)，6大中(3行2列)，6小右(6行1列)
	emMt_VMP_STYLE_19_B2LU1X2_S2RU2X1_S15D3X5_Api ,   	///<十九画面: 2大17小，2大左上(1行2列)，2小右上(2行1列)，15小下(3行5列)
	emMt_VMP_STYLE_19_B2LU2X1_S12RU4X3_S5D1X5_Api ,	    ///<十九画面: 2大17小，2大左上(2行1列)，12小右上(4行3列)，5小下(1行5列)
	emMt_VMP_STYLE_20_B2U1X2_S18D3X6_Api ,			    ///<二十画面: 2大18小，2大上(1行2列)，18小下(3行6列)
	emMt_VMP_STYLE_20_S6U1X6_B2M1X2_S12D2X6_Api ,	    ///<二十画面: 2大18小，6小上(1行6列)，2大中(1行2列)，12小下(2行6列)
	emMt_VMP_STYLE_21_S6U1X6_S4LM4X1_B1MM_S4RM4X1_S6D1X6_Api ,
	///<二十一画面: 1大20小，6小上(1行6列)，4小左中(4行1列)，1大中中，4小右中(4行1列)，6小下(1行6列)

	emMt_VMP_STYLE_21_B1LU_S8RU4X2_S12D2X6_Api ,				///<二十一画面: 1大20小，1大左上，8小右上(4行2列)，12小下(2行6列)
	emMt_VMP_STYLE_22_B1LU_S6RU2X3_S15D3X5_Api ,			    ///<二十二画面: 1大21小，1大左上，6小右上(2行3列)，15小下(3行5列)
	emMt_VMP_STYLE_23_4U1X4_15M3X5_4D1X4_Api ,					///<二十三画面: 23同大，4上(1行4列)，15中(3行5列)，4下(1行4列)
	emMt_VMP_STYLE_23_B2U1X2_S4LM2X2_B1MM_S4RM2X2_S12D2X6_Api ,
	///<二十三画面: 3大20小，2大上(1行2列)，4小左中(2行2列)，1大中中，4小右中(2行2列)，12小下(2行6列)
	
	emMt_VMP_STYLE_24_S6U1X6_S4LM4X1_B4MM2X2_S4RM4X1_S6D1X6_Api ,
	///<二十四画面: 4大20小，6小上(1行6列)，4小左中(4行1列)，4大中中(2行2列)，4小右中(4行1列) 下(1行6列)
	emMt_VMP_STYLE_25_5X5_Api ,									///<二十五画面: 等大，5行5列
	emMt_VMP_STYLE_25_S3LU3X1_B1MU_S3RU3X1_S18D3X6_Api ,
	///<二十五画面: 1大24小，3小左上(3行1列)，1大中上，3小右上(3行1列)，18小下(3行6列)
	emMt_VMP_STYLE_2_B1_S1RU_Api ,						//两画面: 1大1小，1大全屏，1小右上
	emMt_VMP_STYLE_2_B1_S1LD_Api,						// 两画面: 1大1小，1大全屏，1小左下
	emMt_VMP_STYLE_2_B1_S1L_Api ,						// 两画面: 1大1小，1大全屏，1小左上

//	emMt_VMP_STYLE_20_5X4_Api = 100,                                 ///<二十画面，等分5*4， 兼容老的行业平台，5.0平台没有
}