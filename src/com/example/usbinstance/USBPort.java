package com.example.usbinstance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;

@SuppressLint("NewApi")
public class USBPort implements BasePrinterPort {
	// 标签
	private static final String TAG = "USBPrinter";
	// 设备连接的权限
	private final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";
	// USB设备控制
	private UsbManager mUsbManager;
	// USB设备
	private UsbDevice mUsbDevice;
	// USB设备连接
	private UsbDeviceConnection connection;
	// USB设备接口
	private UsbInterface usbInterface;
	// 从设备端读取信息
	private UsbEndpoint inEndpoint;
	// 从手机端发送信息给设备
	private UsbEndpoint outEndpoint;

	private Handler mHandler;
	// 状态
	private int mState;
	private Context mContext;
	// 判断是否为旧设备
	private boolean isOldUSB;

	public USBPort(Context context, UsbDevice usbDevice, Handler handler) {
		this.mContext = context;
		this.mHandler = handler;
		this.mUsbDevice = usbDevice;
		this.mState = 103;
		// 监听USB连接的服务
		this.mUsbManager = (UsbManager) this.mContext.getSystemService("usb");

	}

	@Override
	public boolean open() {
		// TODO Auto-generated method stub
		boolean isConnected = false;
		// 判断当状态不等于103的时候，接口直接关闭
		if (this.mState != 103) {

			this.close();
		}

		if (isUSBPrinter(this.mUsbDevice)) {
			isConnected = this.connect();
		} else {
			this.setState(102);
		}
		return isConnected;
	}

	// 判断是否为USB打印机设备.
	@SuppressLint("NewApi")
	public static boolean isUSBPrinter(UsbDevice device) {
		int vendorId = device.getVendorId();
		int productId = device.getProductId();

		return 1155 == vendorId && 22304 == productId || 1659 == vendorId
				&& 8965 == productId;

	}

	// 判断是否连接
	private boolean connect() {
		// 判断连接是否有错误
		boolean hasError = false;
		if (this.mUsbManager.hasPermission(this.mUsbDevice)) {
			this.isOldUSB = 1659 == this.mUsbDevice.getVendorId()
					&& 8965 == this.mUsbDevice.getProductId();
			try {
				// 得到该设备的一个接口
				this.usbInterface = this.mUsbDevice.getInterface(0);
				// 得到该设备的所有接口
				for (int i = 0; i < this.usbInterface.getEndpointCount(); ++i) {
					// 得到每一个接口
					UsbEndpoint ep = this.usbInterface.getEndpoint(i);
					// 得到类型为2的端口
					if (ep.getType() == 2) {
						if (ep.getDirection() == 0) {// 从手机向device发送数据
							this.outEndpoint = ep;

						} else if (ep.getDirection() == 128) {// 从devic向手机发送数据
							this.inEndpoint = ep;
						}
					}
				}
				// 打开设备
				this.connection = this.mUsbManager.openDevice(this.mUsbDevice);
				// 声明接口
				if (this.connection != null
						&& this.connection.claimInterface(this.usbInterface,
								true)) {
					hasError = false;
				}
			} catch (Exception var4) {
				var4.printStackTrace();
			}
		}
		// 如果判断是错误的
		if (hasError) {
			this.setState(102);
			this.close();
		} else {
			this.setState(101);
		}
		// 返回与hasError相反的数
		return !hasError;

	}

	// 判断状态，并通过状态发送Handler
	private synchronized void setState(int state) {
		if (this.mState != state) {
			this.mState = state;
			if (this.mHandler != null) {
				this.mHandler.obtainMessage(this.mState).sendToTarget();
			}
		}
	}

	// 关闭状态
	@Override
	public void close() {
		// TODO Auto-generated method stub
		try {
			if (this.connection != null) {
				// 当打印机的连接部位null的时候发布该接口
				this.connection.releaseInterface(this.usbInterface);
				this.connection.close();
				this.connection = null;
			}
			// 当打印机的状态不为102的时候
			if (this.mState != 102) {
				this.setState(103);
			}
		} catch (Exception var2) {
			var2.printStackTrace();
		}
	}

	// 发送数据给打印机
	@Override
	public int write(byte[] srcData) {
		// TODO Auto-generated method stub
		try {
			if (this.connection == null) {
				// 当打印机连接为空的时候
				return -1;
			}
			// 通过0节点向此设备传输数据，传输的方向取决于请求的类别（发送）
			this.connection.bulkTransfer(this.outEndpoint, srcData,
					srcData.length, 3000);

		} catch (Exception e) {
			// TODO: handle exception'
			e.printStackTrace();
			return -1;
		}
		return srcData.length;
	}

	// 读取数据给打印机
	@Override
	public int read(byte[] buffer) {
		// TODO Auto-generated method stub
		int readLen = -1;
		// 判断当连接部位空的时候，打印机向手机设备传递数据（接收）
		if (this.connection != null) {
			readLen = this.connection.bulkTransfer(this.inEndpoint, buffer,
					buffer.length, 3000);
		}
		return readLen;
	}

}
