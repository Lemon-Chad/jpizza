package lemon.jpizza.Libraries.JDraw;

import lemon.jpizza.Shell;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

public class PlayThread extends Thread {
    byte[] tempBuffer = new byte[10000];
    AudioFormat audioFormat;
    SourceDataLine sourceDataLine;
    AudioInputStream audioInputStream;

    public PlayThread(AudioFormat audioFormat, SourceDataLine sourceDataLine, AudioInputStream audioInputStream) {
        this.sourceDataLine = sourceDataLine;
        this.audioInputStream = audioInputStream;
        this.audioFormat = audioFormat;
    }

    public void run() {
        try {
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            int cnt;
            while ((cnt = audioInputStream.read(
                    tempBuffer, 0, tempBuffer.length
            )) != -1) {
                if (cnt > 0) {
                    sourceDataLine.write(tempBuffer, 0, cnt);
                }
            }
            sourceDataLine.drain();
            sourceDataLine.close();
        } catch (LineUnavailableException | IOException e) {
            Shell.logger.outln("Error playing audio: " + e.toString());
        }
    }
}
