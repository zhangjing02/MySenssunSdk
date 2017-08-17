package cn.senssun.ble.sdk.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class SysUserInfo implements Parcelable{

	private int serialNum=-1;
	private String PIN;
	private int Sex;//1:女 0:男
	private int height;
	private int age;
	private int activity;
	private int weight;
	public int getSerialNum() {
		return serialNum;
	}
	public void setSerialNum(int serialNum) {
		this.serialNum = serialNum;
	}
	public String getPIN() {
		return PIN;
	}
	public void setPIN(String pIN) {
		PIN = pIN;
	}
	public int getSex() {
		return Sex;
	}
	public void setSex(int sex) {
		Sex = sex;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public int getActivity() {
		return activity;
	}
	public void setActivity(int activity) {
		this.activity = activity;
	}
	public int getWeight() {
		return weight;
	}
	public void setWeight(int weight) {
		this.weight = weight;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SysUserInfo) {
			SysUserInfo u = (SysUserInfo) obj;
			return this.getPIN().equals(u.getPIN());
		}
		return super.equals(obj);
	}
	@Override
	public int describeContents() 
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) 
	{
		out.writeInt(serialNum);
		out.writeString(PIN);
		out.writeInt(Sex);
		out.writeInt(height);
		out.writeInt(age);
		out.writeInt(activity);
		out.writeInt(weight);

	}

	public static final Creator<SysUserInfo> CREATOR = new Creator<SysUserInfo>()
			{
		@Override
		public SysUserInfo createFromParcel(Parcel in) 
		{

			SysUserInfo face = new SysUserInfo();  
			face.serialNum = in.readInt();
			face.PIN=in.readString();
			face.Sex=in.readInt();
			face.height=in.readInt();
			face.age=in.readInt();
			face.activity=in.readInt();
			face.weight=in.readInt();

			return face;
		}

		@Override
		public SysUserInfo[] newArray(int size) 
		{
			return new SysUserInfo[size];
		}
			};


}
