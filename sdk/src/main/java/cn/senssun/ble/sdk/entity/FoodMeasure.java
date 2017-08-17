package cn.senssun.ble.sdk.entity;

import java.io.Serializable;

public class FoodMeasure implements Serializable{
	private static final long serialVersionUID = 1L;
	
	 public enum DataTypeEnum  {
		 gUnit(0),ozUnit(1),mlUnit(2), flozUnit(3),milkmlUnit(4),milkflozUnit(5);
	        private final int value;
	        public int getValue() {
	            return value;
	        }
	        DataTypeEnum(int value) {
	            this.value = value;
	        }
	    }
	 
	 private DataTypeEnum  DataType=DataTypeEnum.gUnit; 
	 private int WeightG=-1;
	 private int WeightML=-1;
	 private boolean IfStable;
	 private boolean symbol;
	 
	public DataTypeEnum getDataType() {
		return DataType;
	}

	public void setDataType(DataTypeEnum dataType) {
		DataType = dataType;
	}

	public int getWeightG() {
		return WeightG;
	}

	public void setWeightG(int weightG) {
		WeightG = weightG;
	}

	public int getWeightML() {
		return WeightML;
	}

	public void setWeightML(int weightML) {
		WeightML = weightML;
	}

	public boolean isIfStable() {
		return IfStable;
	}
	public void setIfStable(boolean ifStable) {
		IfStable = ifStable;
	}

	public boolean isSymbol() {
		return symbol;
	}

	public void setSymbol(boolean symbol) {
		this.symbol = symbol;
	}
	 
	
	 
}
