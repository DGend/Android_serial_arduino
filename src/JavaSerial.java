import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.sql.*;
import java.util.Enumeration;

public class JavaSerial implements SerialPortEventListener {

	boolean flag = false; // 이전 값 0이면 false 이전 값 1이면 true

	int count = 0;
	byte id[] = new byte[2]; // id 값을 2byte만큼 받은 경우 다음 단계로 진행
	long time = 0;
	
	SerialPort serialPort;
	int pRoom[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/** 당신의 아두이노와 연결된 시리얼 포트로 변경해야 한다. */
	private static final String PORT_NAMES[] = { "COM4", // Windows
	};

	/** 포트에서 데이터를 읽기 위한 버퍼를 가진 input stream */
	private InputStream input;

	/** 포트를 통해 아두이노에 데이터를 전송하기 위한 output stream */
	private OutputStream output;

	/** 포트가 오픈되기 까지 기다리기 위한 대략적인 시간(2초) */
	private static final int TIME_OUT = 2000;

	/** 포트에 대한 기본 통신 속도, 아두이노의 Serial.begin의 속도와 일치 */
	private static final int DATA_RATE = 9600;

	public void initialize() {

		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		// 당신의 컴퓨터에서 지원하는 시리얼 포트들 중 아두이노와 연결된

		// 포트에 대한 식별자를 찾는다.

		while (portEnum.hasMoreElements()) {

			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum
					.nextElement();

			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}

		// 식별자를 찾지 못했을 경우 종료

		if (portId == null) {

			System.out.println("Could not find COM port.");

			return;

		}

		try {

			// 시리얼 포트 오픈, 클래스 이름을 애플리케이션을 위한 포트 식별 이름으로 사용

			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);

			// 속도등 포트의 파라메터 설정

			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			// 포트를 통해 읽고 쓰기 위한 스트림 오픈

			input = serialPort.getInputStream();
			output = serialPort.getOutputStream();

			// 아두이노로 부터 전송된 데이터를 수신하는 리스너를 등록
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * 이 메서드는 포트 사용을 중지할 때 반드시 호출해야 한다. 리눅스와 같은 플랫폼에서는 포트 잠금을 방지한다.
	 */

	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * 시리얼 통신에 대한 이벤트를 처리. 데이터를 읽고 출력한다..
	 */

	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {								
				int available = input.available();
				int pSpace = 0;
				byte chunk[] = new byte[available];
				input.read(chunk, 0, available);
				//
				if (available > 1) {
					id[count] = chunk[count];
					count++;
				} else {
					id[count] = chunk[0];
					count++;
				}

				if (count > 1) {
					String t = new String(id);
					pSpace = Integer.parseInt(t);
					
					if(pRoom[pSpace] == 0){
						dBConnection(pSpace, 1);
						pRoom[pSpace] = 1; 
					}				
					
					System.out.print(pSpace);
					count = 0;
				}

				if(time == 0){
					time = System.currentTimeMillis();
				} else{
					if((time-System.currentTimeMillis())/1000000.0 > 1){
						for(int i=0; i<available; i++){
							if (pRoom[pSpace] == 1) {								
								dBConnection(pSpace, 0);
								pRoom[pSpace] = 0;
							}
						}
						time = 0;
					}
				}
				
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}		
	}

	public synchronized void dBConnection(int pNum, int pCheck) {

		try {
			Class.forName("org.git.mm.mysql.Driver");
			System.out.println("jdbc 드라이버 로딩 성공");
		} catch (ClassNotFoundException e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
		}
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://127.0.0.1:3306/parking", "root", "1234");

		} catch (SQLException e) {
			// TODO: handle exception
		}
		try {
			Statement stmt = conn.createStatement();
			StringBuffer sql = new StringBuffer();
			sql.append("update parking_t		\n");
			sql.append("set pcheck=" + pCheck + "		\n");
			sql.append("where pid = '" + pNum + "'		\n");

			int result = stmt.executeUpdate(sql.toString());
			System.out.println("@@" + result + "행 수정");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		JavaSerial main = new JavaSerial();
		main.initialize();
		System.out.println("Started");
	}
}
