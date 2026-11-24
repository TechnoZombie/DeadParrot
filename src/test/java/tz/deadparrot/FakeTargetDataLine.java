package tz.deadparrot;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.TargetDataLine;

final class FakeTargetDataLine implements TargetDataLine {
    private final AudioFormat format;
    private final byte[] audioData;
    private int position;
    private boolean open;
    private boolean running;
    private final boolean blockAfterAudioEnds;

    FakeTargetDataLine(AudioFormat format, byte[] audioData) {
        this(format, audioData, false);
    }

    FakeTargetDataLine(AudioFormat format, byte[] audioData, boolean blockAfterAudioEnds) {
        this.format = format;
        this.audioData = audioData;
        this.blockAfterAudioEnds = blockAfterAudioEnds;
    }

    @Override public void open(AudioFormat format, int bufferSize) { open = true; }
    @Override public void open(AudioFormat format) { open = true; }
    @Override public synchronized int read(byte[] buffer, int offset, int length) {
        while (position == audioData.length && blockAfterAudioEnds && open && running) {
            try {
                wait();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        if (position == audioData.length) return -1;
        int count = Math.min(length, audioData.length - position);
        System.arraycopy(audioData, position, buffer, offset, count);
        position += count;
        return count;
    }
    @Override public void open() { open = true; }
    @Override public synchronized void close() { open = false; running = false; notifyAll(); }
    @Override public boolean isOpen() { return open; }
    @Override public void start() { running = true; }
    @Override public void stop() { running = false; }
    @Override public boolean isRunning() { return running; }
    @Override public boolean isActive() { return running; }
    @Override public AudioFormat getFormat() { return format; }
    @Override public int getBufferSize() { return audioData.length; }
    @Override public int available() { return audioData.length - position; }
    @Override public int getFramePosition() { return position / format.getFrameSize(); }
    @Override public long getLongFramePosition() { return getFramePosition(); }
    @Override public long getMicrosecondPosition() { return 0; }
    @Override public float getLevel() { return 0; }
    @Override public void drain() { }
    @Override public void flush() { }
    @Override public Line.Info getLineInfo() { return new DataLine.Info(TargetDataLine.class, format); }
    @Override public Control[] getControls() { return new Control[0]; }
    @Override public boolean isControlSupported(Control.Type control) { return false; }
    @Override public Control getControl(Control.Type control) { throw new IllegalArgumentException(); }
    @Override public void addLineListener(LineListener listener) { }
    @Override public void removeLineListener(LineListener listener) { }
}
