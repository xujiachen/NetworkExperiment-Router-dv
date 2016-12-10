import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class RouterTable {
	public List<Record> table;
	public RouterTable() {
		table = new ArrayList<>();
	}
	public RouterTable(List<Record> table) {
		this.table = table;
	}

	public void update(String ip, String rt) {
		String[] t = rt.split("#");
		List<Record> table_recieved = new ArrayList<>();

		for (int i = 1; i < t.length; i++) {
			String[] record = t[i].split(" ");
			int cost = Integer.parseInt(record[1]);
			table_recieved.add(new Record(record[0], ip, cost, 0));
			//System.out.println("222" + cost + "\n");
		}

		for (Record r_recieved : table_recieved) {
			if (getCost(r_recieved.dst) == -1) table.add(new Record(r_recieved.dst, ip, 1 + r_recieved.cost, 0));
			//System.out.println("111" + ip + "\n");
		}

		int indirect_cost = getCost(ip);
		int direct_cost;
		boolean flag;
		int index = 0;

		Iterator i = table.iterator();
		i.next();
		while (i.hasNext()) {
			int count = 0;
			Record r = (Record)i.next();

			for (Record r_recieved : table_recieved) {
				if (r_recieved.dst.equals(r.dst)) {
					if (r_recieved.cost+indirect_cost < r.cost) {
						r.next = r_recieved.next;
						r.cost = r_recieved.cost;
						r.count = 0;
					}
				} else if (r_recieved.next.equals(r.next)) count++;
			}

			if (count == table_recieved.size()) {
				r.count++;
				if (r.count >= 3) i.remove();
			}
		}

		/*
		for (Iterator i = table.iterator(); i.hasNext(); ) {
			for (Record r_recieved : table_recieved) {
				if (ip.equals(r_recieved.dst)) {
					flag = false;
					r.cost = 1;
					r.next = ip;
					r.count = 0;
					break;
				}

				if (r.dst.equals(r_recieved.dst)) {
					direct_cost = getCost(r.dst);
					flag = false;

					if (direct_cost > indirect_cost + r_recieved.cost) {
						r.cost = indirect_cost + r_recieved.cost;
						r.next = ip;
						r.count = 0;
						break;
					}
				}

				if (flag && r.next.equals(ip) && !r.dst.equals(r.next)) r.count++;
				if (r.count >= 3) i.remove();
			}
		}
		
		*/
	}

	public int getCost(String ip) {
		for (Record r : table) {
			if (r.dst.equals(ip))
				return r.cost;
		}
		return -1;
	}
	public String getShortcut(String ip) {
		for (Record r : table) {
			if (r.dst.equals(ip))
				return r.next;
		}
		return "invalid";
	}
	public void clear() {
		table.clear();
	}
	public void removeRecord(String ip) {
		for (Iterator i = table.iterator(); i.hasNext(); ) {
			Record r = (Record)i.next();
			if (r.next.equals(ip))
				i.remove();
		}
		
	}
	public static void main(String[] args) {
		List<Record> list = new ArrayList<>();
		list.add(new Record("0:0:0:0", "0:0:0:0", 1, 0));
		String ip = "0:0:0:0";
		String s1 = "r#1:1:1:1?6";
		String s2 = "r#1:1:1:1?2";
		String s3 = "r#2:2:2:2?7";
		String s4 = "r#1:1:1:1?8";
		RouterTable table = new RouterTable(list);
		table.update(ip, s1);
		for (Record r : table.table)
			System.out.println(r.toString());
		System.out.println("");
		table.update(ip, s2);
		table.update(ip, s4);
		for (Record r : table.table)
			System.out.println(r.toString());
		System.out.println("");
		table.update(ip, s3);
		table.update(ip, s3);
		table.update(ip, s3);
		table.update(ip, s3);
		for (Record r : table.table)
			System.out.println(r.toString());
		System.out.println("");
		table.removeRecord("0:0:0:0");
		for (Record r : table.table)
			System.out.println(r.toString());
	}
}
