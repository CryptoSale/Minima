package org.minima.system.commands.base;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;

import org.minima.database.MinimaDB;
import org.minima.database.archive.ArchiveManager;
import org.minima.objects.TxBlock;
import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniNumber;
import org.minima.system.commands.Command;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;

public class test extends Command {

	public test() {
		super("test","test Funxtion");
	}
	
	@Override
	public JSONObject runCommand() throws Exception {
		JSONObject ret = getJSONReply();
	
		//Get the Archive DB
		ArchiveManager arch = MinimaDB.getDB().getArchive();
		
		arch.hackShut();
		
		ret.put("response", "Archive DB Closed for test");
	
		return ret;
	}
	
	@Override
	public Command getFunction() {
		return new test();
	}

	public static void main(String[] zArgs) {
		
		for(int i=0;i<512;i++) {
			
			MiniData data = new MiniData(new BigInteger(Integer.toString(i)));
			
			System.out.println(data.to0xString());
			
		}
		
		
		
	}
}
