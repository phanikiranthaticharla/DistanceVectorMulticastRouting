import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class Host {

	String HostId;
	String LanId;

	private void createHostFiles() throws IOException {
		File houtX = new File("hout" + HostId + ".txt");
		File hinX = new File("hin" + HostId + ".txt");
		File hinCopy = new File("hin" + HostId + "_copy.txt");
		File lanXCopy = new File("lanh"+ HostId + "_copy.txt");
		houtX.createNewFile();
		hinX.createNewFile();
		hinCopy.createNewFile();
		lanXCopy.createNewFile();

	}

	private void readHostMsg() throws IOException {

		String line = null;

		String lanX = "lan" + LanId + ".txt";
		String lanX_copy = "lanh" + HostId + "_copy.txt";

		/*
		 * Forking a Unix command to check the difference between files and
		 * parsing only those lines
		 */

		String[] cmd = { "/bin/sh", "-c", "diff " + lanX + " " + lanX_copy + " | grep \"<\" | sed \'s/^< //g\'" };

		Process p = Runtime.getRuntime().exec(cmd);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		while ((line = stdInput.readLine()) != null) {


			/* Parse each message */
			parseHostMsg(line);

			/* Append this line to copy file so that its not read again! */
			FileWriter lanX_Copy = new FileWriter(lanX_copy, true);
			lanX_Copy.write(line + "\n");
			lanX_Copy.close();

		}

	}

	private void parseHostMsg(String line) throws IOException {

		String[] data = line.split(" ");
		if (data[0].equals("data")) {
			/* copy the message to the hin file */
			FileWriter hinX = new FileWriter("hin" + HostId + ".txt", true);
			hinX.write(line + "\n");
			hinX.close();
		}

	}

	/* Remove the copy files */
	private void removeCopyFiles() {

		File f = new File("hin" + HostId + "_copy.txt");
		f.delete();
		f = new File("lanh" +HostId + "_copy.txt");
		f.delete();
	}

	private void sendDataMsg() throws IOException {

		FileWriter writer;

		writer = new FileWriter("hout" + HostId + ".txt", true);
		writer.write("data " + LanId + " " + LanId+"\n");
		writer.flush();
		writer.close();

	}

	private void sendReceiveMsg() throws IOException {

		FileWriter writer;

		writer = new FileWriter("hout" + HostId + ".txt", true);
		writer.write("receiver " + LanId+"\n");
		writer.flush();
		writer.close();
		
	}

	public static void main(String args[]) throws IOException, InterruptedException {

		Host h = new Host();

		h.HostId = args[0];
		h.LanId = args[1];
		String type = args[2];
		int sendTime = 0;

		h.createHostFiles();
		int period = 0;

		if (args.length > 3) {
			sendTime = Integer.parseInt(args[3]);
			period = Integer.parseInt(args[4]);
		}

		for (int time = 0; time < 100; time++) {

			/* Once every second check if the host received a message */

			if (type.equals("receiver")) {

				if (time % 10 == 0) {
					h.sendReceiveMsg();
				}
				h.readHostMsg();
			} else {

				if (time == sendTime || (time > sendTime) && (time % period == 0)) {
					h.sendDataMsg();
				}

			}
			Thread.sleep(1000);
		}

		/* Remove all the copy files */
		h.removeCopyFiles();
	}

}
