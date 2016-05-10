import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Controller {

	ArrayList<String> Hosts = new ArrayList<String>();
	ArrayList<String> Routers = new ArrayList<String>();
	ArrayList<String> Lans = new ArrayList<String>();

	private void setLists(String[] args) {

		int i = 1;
		
		while (!args[i].equals("router")) {
			Hosts.add(args[i]);
			i++;
		}
		i++;
		while (!args[i].equals("lan")) {
			Routers.add(args[i]);
			i++;
		}
		i++;
		while (i < args.length) {
			Lans.add(args[i]);
			i++;
		}

		return;
	}

	private void readRouterMsg() throws IOException {

		String line = null;

		for (int i = 0; i < Routers.size(); i++) {

			String routerId = Routers.get(i);	
			String routerX = "rout" + routerId + ".txt";
			String routerX_copy = "routc" + routerId + "_copy.txt";

			/*
			 * Forking a Unix command to check the difference between files and
			 * parsing only those lines
			 */

			String[] cmd = { "/bin/sh", "-c",
					"diff " + routerX + " " + routerX_copy + " | grep \"<\" | sed \'s/^< //g\'" };

			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			while ((line = stdInput.readLine()) != null) {

				/* Parse each message */
				parseRouterMsg(line);

				/* Append this line to copy file so that its not read again! */
				FileWriter routerXCopy = new FileWriter(routerX_copy, true);
				routerXCopy.write(line + "\n");
				routerXCopy.close();

			}
		}

	}

	private void parseRouterMsg(String line) throws IOException {

		String[] routerMsg = line.split(" ");

		/* Copy this message to corresponding lan */
		FileWriter writer;
		writer = new FileWriter("lan" + routerMsg[1] + ".txt", true);
		writer.write(line+"\n");
		writer.flush();
		writer.close();

	}

	private void readHostMsg() throws IOException {

		String line = null;

		for (int i = 0; i < Hosts.size(); i++) {

			String hostId = Hosts.get(i);
			String hostX = "hout" + hostId + ".txt";
			String hostX_copy = "houtc" + hostId + "_copy.txt";

			/*
			 * Forking a Unix command to check the difference between files and
			 * parsing only those lines
			 */

			String[] cmd = { "/bin/sh", "-c", "diff " + hostX + " " + hostX_copy + " | grep \"<\" | sed \'s/^< //g\'" };

			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			while ((line = stdInput.readLine()) != null) {

				/* Parse each message */
				parseHostMsg(line);

				/* Append this line to copy file so that its not read again! */
				FileWriter hostXCopy = new FileWriter(hostX_copy, true);
				hostXCopy.write(line + "\n");
				hostXCopy.close();

			}
		}

	}

	private void parseHostMsg(String line) throws IOException {

		String[] hostMsg = line.split(" ");

		/* Copy this message to corresponding lan */
		FileWriter writer;
		writer = new FileWriter("lan" + hostMsg[1] + ".txt", true);
		writer.write(line+"\n");
		writer.flush();
		writer.close();

	}

	private void createCopyFiles() throws IOException {
	
		/*
		 * Create a copy file to check the differences whenever new content
		 * is written to the routX file
		 */
		for (int i = 0; i < Routers.size() ; i++) {
			
			
			File routXCopy = new File("routc" + Routers.get(i) + "_copy.txt");
			routXCopy.createNewFile();
			
		}
		
		for (int i = 0; i < Hosts.size() ; i++) {
	
			File houtXCopy = new File("houtc" + Hosts.get(i) + "_copy.txt");
			houtXCopy.createNewFile();
			
		}
		
	}
	
	private void removeCopyFiles() {
		
		
		for (int i = 0; i < Routers.size(); i++) {
			File f = new File("routc" + Routers.get(i) + "_copy.txt");
			f.delete();
		}

		for (int i = 0; i < Hosts.size(); i++) {
			File f = new File("houtc" + Hosts.get(i) + "_copy.txt");
			f.delete();

		}
		
	}
	
	public static void main(String args[]) throws IOException, InterruptedException {

		Controller c = new Controller();
		
		c.setLists(args);
		/* Routers run for 100 seconds */
		c.createCopyFiles();
		for (int time = 0; time < 100; time++) {

			/*
			 * Once every second check the out files of each Router and copy it
			 * to the respective LAN
			 */
			c.readRouterMsg();
			c.readHostMsg();
			Thread.sleep(1000); 
		}
		c.removeCopyFiles();
	}

	

}
