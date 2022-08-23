package org.minima.system.commands.backup;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.minima.database.MinimaDB;
import org.minima.database.txpowtree.TxPoWTreeNode;
import org.minima.database.wallet.Wallet;
import org.minima.objects.Greeting;
import org.minima.objects.IBD;
import org.minima.objects.TxBlock;
import org.minima.objects.base.MiniByte;
import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniNumber;
import org.minima.system.Main;
import org.minima.system.commands.Command;
import org.minima.system.commands.CommandException;
import org.minima.system.commands.network.connect;
import org.minima.system.network.minima.NIOClientInfo;
import org.minima.system.network.minima.NIOManager;
import org.minima.system.network.minima.NIOMessage;
import org.minima.utils.BIP39;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;

public class archive extends Command {

	public static final MiniNumber ARCHIVE_DATA_SIZE = new MiniNumber(1024);
	
	public archive() {
		super("archive","[action:resync] (host:) (phrase:)- Resync your chain with seed phrase if necessary (otherwise wallet remains the same)");
	}
	
	@Override
	public JSONObject runCommand() throws Exception {
		JSONObject ret = getJSONReply();
	
		String action = getParam("action");
		
		if(action.equals("resync")) {
			
			//Get the host
			String fullhost = getParam("host");
			Message connectdata = connect.createConnectMessage(fullhost);
			
			String host = connectdata.getString("host");
			int port 	= connectdata.getInteger("port");
			
			//Are we resetting the wallet too ?
			boolean seedphrase 	= false;
			MiniData seed 		= null;
			String phrase = getParam("phrase","");
			if(!phrase.equals("")) {
			
				//reset ALL the default data
				Main.getInstance().archiveResetReady(true);
				
				//This can take soem time..
				MinimaLogger.log("Reseeting all wallet private keys..");
				
				//Convert that into a seed..
				seed = BIP39.convertStringToSeed(phrase);
				
				//Set it..
				MinimaDB.getDB().getUserDB().setBasePrivatePhrase(phrase);
				MinimaDB.getDB().getUserDB().setBasePrivateSeed(seed.to0xString());
				MinimaDB.getDB().getWallet().initBaseSeed(seed);
				
				//Now cycle through all the default wallet keys..
				int tot = Wallet.NUMBER_GETADDRESS_KEYS * 2;
				MinimaLogger.log("Creating a total of "+tot+" keys..");
				for(int i=0;i<tot;i++) {
					MinimaLogger.log("Creating key "+i);
					
					//Create a new key..
					MinimaDB.getDB().getWallet().createNewSimpleAddress(true);
				}
				MinimaLogger.log("All keys created..");
				
			}else {
				//reset ALL the default data
				Main.getInstance().archiveResetReady(false);
			}
			
			//Now cycle through the chain..
			MiniNumber startblock 	= MiniNumber.ZERO;
			MiniNumber endblock 	= MiniNumber.ZERO;
			boolean foundsome = false;
			while(true) {
				
				//Send him a message..
				IBD ibd = sendArchiveReq(host, port, startblock);
			
				int size = ibd.getTxBlocks().size();
				
				if(size > 0) {
					foundsome 		= true;
					TxBlock start 	= ibd.getTxBlocks().get(0);
					TxBlock last 	= ibd.getTxBlocks().get(size-1);
					endblock		= last.getTxPoW().getBlockNumber();
					startblock 		= endblock.increment();
					
					MinimaLogger.log("Archive IBD received start : "+start.getTxPoW().getBlockNumber()+" end : "+endblock);
				}
			
				//Post it..
				Main.getInstance().getTxPoWProcessor().postProcessArchiveIBD(ibd, "0x00");
				
				//Do we have enough to ask again.. 
				if(size<32) {
					break;
				}
			}
			
			//Now wait until all blocks have been processed
			TxPoWTreeNode tip = MinimaDB.getDB().getTxPoWTree().getTip();
			while(foundsome && tip == null) {
				Thread.sleep(1000);
				tip = MinimaDB.getDB().getTxPoWTree().getTip();
			}
			
			//Now wait to catch up..
			while(foundsome) {
				if(!tip.getBlockNumber().isEqual(endblock)) {
					MinimaLogger.log("Waiting for chain to catch up.. please wait");
					Thread.sleep(1000);
				}else {
					break;
				}
				
				tip = MinimaDB.getDB().getTxPoWTree().getTip();
			}
			
			//And NOW shut down..
			Main.getInstance().getTxPoWProcessor().stopMessageProcessor();
			
			//Don't do the usual shutdown hook
			Main.getInstance().setHasShutDown();
			
			//And NOW shut down..
			Main.getInstance().stopMessageProcessor();
			
			//Now shutdown and save everything
			MinimaDB.getDB().saveAllDB();
			
			JSONObject resp = new JSONObject();
			resp.put("message", "Archive sync completed.. shutting down now.. pls restart after");
			resp.put("start", "0");
			resp.put("end", endblock.toString());
			ret.put("response", resp);
		
		}else {
			throw new CommandException("Invalid action : "+action);
		}
		
		return ret;
	}
	
	@Override
	public Command getFunction() {
		return new archive();
	}

	/**
	 * A special PING message to  check a valid connection..
	 */
	public IBD sendArchiveReq(String zHost, int zPort, MiniNumber zStartBlock) {
		
		IBD ibd= null;
		
		try {
			
			//Create the Network Message
			MiniData msg = NIOManager.createNIOMessage(NIOMessage.MSG_ARCHIVE_REQ, zStartBlock);
			
			//Open the socket..
			Socket sock = new Socket();

			//3 seconds to connect
			sock.connect(new InetSocketAddress(zHost, zPort), 3000);
			
			//10 seconds to read
			sock.setSoTimeout(10000);
			
			//Create the streams..
			OutputStream out 		= sock.getOutputStream();
			DataOutputStream dos 	= new DataOutputStream(out);
			
			InputStream in			= sock.getInputStream();
			DataInputStream dis 	= new DataInputStream(in);
			
			//Write the data
			msg.writeDataStream(dos);
			dos.flush();
			
			//Tell the NIO
			Main.getInstance().getNIOManager().getTrafficListener().addWriteBytes(msg.getLength());
			
			//Load the message
			MiniData resp = MiniData.ReadFromStream(dis);
			
			//Tell the NIO
			Main.getInstance().getNIOManager().getTrafficListener().addReadBytes(resp.getLength());
			
			//Close the streams..
			dis.close();
			in.close();
			dos.close();
			out.close();
			
			//Convert
			ByteArrayInputStream bais 	= new ByteArrayInputStream(resp.getBytes());
			DataInputStream bdis 		= new DataInputStream(bais);

			//What Type..
			MiniByte type = MiniByte.ReadFromStream(bdis);
			
			//Load the IBD
			ibd = IBD.ReadFromStream(bdis);
			
			bdis.close();
			bais.close();
		
		}catch(Exception exc){
			MinimaLogger.log(exc);
			
			ibd= null;
		}
		
		return ibd;
	}
}
