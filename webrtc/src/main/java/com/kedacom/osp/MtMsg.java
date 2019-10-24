/**
 * MtMessage消息统一封装
 */
package com.kedacom.osp;

import java.util.Vector;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.kedacom.mt.netmanage.protobuf.MessagePB;
import com.kedacom.mt.netmanage.protobuf.MessagePB.TMsgHeader;



/**
 * @author guoquan
 *
 */
public class MtMsg {

	private String  strMsgId;             ///<消息ID
	private String  strSessionId;         ///<会话ID
	private int     nSequenceId;          ///<序列号		
	private String  strUserData;          ///<用户自定义数据
	private Vector<String>   vctReqPath;  ///<Req消息的接力路径
	private Vector<BodyItem> vctBody;     ///<消息体
	
	public MtMsg(){
		vctReqPath = new Vector<String>();
		vctBody    = new Vector<BodyItem>();
		
		Clear();
	}
	
	public void Clear(){
		this.strMsgId = "";
		this.strSessionId = "";
		this.nSequenceId = 0;
		this.strUserData = "";
		
		if ( null != this.vctReqPath )
		{
			this.vctReqPath.clear();
		}
		
		if ( null != this.vctBody )
		{
			this.vctBody.clear();
		}
	}

	

	public void SetMsgId( String strMsgId ){
		this.strMsgId = strMsgId;
	}
	
	public String GetMsgId(){
		return this.strMsgId;
	}
	
	
	public void SetSessionId( String strSessionId ){
		this.strSessionId = strSessionId;
	}
	
	public String GetSessionId(){
		return this.strSessionId;
	}
	
	public void SetSequenceId( int nSequenceId ){
		this.nSequenceId = nSequenceId;
	}
	
	
	public int GetSequenceId(){
		return this.nSequenceId;
	}
	
	public void SetUserData( String strUserData ){
		this.strUserData = strUserData;
	}
	
	
	public String GetUserData()	{
		return this.strUserData;
	}
	
	
	
	public Vector<String> GetReqPath(){
		return this.vctReqPath;
	}
	
	
	public void SetReqPath( Vector<String> vctReqPath ){
		this.vctReqPath = vctReqPath;
	}
	
	
	public void PushReqPath( String strReqPath ){
		
		if ( strReqPath.isEmpty() )
		{
			if ( !this.strSessionId.isEmpty() )
			{
				this.vctReqPath.add( strSessionId );
			}
		}
		else
		{
			this.vctReqPath.add( strReqPath );
		}
		
	}
	
	public String PopReqPath(){
		if ( vctReqPath.size() == 0 )
		{
			return "";
		}
		
		String strReqPath = vctReqPath.firstElement();
		vctReqPath.remove( 0 );
		
		return strReqPath;		
	}
	
	
	public void AddMsgBody( String strName, byte[] byContent )
	{
		BodyItem bodyItem = new BodyItem( strName, byContent );
		
		vctBody.add( bodyItem );
	}

	/**
	 * Added by gaofan_kd7331, 2019-9-25
	 * */
	public void addMsg(Message msg)
	{
		BodyItem bodyItem = new BodyItem(
				msg.getDescriptorForType().getFullName(),
				msg.toByteArray() );

		vctBody.add( bodyItem );
	}


    public BodyItem GetMsgBody( int nIdx )
	{
		if ( nIdx < vctBody.size())
		{
			return vctBody.elementAt( nIdx );
		}
		else
		{
			return null;
		}		
	}
	
	
	public  byte[] Encode()
	{
		MessagePB.TMessage.Builder   builder     = MessagePB.TMessage.newBuilder();
  	
		//构建消息头
		TMsgHeader.Builder builderHead = TMsgHeader.newBuilder();
		builderHead.setMsgId( this.strMsgId );
		builderHead.setSessionId( this.strSessionId );
		builderHead.setSeqId( this.nSequenceId );
		builderHead.setUserData( this.strUserData );
		
		for ( int i = 0; i < this.vctReqPath.size(); ++i )
		{
			builderHead.addReqPath( this.vctReqPath.elementAt( i ) );
		}
		
		TMsgHeader header  = builderHead.build();
		builder.setHeader( header );		
		 
		//构建消息体
		for ( int i = 0; i < this.vctBody.size(); ++i )
		{
			BodyItem item = this.vctBody.elementAt( i );
			ByteString byteString = ByteString.copyFrom( item.getAbyContent() );
			
			MessagePB.TMsgBody.Builder builderBody = MessagePB.TMsgBody.newBuilder();
			builderBody.setName( item.getStrBodyName() );			
			builderBody.setContent( byteString ); 
			MessagePB.TMsgBody body = builderBody.build();
			
			builder.addBody( i, body );	
		}
		
		
		MessagePB.TMessage   message = builder.build();
		
		int nSerialSize = message.getSerializedSize();
		
	    byte[] byContent = new byte[nSerialSize];
		
		byContent = message.toByteArray();
 
		return byContent;		
	}
 
	
	public boolean Decode( byte[] byContent )
	{
		Clear();
		
		if ( byContent.length == 0 )
		{
			return false;
		}
		
		MessagePB.TMessage.Builder   builder = MessagePB.TMessage.newBuilder();
		
		MessagePB.TMessage message = null;
		try {
			message = MessagePB.TMessage.parseFrom( byContent );
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TMsgHeader msgHeader = message.getHeader();
		
		this.strMsgId      = msgHeader.getMsgId();
		this.strSessionId  = msgHeader.getSessionId();
		this.nSequenceId   = msgHeader.getSeqId();
		this.strUserData   = msgHeader.getUserData();
		
		this.vctReqPath.clear();
		int nReqPathCnt = msgHeader.getReqPathCount();
		
		for( int i = 0; i < nReqPathCnt; ++i )
		{
			this.vctReqPath.add( msgHeader.getReqPath( i ) );			
		}
 
		this.vctBody.clear();
		int nBodyItemCnt = message.getBodyCount();
		
		for( int i = 0; i< nBodyItemCnt; ++i )
		{
			BodyItem bodyItem = new BodyItem();
			bodyItem.setStrBodyName( message.getBody( i ).getName() );
			bodyItem.setAbyContent( message.getBody( i ).getContent().toByteArray() );
			
			this.vctBody.add( bodyItem );
		}
 
		return true;
	}
	
	
	public void copyMsgHead( MtMsg mtMsg )
	{
		SetMsgId( mtMsg.GetMsgId() );
		SetReqPath( mtMsg.GetReqPath() );
		SetSequenceId( mtMsg.GetSequenceId() );
		SetSessionId( mtMsg.GetSessionId());
		SetUserData( mtMsg.GetUserData() );
	}

	public int getBodyCnt()
	{
		return vctBody.size();
	}

	@Override
	public String toString() {
		return "MtMsg [strMsgId=" + strMsgId + ", strSessionId=" + strSessionId
				+ ", nSequenceId=" + nSequenceId + ", strUserData="
				+ strUserData + ", vctReqPath=" + vctReqPath + ", vctBody="
				+ vctBody + "]";
	}
	
}
