package org.minima.system.commands.txn;

import java.util.ArrayList;

import org.minima.database.MinimaDB;
import org.minima.database.userprefs.txndb.TxnDB;
import org.minima.database.userprefs.txndb.TxnRow;
import org.minima.database.wallet.KeyRow;
import org.minima.database.wallet.Wallet;
import org.minima.objects.Coin;
import org.minima.objects.Transaction;
import org.minima.objects.Witness;
import org.minima.objects.base.MiniData;
import org.minima.objects.keys.Signature;
import org.minima.system.commands.Command;
import org.minima.system.commands.CommandException;
import org.minima.utils.Crypto;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;

public class txnsign extends Command {

	public txnsign() {
		super("txnsign","[id:] [publickey:0x..|auto] - Sign a transaction");
	}
	
	@Override
	public JSONObject runCommand() throws Exception {
		JSONObject ret = getJSONReply();

		TxnDB db = MinimaDB.getDB().getCustomTxnDB();
		
		String id 	= getParam("id");
		String pubk	= getParam("publickey");
		
		//Get the Transaction..
		TxnRow txnrow 	= db.getTransactionRow(getParam("id"));
		if(txnrow == null) {
			throw new CommandException("Transaction not found : "+id);
		}
		
		Transaction txn = txnrow.getTransaction();
		Witness wit		= txnrow.getWitness();
		
		//Calculate the TransactionID..
		MiniData transid = Crypto.getInstance().hashObject(txn);
	
		//Get the Wallet
		Wallet walletdb = MinimaDB.getDB().getWallet();
		
		//Which keys did we find..
		JSONArray foundkeys = new JSONArray();
		
		//Are we auto signing.. if all the coin inputs are simple
		if(pubk.equals("auto")) {
			
			int sigs = 0;
			ArrayList<Coin> inputs = txn.getAllInputs();
			for(Coin cc : inputs) {
				
				
				KeyRow keyrow = walletdb.getKeysRowFromAddress(cc.getAddress().to0xString()); 
				if(keyrow == null) {
//					txnrow.clearWitness();
//					throw new CommandException("ERROR : Script not found for address : "+cc.getAddress().to0xString());
					continue;
					
					//Is it a simple row..
				}else if(keyrow.getPublicKey().equals("")) {
//					txnrow.clearWitness();
//					throw new CommandException("NON-Simple coin found at coin : "+cc.getAddress().to0xString());
					continue;
				}
				
				//Add to our list..
				foundkeys.add(keyrow.getPublicKey());
				
				//Now sign with that..
				Signature signature = walletdb.sign(keyrow.getPrivateKey(), transid);
					
				//Add it..
				wit.addSignature(signature);
			}
			
		}else {
			//Get the Private key..
			KeyRow pubrow 	= walletdb.getKeysRowFromPublicKey(pubk);
			if(pubrow == null) {
				throw new CommandException("Public Key not found : "+pubk);
			}
			
			//Add to our list..
			foundkeys.add(pubk);
			
			//Use the wallet..
			Signature signature = walletdb.sign(pubrow.getPrivateKey(), transid);
				
			//Add it..
			wit.addSignature(signature);
		}
		
		JSONObject resp = new JSONObject();
		resp.put("keys", foundkeys);
		ret.put("response", resp);
		
		return ret;
	}

	@Override
	public Command getFunction() {
		return new txnsign();
	}

}
