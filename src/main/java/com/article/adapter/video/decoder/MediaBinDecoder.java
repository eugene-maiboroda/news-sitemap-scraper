package com.article.adapter.video.decoder;

// Auto-generated from research5_proto_schema.py
import java.nio.charset.StandardCharsets;

public class MediaBinDecoder {

// ──────────────────────────────────────────────────────────────────────
// Java: read mmid from 2_media.bin without any protobuf library or regex
// Field 17, wire type LEN (2), value is UTF-8 string
// ──────────────────────────────────────────────────────────────────────

public static String extractMmid(byte[] data) {
    int pos = 0;
    while (pos < data.length) {
        // Read tag varint
        long tag = 0; int shift = 0;
        while (pos < data.length) {
            byte b = data[pos++];
            tag |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        int fieldNumber = (int)(tag >>> 3);
        int wireType    = (int)(tag & 0x7L);

        if (wireType == 2) {  // LEN
            long length = 0; shift = 0;
            while (pos < data.length) {
                byte b = data[pos++];
                length |= (long)(b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            if (fieldNumber == 17) {
                // ★ This is the mmid field ★
                return new String(data, pos, (int) length, StandardCharsets.UTF_8);
            }
            pos += (int) length;  // skip other LEN fields
        } else if (wireType == 0) {  // VARINT — skip
            while (pos < data.length && (data[pos++] & 0x80) != 0) {}
        } else if (wireType == 1) {  // I64 — skip
            pos += 8;
        } else if (wireType == 5) {  // I32 — skip
            pos += 4;
        } else {
            break;  // unknown wire type
        }
    }
    return null;  // field 17 not found
}

// Usage:
//   byte[] mediaBin = httpGet("https://vid.connatix.com/" + contentId + "/" + streamId + "/2_media.bin");
//   String mmid = extractMmid(mediaBin);
//   String m3u8Url = "https://vid.connatix.com/" + contentId + "/" + streamId + "/mmid-" + mmid + "/playlist.m3u8";

}
