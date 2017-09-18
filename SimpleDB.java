import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;


public class SimpleDB {

	public static void main(String[] args) {

		try {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			SimpleDB database = new SimpleDB();
			
			while((line=reader.readLine())!=null) {
				String[] parts = line.split(" ");
				
				if(parts.length==0) {
					System.out.println("Invalid Command");
				}
				
				String commandName = parts[0];
				int arg_length = parts.length;
				switch(commandName) {
				case "SET": 
					if(arg_length!=3) {
						System.out.println("Invalid Command");
					}
					else {
						String varName = parts[1];
						int varValue = Integer.parseInt(parts[2]);
						database.set(varName,varValue);
					}
					break;
					
				case "GET":
					if(arg_length!=2) {
						System.out.println("Invalid Command");
					}
					else {
						String varName = parts[1];
						int retVal = database.get(varName);
						if(retVal<0) {
							System.out.println("NULL");
						}
						else {
							System.out.println(retVal);
						}
					}
					break;
					
				case "UNSET":
					if(arg_length!=2) {
						System.out.println("Invalid Command");
					}
					else {
						String varName = parts[1];
						database.unset(varName);
					}
					break;
					
				case "NUMEQUALTO":
					if(arg_length!=2) {
						System.out.println("Invalid Command");
					}
					else {
						int varValue = Integer.parseInt(parts[1]);
						database.numEqualTo(varValue);
					}
					break;
					
				case "BEGIN":
					database.begin();
					break;
				case "ROLLBACK":
					database.rollback();
					break;
				case "COMMIT":
					database.commit();
					break;
				case "END":
					reader.close();
					return;
				default:
					System.out.println("Invalid Command");
					break;
				}
			}
			
		}
		catch(IOException ex) {
			
		}
		
	}
	
	
	/*
	 * Constructor of Simple Database. 
	 * Creates HashTables to store variable and value counts.
	 * Creates list which will store all transactions. 
	 * */
	public SimpleDB() {
		variables = new HashMap<String,Integer>();
		valueCounts = new HashMap<Integer,Integer>();
		transactions = new ArrayList<Transaction>();
		variableSets = new HashMap<String,TreeSet<Integer>>();
		variableUnSets = new HashMap<String,TreeSet<Integer>>();
	}
	
	public void begin() {
		Transaction trans;
		trans = new Transaction();
		transactions.add(trans);
	}
	
	public void rollback() {
		
		if(transactions.size()==0) {
			System.out.println("NO TRANSACTION");
		}
		else {
			Transaction last = transactions.get(transactions.size()-1);
			for(String var : last.getChangedVariables()) {
				
				if(variableSets.containsKey(var)) {
					variableSets.get(var).remove(transactions.size()-1);
				}
				if(variableUnSets.containsKey(var)) {
					variableUnSets.get(var).remove(transactions.size()-1);
				}
				
				int isPresent = this.get(var);
				boolean isPresentTrans = last.getTransVariables().containsKey(var);
				/*Variable was set earlier but value is change*/
				Integer key = last.getTransVariables().get(var);

				if(isPresent>0 && isPresentTrans) {
					valueCounts.put(key, valueCounts.get(key)-1);
					valueCounts.put(isPresent,valueCounts.get(isPresent)+1);
				}
				/*variable not set earlier but set in this transaction*/
				if(isPresent<0 && isPresentTrans) {
					valueCounts.put(key, valueCounts.get(key)-1);
				}
				/* variable set earlier but unset here*/
				if(isPresent>0 && !isPresentTrans) {
					valueCounts.put(isPresent,valueCounts.get(isPresent)+1);
				}
				
			}
			transactions.remove(transactions.size()-1);
		}
		return;
	}
	
	public void commit() {
		
		if(transactions.size()==0) {
			System.out.println("NO TRANSACTION");
		}
		else {
			for(String var: variableSets.keySet()) {
				if(this.get(var)>0) {
					variables.put(var, this.get(var));
				}
			}
			for(String var: variableUnSets.keySet()) {
				if(this.get(var)<0) {
					variables.remove(var);
				}
			}
			variableSets.clear();
			variableUnSets.clear();
			transactions.clear();
		}
	}
	
	public void set(String var,int val) {

		if(transactions.size()==0) {
			
			if(variables.containsKey(var)) {
				valueCounts.put(variables.get(var),valueCounts.get(variables.get(var))-1);
			}

			if(valueCounts.containsKey(val)) {
				valueCounts.put(val, valueCounts.get(val)+1);
			}
			else {
				valueCounts.put(val, 1);
			}
			variables.put(var, val);
		}
		else {
			Transaction last = transactions.get(transactions.size()-1);
			
			/*
			 * Code to update ValueCount map.
			 * */
			if(this.get(var)>0) {
				int oldVal = this.get(var);
				valueCounts.put(oldVal, valueCounts.get(oldVal)-1);
			}

			if(valueCounts.containsKey(val)) {
				valueCounts.put(val, valueCounts.get(val)+1);
			}
			else {
				valueCounts.put(val, 1);
			}
			
			/* Code to update SET/UNSET trees*/
			
			if(variableSets.containsKey(var)) {
				variableSets.get(var).add(transactions.size()-1);
			}
			else
			{
				TreeSet<Integer> set = new TreeSet<Integer>();
				set.add(transactions.size()-1);
				variableSets.put(var,set);
			}
			last.getTransVariables().put(var,val);
			last.variableChanges.add(var);
		}
	}
	
	public void unset(String var) {

		if(transactions.size()==0) {
			valueCounts.put(variables.get(var), valueCounts.get(variables.get(var))-1);
			variables.remove(var);
		}
		else {
			
			Transaction last = transactions.get(transactions.size()-1);
			
			if(this.get(var)>0) {
				valueCounts.put(this.get(var), valueCounts.get(this.get(var))-1);
			}
			
			if(variableUnSets.containsKey(var)) {
				variableUnSets.get(var).add(transactions.size()-1);
			}
			else {
				TreeSet<Integer> set = new TreeSet<Integer>();
				set.add(transactions.size()-1);
				variableUnSets.put(var,set);
			}
			last.transVariables.remove(var);
			last.variableChanges.add(var);
		}
	}
	
	public int get(String var) {

		Integer retVal = null;

		retVal = variables.get(var);

		if(transactions.size()!=0)  {
			int lastSet = -2;
			int lastUnset= -1;
			if(variableSets.containsKey(var) && !variableSets.get(var).isEmpty()) {
				lastSet = variableSets.get(var).last();
			}
			if(variableUnSets.containsKey(var) && !variableUnSets.get(var).isEmpty()) {
				lastUnset = variableUnSets.get(var).last();
				retVal = null;
			}
			if(lastSet>lastUnset || (lastSet==lastUnset && transactions.get(lastSet).getTransVariables().containsKey(var))) {
				retVal = transactions.get(lastSet).getTransVariables().get(var);
			}
		}
		
		if(retVal==null) {
			return -1;
		}
		else {
		}
		return retVal.intValue();
	}
	
	public void numEqualTo(int value) {

		Integer retVal = new Integer(0);
		retVal = valueCounts.get(value);

		if(retVal ==null) {
			System.out.println("0");
		}
		else {
			System.out.println(retVal.intValue());
		}
	}
	
	/* Inner class Transaction to represent
	 * transactions as a object.
	 * */
	
	class Transaction {
		
		Transaction()
		{
			transVariables = new HashMap<String,Integer>();
			variableChanges = new HashSet<String>();
		}
		
		public HashMap<String,Integer> getTransVariables()
		{
			return transVariables;
		}

		public HashSet<String> getChangedVariables() {
			return variableChanges;
		}
		
		HashMap<String,Integer> transVariables; /*List of variables stored during this transaction*/
		HashSet<String> variableChanges;        /*Set of variables which were SET/UNSET in this transaction*/
	}

	HashMap<String,Integer> variables;     /* Stores the variables defined outside any transaction*/
	HashMap<Integer,Integer> valueCounts;  /* Store value counts of variables */
	ArrayList<Transaction> transactions;   /*List of transactions*/
	HashMap<String,TreeSet<Integer>> variableSets; /*Store transactions which has called SET for variables*/
	HashMap<String,TreeSet<Integer>> variableUnSets; /*Store transactions which has called UNSET for variables*/
	
}
