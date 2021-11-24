package com.masakiauto.loaderremocon;

import java.util.UUID;

public interface Constants {
    static final String BT_DEVICE = "CANTERRemote";
    static final UUID BT_UUID = UUID.fromString(
            //           "0000112F-0000-1000-8000-00805F9B34FB");
            "00001101-0000-1000-8000-00805F9B34FB");
    static final String BT_MESS = "BT_MESS";
    static final int MESSAGE_BT = 0;
    static final int MESSAGE_TEMP = 2;

}
