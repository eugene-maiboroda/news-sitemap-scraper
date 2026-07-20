package com.article.adapter.video.decoder;

import java.nio.charset.StandardCharsets;

public final class MediaBinDecoder {

    private static final int MMID_FIELD_NUMBER = 17;

    private MediaBinDecoder() {
    }

    public static String extractMmid(byte[] data) {
        if (data == null) {
            return null;
        }

        int position = 0;
        while (position < data.length) {
            Varint tag = readVarint(data, position);
            if (tag == null) {
                return null;
            }
            position = tag.nextPosition();

            int fieldNumber = (int) (tag.value() >>> 3);
            int wireType = (int) (tag.value() & 0x07);
            switch (wireType) {
                case 0 -> {
                    Varint value = readVarint(data, position);
                    if (value == null) {
                        return null;
                    }
                    position = value.nextPosition();
                }
                case 1 -> position = skip(data, position, Long.BYTES);
                case 2 -> {
                    Varint length = readVarint(data, position);
                    if (length == null || length.value() > Integer.MAX_VALUE) {
                        return null;
                    }
                    position = length.nextPosition();
                    int size = (int) length.value();
                    if (!hasBytes(data, position, size)) {
                        return null;
                    }
                    if (fieldNumber == MMID_FIELD_NUMBER) {
                        return new String(data, position, size, StandardCharsets.UTF_8);
                    }
                    position += size;
                }
                case 5 -> position = skip(data, position, Integer.BYTES);
                default -> {
                    return null;
                }
            }
            if (position < 0) {
                return null;
            }
        }
        return null;
    }

    private static Varint readVarint(byte[] data, int position) {
        long value = 0;
        int shift = 0;
        while (position < data.length && shift < Long.SIZE) {
            int current = data[position++] & 0xff;
            value |= (long) (current & 0x7f) << shift;
            if ((current & 0x80) == 0) {
                return new Varint(value, position);
            }
            shift += 7;
        }
        return null;
    }

    private static int skip(byte[] data, int position, int bytes) {
        return hasBytes(data, position, bytes) ? position + bytes : -1;
    }

    private static boolean hasBytes(byte[] data, int position, int bytes) {
        return position >= 0 && bytes >= 0 && position <= data.length - bytes;
    }

    private record Varint(long value, int nextPosition) {
    }
}
