package cn.senssun.ble.sdk.grow;

import java.util.List;

import cn.senssun.ble.sdk.entity.GrowMeasure;
import cn.senssun.ble.sdk.entity.SysUserInfo;

public class GrowOnActionMethod{
	public interface OnInitService{
		void OnInit();
	}
	public interface OnConnectState{
		void OnState(boolean State);
	}
	public interface OnDisplayDATA{
		void OnDATA(GrowMeasure growMeasure);
	}
	public interface OnUserInfoStatus{
		void OnListener(int isStatus);
	}
	
}