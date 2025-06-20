package tz.deadparrot;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Processor processor = new Processor();
        processor.init();
    }
}