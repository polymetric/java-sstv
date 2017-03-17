package sstvgen;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
		byte[] file_bytes = Files.readAllBytes(Paths.get("test"));
		BitSet bits = BitSet.valueOf(file_bytes);
		byte[] bytes = encode(1000, 30, bits);
		
		audioFormat = new AudioFormat(Encoding.PCM_SIGNED, SAMPLE_RATE, 8, 1, 1, 1, false);
		line = AudioSystem.getSourceDataLine(audioFormat);
		line.open(audioFormat, SAMPLE_RATE);
		line.start();
//		line.write(bytes, 0, bytes.length);
		Files.write(Paths.get("output.raw"), bytes);
		
//		Thread.sleep(500);

		line.drain();
		line.close();
	}
	
	public static byte[] encode(int frequency, int sync_rate, BitSet input_data) {
		ArrayList<Byte> audio = new ArrayList<Byte>();
		// for every bit in input data
		for (int i = 0; i < input_data.size(); i++) {
			byte[] add_bytes;
			if (input_data.get(i)) {
				add_bytes = getwave(frequency, sync_rate);
			} else {
				add_bytes = getsilence(sync_rate);
			}
			for (int j = 0; j < add_bytes.length; j++) {
				audio.add(add_bytes[j]);
			}
		}
		byte[] out = new byte[audio.size()];
		for (int i = 0;i< audio.size();i++) {
			out[i] = audio.get(i);
		}
		return out;
	}
	
	public static byte[] getsilence(double ms) {
		byte[] sin = new byte[(int) (SAMPLE_RATE * ms / 1000)];
		return sin;
	}

	public static byte[] getwave(int frequency, double ms) {
		byte[] sin = new byte[(int) (SAMPLE_RATE * ms / 1000)];

		for (int i = 0; i < sin.length; i++) {
			double period = (double) SAMPLE_RATE / frequency;
			double angle = 2.0 * Math.PI * i / period;
			sin[i] = (byte) (Math.sin(angle) * 127f * .8);
		}
		
		return sin;
	}
}