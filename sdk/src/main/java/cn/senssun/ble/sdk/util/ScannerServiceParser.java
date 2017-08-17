//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk.util;

import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class ScannerServiceParser {
    private static final String TAG = "ScannerServiceParser";
    private static final int FLAGS_BIT = 1;
    private static final int SERVICES_MORE_AVAILABLE_16_BIT = 2;
    private static final int SERVICES_COMPLETE_LIST_16_BIT = 3;
    private static final int SERVICES_MORE_AVAILABLE_32_BIT = 4;
    private static final int SERVICES_COMPLETE_LIST_32_BIT = 5;
    private static final int SERVICES_MORE_AVAILABLE_128_BIT = 6;
    private static final int SERVICES_COMPLETE_LIST_128_BIT = 7;
    private static final int SHORTENED_LOCAL_NAME = 8;
    private static final int COMPLETE_LOCAL_NAME = 9;
    private static final int COMPLETE_MANUFACTURER = -1;
    private static final byte LE_LIMITED_DISCOVERABLE_MODE = 1;
    private static final byte LE_GENERAL_DISCOVERABLE_MODE = 2;

    public ScannerServiceParser() {
    }

    public static boolean decodeDeviceAdvData(byte[] data, UUID requiredUUID) {
        String uuid = requiredUUID != null?requiredUUID.toString():null;
        if(data == null) {
            return false;
        } else {
            boolean connectable = false;
            boolean valid = uuid == null;
            int packetLength = data.length;

            for(int index = 0; index < packetLength; ++index) {
                byte fieldLength = data[index];
                if(fieldLength == 0) {
                    if(connectable && valid) {
                        return true;
                    }

                    return false;
                }

                ++index;
                byte fieldName = data[index];
                if(uuid != null) {
                    int flags;
                    if(fieldName != 2 && fieldName != 3) {
                        if(fieldName == 4 || fieldName == 5) {
                            for(flags = index + 1; flags < index + fieldLength - 1; flags += 4) {
                                valid = valid || decodeService32BitUUID(uuid, data, flags, 4);
                            }
                        } else if(fieldName == 6 || fieldName == 7) {
                            for(flags = index + 1; flags < index + fieldLength - 1; flags += 16) {
                                valid = valid || decodeService128BitUUID(uuid, data, flags, 16);
                            }
                        }
                    } else {
                        for(flags = index + 1; flags < index + fieldLength - 1; flags += 2) {
                            valid = valid || decodeService16BitUUID(uuid, data, flags, 2);
                        }
                    }
                }

                if(fieldName == 1) {
                    byte var10 = data[index + 1];
                    connectable = (var10 & 3) > 0;
                }

                index += fieldLength - 1;
            }

            return connectable && valid;
        }
    }

    public static String decodeDeviceName(byte[] data) {
        String name = null;
        int packetLength = data.length;

        for(int index = 0; index < packetLength; ++index) {
            byte fieldLength = data[index];
            if(fieldLength == 0) {
                break;
            }

            ++index;
            byte fieldName = data[index];
            if(fieldName == 9 || fieldName == 8) {
                name = decodeLocalName(data, index + 1, fieldLength - 1);
                break;
            }

            index += fieldLength - 1;
        }

        return name;
    }

    public static byte[] decodeManufacturer(byte[] data) {
        Object name = null;
        byte[] one = null;
        byte[] to = new byte[62];
        int packetLength = data.length;

        for(int index = 0; index < packetLength; ++index) {
            byte fieldLength = data[index];
            if(fieldLength == 0) {
                break;
            }

            ++index;
            byte fieldName = data[index];
            if(fieldName == -1) {
                ByteDataConvertUtil.BinnCat(data, to, index + 1, fieldLength - 1);
                one = new byte[fieldLength - 1];
                ByteDataConvertUtil.BinnCat(to, one, 0, fieldLength - 1);
                break;
            }

            index += fieldLength - 1;
        }

        return one;
    }

    public static String decodeLocalName(byte[] data, int start, int length) {
        try {
            return new String(data, start, length, "UTF-8");
        } catch (UnsupportedEncodingException var4) {
            Log.e("ScannerServiceParser", "Unable to convert the complete local name to UTF-8", var4);
            return null;
        } catch (IndexOutOfBoundsException var5) {
            Log.e("ScannerServiceParser", "Error when reading complete local name", var5);
            return null;
        }
    }

    private static boolean decodeService16BitUUID(String uuid, byte[] data, int startPosition, int serviceDataLength) {
        String serviceUUID = String.format("%04x", new Object[]{Integer.valueOf(decodeUuid16(data, startPosition))});
        String requiredUUID = uuid.substring(4, 8);
        Log.v("ScannerServiceParser", requiredUUID + "--16--" + serviceUUID);
        return serviceUUID.equalsIgnoreCase(requiredUUID);
    }

    private static boolean decodeService32BitUUID(String uuid, byte[] data, int startPosition, int serviceDataLength) {
        String serviceUUID = String.format("%04x", new Object[]{Integer.valueOf(decodeUuid16(data, startPosition + serviceDataLength - 4))});
        String requiredUUID = uuid.substring(4, 8);
        Log.v("ScannerServiceParser", requiredUUID + "--32--" + serviceUUID);
        return serviceUUID.equalsIgnoreCase(requiredUUID);
    }

    private static boolean decodeService128BitUUID(String uuid, byte[] data, int startPosition, int serviceDataLength) {
        String serviceUUID = String.format("%04x", new Object[]{Integer.valueOf(decodeUuid16(data, startPosition + serviceDataLength - 4))});
        String requiredUUID = uuid.substring(4, 8);
        return serviceUUID.equalsIgnoreCase(requiredUUID);
    }

    private static int decodeUuid16(byte[] data, int start) {
        int b1 = data[start] & 255;
        int b2 = data[start + 1] & 255;
        return b2 << 8 | b1 << 0;
    }
}
