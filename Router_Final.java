import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Router {

	static int MAX_SOURCES = 10;
	static int MAX_LANS = 10;
	static int MAX_ROUTERS = 10;

	/* Neighbor Lans of the router */
	ArrayList<Integer> neighborLans = new ArrayList<Integer>();
	ArrayList<routeInfo> routes = new ArrayList<routeInfo>();
	routeInfo[] lanRoutingTable = new routeInfo[MAX_LANS];
	String fileName = null;
	ArrayList<Integer> sources = new ArrayList<Integer>();

	boolean[][] childLanMap = new boolean[10][10];

	/*
	 * multicastTable[i][j] i -> for the source i and router j
	 */

	multiCastTable[][] multicastTable = new multiCastTable[10][10];

	/*
	 * childLanTable[i][j] i -> for the source i and lan j
	 */

	int routerId;

	private void setNeighborLans(String[] args) {
		for (int i = 1; i < args.length; i++) {
			int lanId = Integer.parseInt(args[i]);
			neighborLans.add(lanId);
			lanRoutingTable[lanId].distToLanX = 0;
			lanRoutingTable[lanId].nextHopRIdToLanX = routerId;
			lanRoutingTable[lanId].nextLan = lanId;
		}
	}

	public Router() {
		for (int i = 0; i < 10; i++) {
			lanRoutingTable[i] = new routeInfo(10, 10, 10, true, new ArrayList<Integer>(), 0);
			for (int j = 0; j < 10; j++) {
				multicastTable[i][j] = new multiCastTable(0, false, true);
			}
		}
	}

	private void createLanFiles() throws IOException {

		for (int i = 0; i < neighborLans.size(); i++) {
			File lanX = new File("lan" + neighborLans.get(i) + ".txt");
			lanX.createNewFile();

			/*
			 * Create a copy file to check the differences whenever new content
			 * is written to the lanX file
			 */
			lanX = new File("lanr" + routerId + neighborLans.get(i) + "_copy.txt");
			lanX.createNewFile();

		}
	}

	/* Create routX file for writing outgoing data */
	private void createRoutXFile(String fileName) throws IOException {
		File routX = new File(fileName);
		routX.createNewFile();
	}

	/* Parse the message line by line */
	private void parseMsg(String line) throws IOException {

		String[] msg = line.split(" ");

		if (msg[0].equals("data")) {
			parseDataMsg(line);
		} else if (msg[0].equals("DV")) {
			parseDVmsg(line);
		} else if (msg[0].equals("NMR")) {
			parseNMRmsg(line);
		} else if (msg[0].equals("receiver")) {
			parseReceiverMsg(line);
		}

	}

	private void parseDataMsg(String line) throws IOException {

		String[] data = line.split(" ");
		int inLan = Integer.parseInt(data[1]);
		int hostLanId = Integer.parseInt(data[2]);

		if (lanRoutingTable[hostLanId].nextLan != inLan) {
			return; // If I get a data message from a router other than parent,
					// discard it
		}

		if (!sources.contains(hostLanId))
			sources.add(hostLanId);

		/*
		 * If you have received nmrs from all of your children do not forward
		 * the msg
		 */
		
		int nmr_counter = 0; // counter to check if none of my neighbor lans
								// need the data msg

		/* If inLan = hostLanId, then host is directly attached */
		for (int i = 0; i < neighborLans.size(); i++) {

			int neighborLan = neighborLans.get(i);

			/*
			 * A flag to check if a receiver is present on this lan for this
			 * source
			 */
			boolean flag = false;
			/*
			 * Copy the data message from one interface to others according to
			 * dvmrp
			 */
			if (neighborLan != inLan) {

				if (lanRoutingTable[neighborLan].recvPresent) {
					/*
					 * If I have another router who is not using me a nextHop to
					 * source, it means that router and I are equidistant to
					 * source. If my routerId is greater than that routerId, I
					 * will not put data msg on this lan
					 */
					
					if(lanRoutingTable[neighborLan].neighborRouters.size() == 1) {
						flag = true;
						childLanMap[hostLanId][neighborLan] = true;
						
					} else {
						System.out.println("lanRoutingTable[neighborLan].neigborRouters "+lanRoutingTable[neighborLan].neighborRouters.size());
					}
					for (int r = 0; r < lanRoutingTable[neighborLan].neighborRouters.size(); r++) {

						int neighborRouter = lanRoutingTable[neighborLan].neighborRouters.get(r);

						if (lanRoutingTable[hostLanId].nextHopRIdToLanX != neighborRouter) {
							if (!multicastTable[hostLanId][neighborRouter].isChild && neighborRouter < routerId) {
								/* will be sending nmr */
								childLanMap[hostLanId][neighborLan] = false;
								flag = false;
							} else {
								flag = true;
								childLanMap[hostLanId][neighborLan] = true;
							}
						}
					}
				}

				/* Check if this neighborLan is a child for this source */
				else if (childLanMap[hostLanId][neighborLan] == true) {

					/*
					 * Check in the multicast table if neighborRouter is a child
					 * for this particular source
					 */

					for (int j = 0; j < lanRoutingTable[neighborLan].neighborRouters.size(); j++) {
						int neighborRouter = lanRoutingTable[neighborLan].neighborRouters.get(j);

						/*
						 * Check if an NMR was sent 20 sec ago, if so then
						 * assume no NMR sent by this router
						 */

						if (multicastTable[hostLanId][neighborRouter].nmrTime == 20) {
							multicastTable[hostLanId][neighborRouter].nmrReceived = false;
							multicastTable[hostLanId][neighborRouter].nmrTime = 0;
						} else {
							multicastTable[hostLanId][neighborRouter].nmrReceived = true;
						}

						if (multicastTable[hostLanId][neighborRouter].isChild
								&& !multicastTable[hostLanId][neighborRouter].nmrReceived) {

							/* Set the flag for writing data */
							flag = true;
						}

					}

				}

				/* Write data on this lan if flag is set */
				if (flag) {
					/* Write this message to routX file */

					FileWriter writer;
					writer = new FileWriter(fileName, true);
					writer.write("data " + neighborLan + " " + hostLanId + "\n");
					writer.flush();
					writer.close();
				} else {
					nmr_counter++;
				}
				if (lanRoutingTable[neighborLan].recvPresent) {
					flag = true;
				}
			}

		}

		/*
		 * If none of my neighbor lans need this data msg, send an nmr to parent
		 */
		int numChildLans = 0;
		for (int i = 0; i < childLanMap[hostLanId].length; i++) {
			if (childLanMap[hostLanId][i] == true) {
				numChildLans++; // This will give the number child lans for
								// that
								// source
			}
		}
		System.out.println("nmr counter " + nmr_counter + " num child lans " + numChildLans);
		if (numChildLans == 0 || nmr_counter == numChildLans) {
			sendNMR(hostLanId);
		}

	}

	private void parseDVmsg(String line) {

		String[] dv = line.split(" ");
		int neighborRouter = Integer.parseInt(dv[2]);
		/*
		 * This will be through which other routers can be reached
		 */
		int nextLan = Integer.parseInt(dv[1]);

		for (int i = 3, k = i - 3; i < dv.length; i += 2, k++) {

			/* Add the neighbor router to lanRoutingTable */
			if (!lanRoutingTable[k].neighborRouters.contains(neighborRouter) && neighborRouter != routerId) {
				lanRoutingTable[k].neighborRouters.add(neighborRouter);
			} /*
				 * If the distance to lanX is already present, check for minimum
				 * distance
				 */

			int distance = Integer.parseInt(dv[i]) + 1;
			int nextHopRouterId = Integer.parseInt(dv[i + 1]);
			/*
			 * If i am not the nextHop for this lan, then update table
			 * accordingly
			 */
			if (nextHopRouterId != routerId) {
				/*
				 * If distance to lan is less through the neighborRouter, update
				 * the distance and next hop
				 */
				if (lanRoutingTable[k].distToLanX > distance) {
					lanRoutingTable[k].distToLanX = distance;
					lanRoutingTable[k].nextHopRIdToLanX = neighborRouter;
					lanRoutingTable[k].nextLan = nextLan;

				}

				/* If there is a tie, break the tie with the lower router id */
				if ((lanRoutingTable[k].distToLanX == distance)
						&& (lanRoutingTable[k].nextHopRIdToLanX > neighborRouter)) {
					lanRoutingTable[k].nextHopRIdToLanX = neighborRouter;
					lanRoutingTable[k].nextLan = nextLan;
				}
			}
			multicastTable[k][neighborRouter].isChild = false;
			/* Update child routers */
			if (nextHopRouterId == routerId) {
				/*
				 * If the neighbor router is using me as a next hop to lan k,
				 * update this neighbor router as a child router for this
				 * particular source lan k
				 */
				multicastTable[k][neighborRouter].isChild = true;
				childLanMap[k][nextLan] = true;

			}
		}
	}

	private void parseNMRmsg(String line) throws IOException {

		String[] nmr = line.split(" ");
		int inLan = Integer.parseInt(nmr[1]);
		int nmrRouterId = Integer.parseInt(nmr[2]);
		int hostLanId = Integer.parseInt(nmr[3]);

		if (nmrRouterId == routerId || childLanMap[hostLanId][inLan] == false) {
			return; // Do not parse this message as it sent by yourself
		}

		/*
		 * Update the multicastTable for this source lanId (hostLanId) and
		 * nmrRouterId
		 */
		int nmr_counter = 0;

		multicastTable[hostLanId][nmrRouterId].nmrTime = 0;
		multicastTable[hostLanId][nmrRouterId].nmrReceived = true;

		/* If inLan = hostLanId, then host is directly attached */
		for (int i = 0; i < neighborLans.size(); i++) {

			int neighborLan = neighborLans.get(i);

			/*
			 * A flag to check if a receiver is present on this lan for this
			 * source
			 */
			boolean flag = false;
			/*
			 * Copy the data message from one interface to others according to
			 * dvmrp
			 */
			if (neighborLan != inLan) {

				/* Check if this neighborLan is a child for this source */
				if (childLanMap[hostLanId][neighborLan] == true) {

					/*
					 * Check in the multicast table if neighborRouter is a child
					 * for this particular source
					 */

					for (int j = 0; j < lanRoutingTable[neighborLan].neighborRouters.size(); j++) {
						int neighborRouter = lanRoutingTable[neighborLan].neighborRouters.get(j);

						if (lanRoutingTable[neighborLan].recvPresent) {
							flag = true;
						}

						/*
						 * Check if an NMR was sent 20 sec ago, if so then
						 * assume no NMR sent by this router
						 */

						if (multicastTable[hostLanId][neighborRouter].nmrTime == 20) {
							multicastTable[hostLanId][neighborRouter].nmrReceived = false;
							multicastTable[hostLanId][neighborRouter].nmrTime = 0;
						}

						if (multicastTable[hostLanId][neighborRouter].isChild
								&& !multicastTable[hostLanId][neighborRouter].nmrReceived) {

							/* Set the flag for checking nmrs */
							flag = true;
						}

					}
					/*  */
					if (!flag) {

						nmr_counter++;
					}
				}
			}

		}

		/*
		 * If none of my neighbor lans need this data msg, send an nmr to parent
		 */
		int numChildLans = 0;
		for (int i = 0; i < childLanMap[hostLanId].length; i++) {
			if (childLanMap[hostLanId][i] == true) {
				numChildLans++; // This will give the number child lans for that
								// source
			}
		}

		if (nmr_counter == numChildLans)

		{
			sendNMR(hostLanId);

		}

	}

	private void checkAndSendNMR() throws IOException {

		/* If no sources are registered yet, then need not send NMR */

		if (sources.size() == 0) {
			return;
		}
		int hostLanId;
		/*
		 * Check for all sources, if the corresponding child lans have sent an
		 * NMR, if so send NMR
		 */
		for (int k = 0; k < sources.size(); k++) {
			hostLanId = sources.get(k);

			int nmr_counter = 0;
			int neighborLan = 10;

			/* If inLan = hostLanId, then host is directly attached */
			for (int i = 0; i < neighborLans.size(); i++) {

				neighborLan = neighborLans.get(i);

				/*
				 * A flag to check if a receiver is present on this lan for this
				 * source
				 */
				boolean flag = false;
				/*
				 * Copy the data message from one interface to others according
				 * to dvmrp
				 */

				/* Check if this neighborLan is a child for this source */
				if (childLanMap[hostLanId][neighborLan] == true) {

					/*
					 * Check in the multicast table if neighborRouter is a child
					 * for this particular source
					 */

					for (int j = 0; j < lanRoutingTable[neighborLan].neighborRouters.size(); j++) {
						int neighborRouter = lanRoutingTable[neighborLan].neighborRouters.get(j);

						if (lanRoutingTable[neighborLan].recvPresent) {
							flag = true;
						}

						/*
						 * Check if an NMR was sent 20 sec ago, if so then
						 * assume no NMR sent by this router
						 */

						if (multicastTable[hostLanId][neighborRouter].nmrTime == 20) {
							multicastTable[hostLanId][neighborRouter].nmrReceived = false;
							multicastTable[hostLanId][neighborRouter].nmrTime = 0;
						}

						if (multicastTable[hostLanId][neighborRouter].isChild
								&& !multicastTable[hostLanId][neighborRouter].nmrReceived) {

							/* Set the flag for checking nmrs */
							flag = true;
						} else {

						}

					}
					/*  */
					if (!flag) {

						nmr_counter++;
					}
				}

			}

			int numChildLans = 0;
			for (int i = 0; i < childLanMap[hostLanId].length; i++) {
				if (childLanMap[hostLanId][i] == true) {
					numChildLans++; // This will give the number child lans for
									// that source
				}
			}

			if (numChildLans == 0 || nmr_counter == numChildLans && nmr_counter != 0 )

			{
				sendNMR(hostLanId);

			}

		}

	}

	private void parseReceiverMsg(String line) throws IOException {

		String[] recv = line.split(" ");
		/*
		 * When you receive a recv message, send this message on all lans apart
		 * from where it came
		 */
		int recvLan = Integer.parseInt(recv[1]);

		/* Mark the receiver present bit of this neighbor lan as true */
		lanRoutingTable[recvLan].recvPresent = true;
		lanRoutingTable[recvLan].recvTime = 0;

	}

	private void readLanMessage() throws IOException {

		String line = null;
		/*
		 * Once every second, read message from every lanX and react accordingly
		 */
		for (int i = 0; i < neighborLans.size(); i++) {

			String neighborLan = "lan" + neighborLans.get(i) + ".txt";
			String neighborLan_copy = "lanr" + routerId + neighborLans.get(i) + "_copy.txt";

			/*
			 * Forking a Unix command to check the difference between files and
			 * parsing only those lines
			 */

			String[] cmd = { "/bin/sh", "-c",
					"diff " + neighborLan + " " + neighborLan_copy + " | grep \"<\" | sed \'s/^< //g\'" };

			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			while ((line = stdInput.readLine()) != null) {

				/* Parse each message */
				parseMsg(line);
				FileWriter lanXCopy = new FileWriter(neighborLan_copy, true);
				lanXCopy.write(line + "\n");
				lanXCopy.close();

			}
		}

	}

	private void sendDVmsg() throws IOException {

		/* Construct distance vector message */

		String dv_part = "";
		for (int i = 0; i < MAX_LANS; i++) {
			dv_part += lanRoutingTable[i].distToLanX + " " + lanRoutingTable[i].nextHopRIdToLanX + " ";
		}
		String dv = "";

		/*
		 * lanRoutingTable consists of distance vectors to each Lan. Send them
		 * to each neighboring lan
		 */

		for (int i = 0; i < neighborLans.size(); i++) {
			dv = dv + "DV " + neighborLans.get(i) + " " + routerId + " " + dv_part + "\n";

		}

		FileWriter writer;
		writer = new FileWriter(fileName, true);
		writer.write(dv);
		writer.flush();
		writer.close();
	}

	private void incrementTimer() {

		/*
		 * Iterate through all of the sources , iterate through all of your lans
		 * and the routers to attached to these lans and increment the nmrtime
		 */

		for (int s = 0; s < sources.size(); s++) {
			for (int i = 0; i < neighborLans.size(); i++) {

				int neighborLan = neighborLans.get(i);
				lanRoutingTable[neighborLan].recvTime++;
				if (lanRoutingTable[neighborLan].recvTime == 20) {
					lanRoutingTable[neighborLan].recvPresent = false;
					lanRoutingTable[neighborLan].recvTime = 0;
				}
				/* Iterate through all neighbor routers present in this lan */
				for (int j = 0; j < lanRoutingTable[neighborLan].neighborRouters.size(); j++) {
					int neighborRouter = lanRoutingTable[neighborLan].neighborRouters.get(j);

					multicastTable[s][neighborRouter].nmrTime++;

				}
			}
		}
	}

	private void removeCopyFiles() {

		for (int i = 0; i < neighborLans.size(); i++) {
			File f = new File("lanr" + routerId + neighborLans.get(i) + "_copy.txt");
			f.delete();
		}

	}

	private void sendNMR(int hostLanId) throws IOException {

		/* send an nmr to this router */

		FileWriter writer;
		writer = new FileWriter(fileName, true);
		writer.write("NMR " + lanRoutingTable[hostLanId].nextLan + " " + routerId + " " + hostLanId + "\n");
		writer.flush();
		writer.close();
	}

	public static void main(String args[]) throws IOException, InterruptedException {

		Router r = new Router();
		r.routerId = Integer.parseInt(args[0]); // This is the routerId

		/* Set the neighboring LANs of the router */
		r.setNeighborLans(args);

		/* Create neighboring lanX files */
		r.createLanFiles();

		/* routX file name */
		r.fileName = "rout" + r.routerId + ".txt";

		/* Create routX file for writing outgoing data */
		r.createRoutXFile(r.fileName);

		/* Routers run for 100 seconds */
		for (int time = 0; time < 100; time++) {

			r.readLanMessage();

			if (time % 5 == 0) {
				r.sendDVmsg();
			}

			if (time % 10 == 0) {
				r.checkAndSendNMR();
			}

			/* Increment receiver time */

			/* Increment NMRs */
			r.incrementTimer();
			Thread.sleep(1000);
		}

		/* Remove all the copy files */
		r.removeCopyFiles();

	}

}

class routeInfo {

	int nextHopRIdToLanX;
	int distToLanX;
	int nextLan;
	boolean recvPresent;
	ArrayList<Integer> neighborRouters;
	int recvTime;

	public routeInfo(int nextHopRIdToLanX, int distToLanX, int nextLan, boolean recvPresent,
			ArrayList<Integer> neighborRouters, int recvTime) {

		this.nextHopRIdToLanX = nextHopRIdToLanX;
		this.distToLanX = distToLanX;
		this.nextLan = nextLan;
		this.recvPresent = recvPresent;
		this.neighborRouters = neighborRouters;
		this.recvTime = recvTime;
	}
}

class multiCastTable {

	int nmrTime;
	boolean nmrReceived;
	boolean isChild;

	public multiCastTable(int nmrTime, boolean nmrReceived, boolean isChild) {

		this.nmrTime = nmrTime;
		this.nmrReceived = nmrReceived;
		this.isChild = isChild; // If the neighbor router is a child to you

	}
}
