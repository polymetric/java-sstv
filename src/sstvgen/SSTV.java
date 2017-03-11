package sstvgen;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class SSTV {
	public static AudioFormat audioFormat;
	public static SourceDataLine line;
	public static final int SAMPLE_RATE = 44100;

	public static void main(String[] args) throws Exception {
//		System.out.println("Plain FSK");
//		encode_plain("test");
//		Thread.sleep(500);
//		System.out.println("Byte-sized FSK");
//		encode_byte("test");
		audioFormat = new AudioFormat(Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 1, 1, 1, false);
		line = AudioSystem.getSourceDataLine(audioFormat);
		line.open(audioFormat, SAMPLE_RATE);
		line.start();
		line.write(merge_waves(getwave(1200, 500), getwave(1900, 500)), 0, 22049);
		line.write(getwave(1500, 500), 0, 22049);
		
		Thread.sleep(500);
//
//		line.drain();
//		line.close();
	}

	public static void encode_plain(String path) throws Exception {
		int sync = 10;
		
		// set up audio stream
		audioFormat = new AudioFormat(Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 1, 1, 1, false);
		line = AudioSystem.getSourceDataLine(audioFormat);
		line.open(audioFormat, SAMPLE_RATE);
		line.start();

		// load file
		byte[] file = Files.readAllBytes(Paths.get(path));

		BitSet set = BitSet.valueOf(file);

		// convert file to audio
		for (int i = 0; i < set.length(); i++) {
			if (i % 32 == 0) {
				play(1500, sync);
				Files.write(Paths.get("out"), getwave(1500, sync), StandardOpenOption.APPEND);
			}
			if (set.get(i)) {
				play(2000, sync);
				Files.write(Paths.get("out"), getwave(1900, sync), StandardOpenOption.APPEND);
			} else {
				play(1000, sync);
				Files.write(Paths.get("out"), getwave(1200, sync), StandardOpenOption.APPEND);
			}
		}

		line.drain();
		line.close();
	}

	public static void encode_byte(String path) throws Exception {
		int sync = 1000; // the length of each byte in ms
		int freq_start = 1000; // the frequency of the first bit of each byte
		int freq_incr = 500; // the incremental frequency of each bit after the
								// first

		int[] freq = new int[8];

		// set up frequency array
		System.out.println("frequency array:");
		for (int i = 0; i < freq.length; i++) {
			if (i == 0) {
				freq[i] = freq_start;
			} else {
				freq[i] = freq[i - 1] + freq_incr;
			}
			System.out.println(freq[i]);
		}
		// set up audio stream
		audioFormat = new AudioFormat(Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 1, 1, 1, false);
		line = AudioSystem.getSourceDataLine(audioFormat);
		line.open(audioFormat, SAMPLE_RATE);
		line.start();

		// load file
		byte[] file = Files.readAllBytes(Paths.get(path));
		
		// delete and recreate output file
		Files.deleteIfExists(Paths.get("out"));
		Files.createFile(Paths.get("out"));

		// convert file to audio
		for (int i = 0; i < file.length; i++) {

			// set up master stream
			byte[] audio = new byte[sync * SAMPLE_RATE / 1000];
			
			// set up individual bit frequency streams
			byte[] bytes0 = new byte[audio.length];
			byte[] bytes1 = new byte[audio.length];
			byte[] bytes2 = new byte[audio.length];
			byte[] bytes3 = new byte[audio.length];
			byte[] bytes4 = new byte[audio.length];
			byte[] bytes5 = new byte[audio.length];
			byte[] bytes6 = new byte[audio.length];
			byte[] bytes7 = new byte[audio.length];
			
//			System.out.print(i);
//			System.out.print("\t");
//			System.out.println(file[i]);
			
			// for each bit:
			// if the bit is 1, play the frequency at its offset. else, leave the bytes blank
			// 0
			if ((file[i] & 0b10000000) == 1) {
				bytes0 = getwave(freq[0], sync);
			}
			// 1
			if ((file[i] & 0b01000000) == 1) {
				bytes1 = getwave(freq[1], sync);
			}
			// 2
			if ((file[i] & 0b00100000) == 1) {
				bytes2 = getwave(freq[2], sync);
			}
			// 3
			if ((file[i] & 0b00010000) == 1) {
				bytes3 = getwave(freq[3], sync);
			}
			// 0
			if ((file[i] & 0b00001000) == 1) {
				bytes4 = getwave(freq[4], sync);
			}
			// 1
			if ((file[i] & 0b00000100) == 1) {
				bytes5 = getwave(freq[5], sync);
			}
			// 2
			if ((file[i] & 0b00000010) == 1) {
				bytes6 = getwave(freq[6], sync);
			}
			// 3
			if ((file[i] & 0b00000001) == 1) {
				bytes7 = getwave(freq[7], sync);
			}
			
			// merge all 8 audio channels. each channel corresponds to a bit in the byte
			for (int j = 0; j < audio.length; j++) {
				audio[j] += bytes0[j]/2;
//				audio[j] += bytes1[j];
//				audio[j] += bytes2[j];
//				audio[j] += bytes3[j];
//				audio[j] += bytes4[j];
//				audio[j] += bytes5[j];
//				audio[j] += bytes6[j];
//				audio[j] += bytes7[j];
			}

			line.write(audio, 0, audio.length);
//			Files.write(Paths.get("out"), audio, StandardOpenOption.APPEND);
		}
		
		line.drain();
		line.close();
	}

	public static void encode_qam(String path) throws Exception {
//		int sync = 2;
	}
	
	public static byte[] merge_waves (byte[] a, byte[] b) {
		int length = Math.min(a.length, b.length);
		byte[] out = new byte[length];
		for (int i = 0; i < length;i++) {
			out[i] = (byte) ((a[i] + b[i]) / 2);
		}
		return out;
	}
	
	public static void play(int frequency, double ms) throws Exception {
		byte[] sin = getwave(frequency, ms);

		play(sin, ms);
	}
	
	public static void play(byte[] sin, double ms) {
		line.write(sin, 0, (int) (SAMPLE_RATE * ms / 1000));
	}
	
	public static void play(byte[] sin) {
		line.write(sin, 0, sin.length);
	}

	public static byte[] getwave(int frequency, double ms) throws Exception {
		byte[] sin = new byte[SAMPLE_RATE];

		for (int i = 0; i < sin.length; i++) {
			double period = (double) SAMPLE_RATE / frequency;
			double angle = 2.0 * Math.PI * i / period;
			sin[i] = (byte) (Math.sin(angle) * 127f * .8);
		}
		
		return java.util.Arrays.copyOf(sin, (int) (SAMPLE_RATE * ms / 1000));
	}
}