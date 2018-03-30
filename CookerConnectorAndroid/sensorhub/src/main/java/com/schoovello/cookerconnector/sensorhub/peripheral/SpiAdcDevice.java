package com.schoovello.cookerconnector.sensorhub.peripheral;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.Arrays;

public class SpiAdcDevice implements AutoCloseable {

	public static final String DEFAULT_DEVICE_NAME = "SPI0.0";
	public static final int DEFAULT_ITERATIONS = 100;

	private static final int DEFAULT_BITS_PER_WORD = 8;
	private static final int DEFAULT_SPI_MODE = SpiDevice.MODE0;
	private static final int DEFAULT_FREQUENCY_HZ = 10_000;
	private static final boolean DEFAULT_CS_CHANGE = false;

	private static SpiAdcDevice sSharedDevice;
	private static int sSharedRefCount;

	public static synchronized SpiAdcDevice acquireOpenSharedDevice() throws IOException {
		if (sSharedDevice == null) {
			sSharedDevice = new SpiAdcDevice();
			sSharedDevice.open();
		}
		sSharedRefCount++;
		return sSharedDevice;
	}

	public static synchronized void releaseSharedDevice() throws IOException {
		sSharedRefCount--;
		if (sSharedRefCount == 0) {
			sSharedDevice.close();
			sSharedDevice = null;
		}
	}

	private final String mDeviceName;
	private int mIterations;

	private SpiDevice mSpi;

	private byte[] mIn;

	private SpiAdcDevice() {
		this(DEFAULT_DEVICE_NAME);
	}

	public SpiAdcDevice(String deviceName) {
		mDeviceName = deviceName;

		mIn = new byte[3];

		mIterations = DEFAULT_ITERATIONS;
	}

	public synchronized void setIterations(int iterations) {
		mIterations = iterations;
	}

	public synchronized void open() throws IOException {
		open(DEFAULT_BITS_PER_WORD, DEFAULT_SPI_MODE, DEFAULT_FREQUENCY_HZ, DEFAULT_CS_CHANGE);
	}

	public synchronized void open(int bitsPerWord, int spiMode, int frequencyHz, boolean csChange) throws IOException {
		if (mSpi != null) {
			// already open
			return;
		}

		PeripheralManagerService peripheralManagerService = new PeripheralManagerService();
		mSpi = peripheralManagerService.openSpiDevice(mDeviceName);
		mSpi.setBitsPerWord(bitsPerWord);
		mSpi.setMode(spiMode);
		mSpi.setFrequency(frequencyHz);
		mSpi.setCsChange(csChange);
	}

	public synchronized int getSingleValue(int channel) throws IOException {
		checkOpen();

		return readAdcValue(channel);
	}

	public synchronized double getSampledValue(int channel) throws IOException {
		checkOpen();

		return getSampledValue(channel, mIterations);
	}

	public synchronized double getSampledValue(int channel, int iterations) throws IOException {
		double value = 0;
		for (int i = 0; i < iterations; i++) {
			value += readAdcValue(channel);
		}
		return value / iterations;
	}

	public synchronized double getSampledMedianValue(int channel, int sampleCount) throws IOException {
		final double[] samples = new double[sampleCount];

		for (int i = 0; i < sampleCount; i++) {
			samples[i] = readAdcValue(channel);
		}

		if (sampleCount == 1) {
			return samples[0];
		}

		Arrays.sort(samples);

		final int mid = (sampleCount - 1) / 2;
		if (sampleCount % 2 == 0) {
			return (samples[mid] + samples[mid + 1]) / 2;
		}
		return samples[mid];
	}

	private synchronized int readAdcValue(int channel) throws IOException {
		final byte[] out = COMMANDS_CHANNELS[channel];
		mSpi.transfer(out, mIn, 3);

		//noinspection UnnecessaryLocalVariable
		final int result = ((mIn[1] & 0b00001111) << 8) | toUnsignedInt(mIn[2]);

		return result;
	}

	private synchronized void checkOpen() {
		if (mSpi == null) {
			throw new IllegalStateException("Spi device " + mDeviceName + " is not open");
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (mSpi != null) {
			mSpi.close();
			mSpi = null;
		}
	}

	private static int toUnsignedInt(byte b) {
		return ((int) b) & 0xff;
	}

	/**
	Commands are always 24 bits:
	 <pre>
	[23:19] - Padding: all 0
	[18]    - Start bit: always 1
	[17]    - Single/~Diff: always 1 (single-ended)
	[16:14] - Channel selection (BCD range [0..3])
	[13:0]  - Padding: all 0
	 </pre>
	 */
	private static final byte[][] COMMANDS_CHANNELS = {
			intToThreeBytes(0b00000_1_1_000_00000000000000),
			intToThreeBytes(0b00000_1_1_001_00000000000000),
			intToThreeBytes(0b00000_1_1_010_00000000000000),
			intToThreeBytes(0b00000_1_1_011_00000000000000),
	};

	private static byte[] intToThreeBytes(int value) {
		final byte[] result = new byte[3];

		result[0] = (byte) ((value >>> 16) & 0xff);
		result[1] = (byte) ((value >>> 8)  & 0xff);
		result[2] = (byte) (value          & 0xff);

		return result;
	}
}
