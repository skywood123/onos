package com.sun.net.httpserver;

class Base64 {
    static String byteArrayToBase64(byte[] paramArrayOfbyte) {
        return byteArrayToBase64(paramArrayOfbyte, false);
    }

    static String byteArrayToAltBase64(byte[] paramArrayOfbyte) {
        return byteArrayToBase64(paramArrayOfbyte, true);
    }

    private static String byteArrayToBase64(byte[] paramArrayOfbyte, boolean paramBoolean) {
        int i = paramArrayOfbyte.length;
        int j = i / 3;
        int k = i - 3 * j;
        int m = 4 * (i + 2) / 3;
        StringBuffer stringBuffer = new StringBuffer(m);
        char[] arrayOfChar = paramBoolean ? intToAltBase64 : intToBase64;
        byte b = 0;
        int n;
        for (n = 0; n < j; n++) {
            int i1 = paramArrayOfbyte[b++] & 0xFF;
            int i2 = paramArrayOfbyte[b++] & 0xFF;
            int i3 = paramArrayOfbyte[b++] & 0xFF;
            stringBuffer.append(arrayOfChar[i1 >> 2]);
            stringBuffer.append(arrayOfChar[i1 << 4 & 0x3F | i2 >> 4]);
            stringBuffer.append(arrayOfChar[i2 << 2 & 0x3F | i3 >> 6]);
            stringBuffer.append(arrayOfChar[i3 & 0x3F]);
        }
        if (k != 0) {
            n = paramArrayOfbyte[b++] & 0xFF;
            stringBuffer.append(arrayOfChar[n >> 2]);
            if (k == 1) {
                stringBuffer.append(arrayOfChar[n << 4 & 0x3F]);
                stringBuffer.append("==");
            } else {
                int i1 = paramArrayOfbyte[b++] & 0xFF;
                stringBuffer.append(arrayOfChar[n << 4 & 0x3F | i1 >> 4]);
                stringBuffer.append(arrayOfChar[i1 << 2 & 0x3F]);
                stringBuffer.append('=');
            }
        }
        return stringBuffer.toString();
    }

    private static final char[] intToBase64 = new char[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '+', '/' };

    private static final char[] intToAltBase64 = new char[] {
            '!', '"', '#', '$', '%', '&', '\'', '(', ')', ',',
            '-', '.', ':', ';', '<', '>', '@', '[', ']', '^',
            '`', '_', '{', '|', '}', '~', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '+', '?' };

    static byte[] base64ToByteArray(String paramString) {
        return base64ToByteArray(paramString, false);
    }

    static byte[] altBase64ToByteArray(String paramString) {
        return base64ToByteArray(paramString, true);
    }

    private static byte[] base64ToByteArray(String paramString, boolean paramBoolean) {
        byte[] arrayOfByte1 = paramBoolean ? altBase64ToInt : base64ToInt;
        int i = paramString.length();
        int j = i / 4;
        if (4 * j != i)
            throw new IllegalArgumentException("String length must be a multiple of four.");
        byte b1 = 0;
        int k = j;
        if (i != 0) {
            if (paramString.charAt(i - 1) == '=') {
                b1++;
                k--;
            }
            if (paramString.charAt(i - 2) == '=')
                b1++;
        }
        byte[] arrayOfByte2 = new byte[3 * j - b1];
        byte b2 = 0, b3 = 0;
        int m;
        for (m = 0; m < k; m++) {
            int n = base64toInt(paramString.charAt(b2++), arrayOfByte1);
            int i1 = base64toInt(paramString.charAt(b2++), arrayOfByte1);
            int i2 = base64toInt(paramString.charAt(b2++), arrayOfByte1);
            int i3 = base64toInt(paramString.charAt(b2++), arrayOfByte1);
            arrayOfByte2[b3++] = (byte)(n << 2 | i1 >> 4);
            arrayOfByte2[b3++] = (byte)(i1 << 4 | i2 >> 2);
            arrayOfByte2[b3++] = (byte)(i2 << 6 | i3);
        }
        if (b1 != 0) {
            m = base64toInt(paramString.charAt(b2++), arrayOfByte1);
            int n = base64toInt(paramString.charAt(b2++), arrayOfByte1);
            arrayOfByte2[b3++] = (byte)(m << 2 | n >> 4);
            if (b1 == 1) {
                int i1 = base64toInt(paramString.charAt(b2++), arrayOfByte1);
                arrayOfByte2[b3++] = (byte)(n << 4 | i1 >> 2);
            }
        }
        return arrayOfByte2;
    }

    private static int base64toInt(char paramChar, byte[] paramArrayOfbyte) {
        byte b = paramArrayOfbyte[paramChar];
        if (b < 0)
            throw new IllegalArgumentException("Illegal character " + paramChar);
        return b;
    }

    private static final byte[] base64ToInt = new byte[] {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, 62, -1, -1, -1, 63, 52, 53,
            54, 55, 56, 57, 58, 59, 60, 61, -1, -1,
            -1, -1, -1, -1, -1, 0, 1, 2, 3, 4,
            5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, -1, -1, -1, -1, -1, -1, 26, 27, 28,
            29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
            39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51 };

    private static final byte[] altBase64ToInt = new byte[] {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, 0, 1, 2, 3, 4, 5, 6,
            7, 8, -1, 62, 9, 10, 11, -1, 52, 53,
            54, 55, 56, 57, 58, 59, 60, 61, 12, 13,
            14, -1, 15, 63, 16, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, 17, -1, 18, 19, 21, 20, 26, 27, 28,
            29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
            39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51, 22, 23, 24, 25 };
}
