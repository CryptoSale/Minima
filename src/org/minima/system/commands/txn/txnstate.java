package org.minima.system.commands.txn;

import java.util.ArrayList;
import java.util.Arrays;

import org.minima.database.MinimaDB;
import org.minima.database.userprefs.txndb.TxnDB;
import org.minima.database.userprefs.txndb.TxnRow;
import org.minima.objects.StateVariable;
import org.minima.objects.Transaction;
import org.minima.system.commands.Command;
import org.minima.system.commands.CommandException;
import org.minima.utils.json.JSONObject;

public class txnstate extends Command {

	public txnstate() {
		super("txnstate","[id:] [port:] [value:] - Add a state variable");
	}
	
	@Override
	public ArrayList<String> getValidParams(){
		return new ArrayList<>(Arrays.asList(new String[]{"id","port","value"}));
	}
	
	@Override
	public JSONObject runCommand() throws Exception {
		JSONObject ret = getJSONReply();

		TxnDB db = MinimaDB.getDB().getCustomTxnDB();
		
		//The transaction
		String id 			= getParam("id");
		String port			= getParam("port");
		String value		= getParam("value");
		
		//Get the Transaction
		TxnRow txnrow 	= db.getTransactionRow(getParam("id"));
		if(txnrow == null) {
			throw new CommandException("Transaction not found : "+id);
		}
		Transaction trans = txnrow.getTransaction();
		
		//Create a state variable..
		StateVariable sv = new StateVariable(Integer.parseInt(port),value);
		
		//Add it to the transaction
		trans.addStateVariable(sv);
		
		//Calculate transid
		trans.calculateTransactionID();
				
		//Output the current trans..
		ret.put("response", db.getTransactionRow(id).toJSON());
		
		return ret;
	}

	@Override
	public Command getFunction() {
		return new txnstate();
	}

}
