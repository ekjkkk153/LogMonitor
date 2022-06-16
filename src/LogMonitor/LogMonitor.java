package LogMonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;

import org.ini4j.Ini;
import org.ini4j.Wini;

public class LogMonitor {
	public static String config = "config.ini";
	public static File conf;

	static FileInputStream fis;
	static FileOutputStream fos;

	static OperatingSystemMXBean osBean;

	static Ini inif;

	static int cpuLimit = 60;
	static int memLimit = 60;
	static String[] checkStr;

	public static void loadIni() {
		try {
//			System.out.println("Loading Ini...\r\n");

			conf = new File(config);
			inif = new Ini(conf);

			if (inif.get("UsageMonitor", "CpuLimit") != null)
				cpuLimit = Integer.parseInt(inif.get("UsageMonitor", "CpuLimit"));
//			else 
//				System.out.println("Cannot find option 'CpuLimit'. Setting default value(60)...");

			if (inif.get("UsageMonitor", "MemLimit") != null)
				memLimit = Integer.parseInt(inif.get("UsageMonitor", "MemLimit"));
//			else 
//				System.out.println("Cannot find option 'MemLimit'. Setting default value(60)...");

			if (inif.get("UsageMonitor", "checkStr") != null) {
				checkStr = inif.get("UsageMonitor", "checkStr").replaceAll(" ","").split(",");
			} else
				checkStr = "".split(",");

//			System.out.println("Inserted CPU Limit : " + cpuLimit);
//			System.out.println("Inserted Mem Limit : " + memLimit);
//			System.out.print("Inserted checkStr : ");
//			for(String s : checkStr) System.out.print(s + ",");
//			System.out.println();
			
		} catch (IOException e) {
//			System.out.println("no ini. making new ini with default setting.");
			try {
				fos = new FileOutputStream(conf);
				fos.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
//				System.out.println("cannot make ini.");
			}

			try {
				Wini winif = new Wini(conf);
				winif.put("UsageMonitor", "CpuLimit", cpuLimit);
				winif.put("UsageMonitor", "MemLimit", memLimit);
				winif.put("UsageMonitor", "checkStr", "");

				winif.store();
			} catch (IOException ioe) {
				ioe.printStackTrace();
//				System.out.println("cannot write ini.");
			}
		}
	}

	public static void checkUsage() {
		try {
			osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

			double usageCpu = osBean.getSystemCpuLoad() * 100;

			Thread.sleep(1000);
			usageCpu = osBean.getSystemCpuLoad() * 100;
			long usageFrM = osBean.getFreePhysicalMemorySize();
			long usageToM = osBean.getTotalPhysicalMemorySize();

			// Debug
			System.out.println("CPU Usage : " + String.format("%.2f", usageCpu) + "%");
			System.out.println("Mem Usage : " + usageFrM + " / " + usageToM + " ("
					+ String.format("%.2f", 100 * (double) usageFrM / usageToM) + "%)");

			if (usageCpu > cpuLimit)
				System.out.println("서버 CPU 사용량이 " + cpuLimit + "%를 초과했습니다. CPU 점유율을 확인하세요.");
			if (100 * (double) usageFrM / usageToM > memLimit)
				System.out.println("서버 메모리 사용량이 " + memLimit + "%를 초과했습니다. 메모리 점유율을 확인하세요.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void checkLog() {
		LocalDate now = LocalDate.now();

		String filename = "catalina.out." + String.format("%4d", now.getYear()) + "-"
				+ String.format("%2d", now.getMonthValue()) + "-" + String.format("%2d", now.getDayOfMonth());
		String logAddr = "";

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		int errorCount = 0;

		try {
			br = new BufferedReader(new InputStreamReader((fis = new FileInputStream(logAddr + filename)), "UTF-8"));
			String str = "";
			while ((str = br.readLine()) != null) {
				sb.append(str);
				for(String s : checkStr) 
					if(str.contains(s)) errorCount++;
			}
		} catch (IOException e) {
			try {
				if (br != null)
					br.close();
				if (fis != null)
					fis.close();
			} catch (Exception e1) {
			}
		}
		
		if (errorCount >= 4) System.out.println("서버 오류 로그가 발생 했습니다. 서비스 상태를 확인하세요.");
		
		
	}

	public static void main(String[] args) {
		loadIni();
		checkUsage();
		checkLog();
	}

}
