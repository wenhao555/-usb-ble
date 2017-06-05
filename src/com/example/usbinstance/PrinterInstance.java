package com.example.usbinstance;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;

//打印机类
public class PrinterInstance {
	private static final String TAG = "PrinterInstance";
	private static BasePrinterPort myPrinterPort;
	// 字符集名称
	private String charsetName = "gbk";
	public static PrinterInstance mPrinter = null;
	int paperWidth = 576;
	int mLineWidth = 0;

	// private PrinterInstance(BluetoothDevice mdevices,Handler handler){
	// myPrinterPort=new BluetoothPort
	// }

	// 接口方法
	private PrinterInstance(Context context, UsbDevice usbDevice,
			Handler handler) {
		myPrinterPort = new USBPort(context, usbDevice, handler);
	}

	// 该方法加锁
	public static synchronized PrinterInstance getPrinterInstance(
			Context context, UsbDevice usbDevice, Handler handler) {
		if (mPrinter == null) {
			mPrinter = new PrinterInstance(context, usbDevice, handler);
		}
		return mPrinter;
	}

	// 判断是否打印连接 有就打开连接
	public boolean openConnection() {
		boolean isConnected = false;
		// 当USB接口为空的时候
		if (myPrinterPort == null) {
			// 当没有初始化打印机
			return false;
		} else {
			if (!(isConnected = myPrinterPort.open())) {
				mPrinter = null;
			}
		}
		return isConnected;

	}

	// 关闭连接
	public void closeConnection() {
		if (myPrinterPort == null) {
			// 当空时，显示关闭失败
		} else {
			myPrinterPort.close();
		}
		mPrinter = null;
	}

	// 串口和USB连接模式下都一样发送指令（只写了USB连接）
	public int sendBytesData(byte[] srcData) throws InterruptedException {
		if (myPrinterPort == null) {
			// 当打印机为null的时候不能发送数据
			return -1;
		} else if (srcData != null && srcData.length != 0) {// 当传递的数据不为空的时候
			if (!(myPrinterPort instanceof USBPort)) {// 判断myPrinterPort是不是USBPort的实例
				if (myPrinterPort.write(srcData) < 0) {// 当write的值小于0的时候
					return -3;
				}
			} else {
				short packetSize = 512;
				int num = srcData.length / packetSize;
				byte[] pack = new byte[packetSize];
				byte[] temp = new byte[srcData.length - packetSize * num];
				if (num >= 1) {
					for (int i = 0; i <= num - 1; ++i) {
						myPrinterPort.write(pack);
						Thread.sleep(20L);
					}
					if (myPrinterPort.write(temp) < 0) {
						return -3;
					}
				} else if (myPrinterPort.write(srcData) < 0) {
					return -3;
				}
			}
			return srcData.length;
		} else {
			// 发送数据失败byte数组为空或者么有byte数组
			return -2;
		}
	}

	/*
	 * 读取打印机返回的数据（用于接收读到字节的数组） 返回为-1的时候未初始化打印机 返回为-2的时候数组里没有数据或者为空
	 */
	public int read(byte[] buffer) {
		if (myPrinterPort == null) {

			// 发送数据失败打印机为空
			return -1;
		} else if (buffer != null && buffer.length != 0) {
			return myPrinterPort.read(buffer);
		} else {
			// buffer为空的话
			return -2;
		}
	}

	// 设置打印机
	public void setPrinter(int command, int value) throws InterruptedException {
		byte[] arrayOFByte = new byte[3];
		switch (command) {
		case 0:
			arrayOFByte[0] = 27;
			arrayOFByte[1] = 74;
			break;
		case 1:
			arrayOFByte[0] = 27;
			arrayOFByte[1] = 100;
			break;
		case 4:
			arrayOFByte[0] = 27;
			arrayOFByte[1] = 86;
			break;
		case 11:
			arrayOFByte[0] = 27;
			arrayOFByte[1] = 32;
			break;
		case 13:
			arrayOFByte[0] = 27;
			arrayOFByte[1] = 97;
			if (value > 2 || value < 0) {
				value = 0;
			}
		}
		arrayOFByte[2] = (byte) value;
		this.sendBytesData(arrayOFByte);
	}
//读取打印机状态
	public int getCurremtStates() throws InterruptedException {
		int readLen = -1;
		byte retByte = -1;
		int i;
		for (i = 0; i < 2; ++i) {
			// 循环发送的字符组
			this.sendBytesData(new byte[] { (byte) 16, (byte) 4, (byte) 4 });
			Thread.sleep(100L);
		}
		byte[] buffer;
		int k;
		label127: for (i = 0; i < 3; ++i) {
			buffer = new byte[16];
			readLen = this.read(buffer);
			if (readLen > 1 && readLen < '\uffff') {
				k = 0;
				while (true) {
					if (k >= readLen) {
						break label127;
					}

					if (buffer[k] != 0) {
						retByte = buffer[k];
					}
					++k;
				}
			}
			if (readLen == 1) {
				if ((retByte = buffer[0]) != 0) {
					retByte = buffer[0];
					break;
				}
			} else if (readLen == '\uffff') {
				return -1;// 初始化打印机
			}
		}

		if (readLen == 0) {
			return -1;
		} else {
			if ((retByte & 96) == 96) {
				return -2;
			} else if ((retByte & 12) == 12) {
				return -3;
			} else {
				for (i = 0; i < 2; ++i) {
					this.sendBytesData(new byte[] { (byte) 16, (byte) 4,
							(byte) 2 });
					Thread.sleep(100L);
				}

				label103: for (i = 0; i < 3; ++i) {
					buffer = new byte[16];
					readLen = this.read(buffer);
					if (readLen > 1 && readLen < '\uffff') {
						k = 0;
						while (true) {
							if (k >= readLen) {
								break label103;
							}
							if (buffer[k] != 0) {
								retByte = buffer[k];
							}
							++k;
						}
					}

					if (readLen == 1) {
						if ((retByte = buffer[0]) != 0) {
							retByte = buffer[0];
							break;
						}
					} else if (readLen == '\uffff') {
						return -1;
					}
				}

				if (readLen == 0) {
					return -1;
				} else {
					return (retByte & 4) == 4 ? -4 : 0;
				}
			}
		}
	}
}
