package cop5556sp17;

import java.util.*;
import java.util.Map.Entry;

import cop5556sp17.AST.Dec;

public class SymbolTable {

	class SymbolTableEntry {
		int scope_number;
		Dec dec;

		public SymbolTableEntry(int scope_number, Dec dec) {
			this.scope_number = scope_number;
			this.dec = dec;
		}
	}

	// TODO add fields

	int current_scope, next_scope;

	Stack<Integer> scope_stack;
	HashMap<String, LinkedList<SymbolTableEntry>> table;

	/**
	 * to be called when block entered
	 */
	public void enterScope() {
		// TODO: IMPLEMENT THIS
		current_scope = next_scope++;
		scope_stack.push(current_scope);
		// System.out.println("Current Scope : "+ current_scope);
	}

	/**
	 * leaves scope
	 */
	public void leaveScope() {
		// TODO: IMPLEMENT THIS
		current_scope = scope_stack.pop();
	}

	public boolean insert(String ident, Dec dec) {
		// TODO: IMPLEMENT THIS
		// get element for ident. retrive the LL and put this entry in front
		// if not exists, add a new entry for this

		if (table.containsKey(ident)) {
			LinkedList<SymbolTableEntry> values = table.get(ident);
			for (SymbolTableEntry val : values) {
				if (val.scope_number == current_scope)
					return false;
			}
			values.addFirst(new SymbolTableEntry(current_scope, dec));
			table.put(ident, values);
		} else {
			LinkedList<SymbolTableEntry> values = new LinkedList<SymbolTableEntry>();
			values.addFirst(new SymbolTableEntry(current_scope, dec));
			table.put(ident, values);
		}

		return true;
	}

	public Dec lookup(String ident) {
		// TODO: IMPLEMENT THIS
		LinkedList<SymbolTableEntry> declarations = table.get(ident);
		if (declarations != null)
			for (SymbolTableEntry st : declarations) {
				if (scope_stack.contains(st.scope_number)) {
					return st.dec;
				}
			}
		return null;
	}

	public SymbolTable() {
		// TODO: IMPLEMENT THIS
		next_scope = 0;
		current_scope = 0;
		scope_stack = new Stack<Integer>();
		table = new HashMap<String, LinkedList<SymbolTableEntry>>();
	}

	@Override
	public String toString() {
		// TODO: IMPLEMENT THIS
		StringBuilder sb = new StringBuilder();
		Set<Entry<String,LinkedList<SymbolTableEntry>>> set = table.entrySet();
		for(Entry<String,LinkedList<SymbolTableEntry>> e : set)
		{
			sb.append("Symbol : " + e.getKey().toString() + " ");
			for(SymbolTableEntry entry : (LinkedList<SymbolTableEntry>)e.getValue())
			{
				sb.append("Scope : " + entry.scope_number + " Type : "+ entry.dec.getTypeName().toString() + " ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
