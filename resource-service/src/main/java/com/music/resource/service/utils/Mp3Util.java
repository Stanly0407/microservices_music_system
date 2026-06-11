package com.music.resource.service.utils;

public final class Mp3Util {

    private Mp3Util() {
    }

    public static boolean looksLikeMp3(byte[] data) {
        if (data.length >= 3 && data[0] == 'I' && data[1] == 'D' && data[2] == '3') {
            return true;
        }
        return data.length >= 2
                && (data[0] & 0xFF) == 0xFF
                && ((data[1] & 0xE0) == 0xE0 || (data[1] & 0xF0) == 0xF0);
    }
}
