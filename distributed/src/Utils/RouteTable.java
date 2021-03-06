package Utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

public class RouteTable implements Serializable {
	// 索引值是 `IP:port`
	private Map<String, Map<String, Integer>> table;

	// 16视为不可达
	public static final int INF = 16;

	public RouteTable() {
		table = new HashMap<>();
	}

	public RouteTable(String addr1, String addr2, Integer distance) {
		table = new HashMap<>();
		addVertex(addr1);
		addVertex(addr2);
		table.get(addr1).put(addr2, distance);
		table.get(addr2).put(addr1, distance);
	}

	public Map<String, Map<String, Integer>> getTable() {
		return table;
	}

	private void addVertex(String address) {
		Map<String, Integer> newEntry = new HashMap<>();
		for (String addr : table.keySet()) { // addr与 address之间的距离都设置为INF
			newEntry.put(addr, INF);
			table.get(addr).put(address, INF);
		}
		newEntry.put(address, 0); // 到自身的距离为0
		table.put(address, newEntry);
	}

	public boolean updateTable(RouteTable rt) {
		boolean changed = false;
		Map<String, Map<String, Integer>> otherTable = rt.getTable();
		// 调整表的大小
		for (String key : otherTable.keySet()) {
			if (!table.keySet().contains(key)) {
				addVertex(key);
				changed = true;
			}
		}
		// 更新路径
		for (String addr1 : otherTable.keySet()) {
			for (String addr2 : otherTable.get(addr1).keySet()) {
				Integer distance = otherTable.get(addr1).get(addr2);
				// 有更优的路径
				if (distance < table.get(addr1).get(addr2)) {
					table.get(addr1).put(addr2, distance);
					changed = true;
				}
			}
		}

		// 确认是否为最优距离  floyd算法求出矩阵各节点之间的最短距离   //LS
		Set<String> hosts = table.keySet();
		for (String a : hosts) {
			for (String b : hosts) {
				if (a.equals(b))
					continue;
				for (String c : hosts) {
					if (b.equals(c))
						continue;
					if (table.get(a).get(b) + table.get(b).get(c) < table
							.get(a).get(c)) {
						table.get(a).put(c,
								table.get(a).get(b) + table.get(b).get(c));
						changed = true;
					}
				}
			}
		}

		return changed;
	}

	public String getNextRouteAddress(MsgPacket msgPacket, String currentIp,
			List<HostChannel> connList) {		// 计算得到要到达包的目的IP所需要经过的下一跳路由
		String resIP = "";
		int minDis = Integer.MAX_VALUE;
		Logger.logMsgPacket(msgPacket);
		System.out.println("finding best next hop for msgPacket");
		for (int i = 0; i < connList.size(); i++) { // 寻找距离最短的中继点
			HostChannel neg = connList.get(i);
			String negIp = neg.getIP();
			int disFromHereToNeg = neg.getDistance();
			int disFromNegToDes = table.get(negIp).get(msgPacket.getDesIP());
			int curDis = disFromHereToNeg + disFromNegToDes;
			System.out.println("current distance sum is " + curDis +
					"(" + disFromHereToNeg + " + " + disFromNegToDes + ")");
			if (minDis > curDis) {
				minDis = curDis;
				resIP = negIp;
			}
		}
		System.out.println("best next hop is " + resIP + " with distance of " + minDis);
		return resIP;
	}

	@Override
	public String toString() {
		String str = "\n==================================\n";
		boolean isTableHead = true;
		for (String addr1 : table.keySet()) {
			if (isTableHead) {
				// 插入表头
				isTableHead = false;
				for (String addr2 : table.get(addr1).keySet()) {
					str += "\t" + addr2;
				}
				str += "\n";
			}
			str += addr1 + ":";
			for (String addr2 : table.get(addr1).keySet()) {
				str += "\t" + table.get(addr1).get(addr2);
			}
			str += "\n";
		}
		str += "==================================\n";
		return str;
	}

	public RouteTable deepClone() {
		RouteTable clone = new RouteTable();
		clone.updateTable(this);
		return clone;
	}
}
