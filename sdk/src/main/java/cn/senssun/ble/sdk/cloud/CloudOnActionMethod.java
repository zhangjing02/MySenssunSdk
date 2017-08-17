//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk.cloud;


import java.util.List;

import cn.senssun.ble.sdk.entity.SysUserInfo;

public class CloudOnActionMethod {
    public CloudOnActionMethod() {
    }

    public interface OnAllUsers {
        void OnShow(List<SysUserInfo> var1, boolean var2);
    }

    public interface OnConnectState {
        void OnState(boolean var1);
    }

    public interface OnDisplayDATA {
        void OnDATA(String var1);
    }

    public interface OnInitService {
        void OnInit();
    }

    public interface OnUserInfoStatus {
        void OnListener(int var1);
    }
}
