package cn.senssun.ble.sdk.entity;

import java.io.Serializable;
import java.util.Date;


public class GrowMeasure implements Serializable{
	private static final long serialVersionUID = 1L;
	
	 public enum DataTypeEnum  {
		DataTypeWeigh(0),DataTypeHistory(1);
	        private final int value;
	        public int getValue() {
	            return value;
	        }
	        DataTypeEnum(int value) {
	            this.value = value;
	        }
	    }
	 
	 private DataTypeEnum  DataType=DataTypeEnum.DataTypeWeigh; 
	 
	 public enum UnitTypeEnum  {
		 KgCmUnit(0),LbInchUnit(1),OzInchUnit(2),LbOzInchUnit(3);
	        private final int value;
	        public int getValue() {
	            return value;
	        }
	        UnitTypeEnum(int value) {
	            this.value = value;
	        }
	    }
	 
	 private UnitTypeEnum  UnitType=UnitTypeEnum.KgCmUnit; 
	 private int WeightKg=-1;
	 private int WeightLb=-1;
	 private int HeightCm=-1;
	 private boolean IfStable;
	 private boolean symbol;
	 private int Serimal=-1;
	 
	 private Date HistoryDate=null;
	 private int HistoryWeightKg=-1;
	 private int HistoryHeightCm=-1;
	 private int HistoryUserSerimal=-1;
	 private int HistoryDataSerimal=-1;
	 private int HistoryDataAmount=-1;
	 

	public DataTypeEnum getDataType() {
		return DataType;
	}

	public void setDataType(DataTypeEnum dataType) {
		DataType = dataType;
	}

	public UnitTypeEnum getUnitType() {
		return UnitType;
	}

	public void setUnitType(UnitTypeEnum unitType) {
		UnitType = unitType;
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

	public int getHeightCm() {
		return HeightCm;
	}

	public void setHeightCm(int heightCm) {
		HeightCm = heightCm;
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

	
	
	public int getSerimal() {
		return Serimal;
	}

	public void setSerimal(int serimal) {
		Serimal = serimal;
	}

	public Date getHistoryDate() {
		return HistoryDate;
	}

	public void setHistoryDate(Date historyDate) {
		HistoryDate = historyDate;
	}

	public int getHistoryWeightKg() {
		return HistoryWeightKg;
	}

	public void setHistoryWeightKg(int historyWeightKg) {
		HistoryWeightKg = historyWeightKg;
	}

	public int getHistoryHeightCm() {
		return HistoryHeightCm;
	}

	public void setHistoryHeightCm(int historyHeightCm) {
		HistoryHeightCm = historyHeightCm;
	}

	public int getHistoryUserSerimal() {
		return HistoryUserSerimal;
	}

	public void setHistoryUserSerimal(int historyUserSerimal) {
		HistoryUserSerimal = historyUserSerimal;
	}

	public int getHistoryDataSerimal() {
		return HistoryDataSerimal;
	}

	public void setHistoryDataSerimal(int historyDataSerimal) {
		HistoryDataSerimal = historyDataSerimal;
	}

	public int getHistoryDataAmount() {
		return HistoryDataAmount;
	}

	public void setHistoryDataAmount(int historyDataAmount) {
		HistoryDataAmount = historyDataAmount;
	}
	
	
}
