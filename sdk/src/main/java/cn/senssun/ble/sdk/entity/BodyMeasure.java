package cn.senssun.ble.sdk.entity;

import java.io.Serializable;

public class BodyMeasure implements Serializable{
	private static final long serialVersionUID = 1L;
	
	 private int WeightKg=-1;
	 private int WeightLb=-1;
	 private boolean IfStable=false;
	 
	 public BodyMeasure(){}


	public void empty(){
		  WeightKg=-1;
		  WeightLb=-1;
		  IfStable=false;
	 }
	 
	public int getWeightKg() {
		return WeightKg;
	}
	public void setWeightKg(int weightKg) {
		WeightKg = weightKg;
	}
	public int getWeightLb() {
		return WeightLb;
	}
	public void setWeightLb(int weightLb) {
		WeightLb = weightLb;
	}
	public boolean isIfStable() {
		return IfStable;
	}
	public void setIfStable(boolean ifStable) {
		IfStable = ifStable;
	}
	 
	
	 
}
