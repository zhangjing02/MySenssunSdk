package cn.senssun.ble.sdk.entity;

import java.io.Serializable;

public class FatMeasure implements Serializable {
	private static final long serialVersionUID = 1L;
	
	 public enum DataTypeEnum  {
		 DataTypeNone(-1), DataTypeWeigh(0),DatatypeTestFat(1), DataTypeHistory(2);
	        private final int value;
	        public int getValue() {
	            return value;
	        }
	        DataTypeEnum(int value) {
	            this.value = value;
	        }
	    }
	 
	 private DataTypeEnum  DataType=DataTypeEnum.DataTypeNone; 
	 private int WeightKg=-1;
	 private int WeightLb=-1;
	 private boolean IfStable=false;
	 
	 /*12项数据 
	 体重、	 脂肪、骨骼、水分、
	 肌肉、	 蛋白质、基础代谢、健康评分、
	 身体年龄、去脂体重（通过脂肪率计算）、BMI（通过身高体重计算）、内脏脂肪指数
	 */
	 private int Fat=-1;//脂肪
	 private int Bone=-1;//骨骼
	 private int Hydration=-1;//水分
	 private int Muscle=-1;//肌肉
	 private int Protein=-1;//蛋白质
	 private int Kcal=-1;// 基础代谢
	 private int HealthGrade=-1;//健康评分 //水分
	 private int BodyAge;//身体年龄
	 private int LBM;//去脂体重（通过脂肪率计算）
	 private int BMI;//BMI（通过身高体重计算）
	 private int UVI;//内脏脂肪指数
	 
	 private int UserID=-1;
	 private int Number=-1;
	 private String HistoryDate=null;
	 private int HistoryWeightKg=-1;
	 private int HistoryWeightLb=-1;
	 private int HistoryFat=-1;
	 private int HistoryHydration=-1;
	 private int HistoryMuscle=-1;
	 private int HistoryBone=-1;
	 private int HistoryKcal=-1;
	 
	 
	 public FatMeasure(){}
	 public FatMeasure(FatMeasure tmp) {
		DataType = tmp.getDataType();
		WeightKg = tmp.getWeightKg();
		WeightLb = tmp.getWeightLb();
		IfStable = tmp.isIfStable();
		Fat = tmp.getFat();
		Hydration = tmp.getHydration();
		Muscle = tmp.getMuscle();
		Bone = tmp.getBone();
		Kcal = tmp.getKcal();
		UserID = tmp.getUserID();
		Number = tmp.getNumber();
		HistoryDate = tmp.getHistoryDate();
		HistoryWeightKg = tmp.getHistoryWeightKg();
		HistoryWeightLb = tmp.getHistoryWeightLb();
		HistoryFat = tmp.getHistoryFat();
		HistoryHydration = tmp.getHistoryHydration();
		HistoryMuscle = tmp.getHistoryMuscle();
		HistoryBone = tmp.getHistoryBone();
		HistoryKcal = tmp.getHistoryKcal();
	}


	public void empty(){
		  WeightKg=-1;
		  WeightLb=-1;
		  IfStable=false;
		 
		  Fat=-1;
		  Hydration=-1;
		  Muscle=-1;
		  Bone=-1;
		  Kcal=-1;
		 
		  Number=-1;
		  HistoryDate=null;
		  HistoryWeightKg=-1;
		  HistoryWeightLb=-1;
		  HistoryFat=-1;
		  HistoryHydration=-1;
		  HistoryMuscle=-1;
		  HistoryBone=-1;
		  HistoryKcal=-1;
	 }
	 
	public DataTypeEnum getDataType() {
		return DataType;
	}
	public void setDataType(DataTypeEnum dataType) {
		DataType = dataType;
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
	public int getFat() {
		return Fat;
	}
	public void setFat(int fat) {
		Fat = fat;
	}
	public int getHydration() {
		return Hydration;
	}
	public void setHydration(int hydration) {
		Hydration = hydration;
	}
	public int getMuscle() {
		return Muscle;
	}
	public void setMuscle(int muscle) {
		Muscle = muscle;
	}
	public int getBone() {
		return Bone;
	}
	public void setBone(int bone) {
		Bone = bone;
	}
	public int getKcal() {
		return Kcal;
	}
	public void setKcal(int kcal) {
		Kcal = kcal;
	}
	public int getUserID() {
		return UserID;
	}
	public void setUserID(int userID) {
		UserID = userID;
	}
	public int getNumber() {
		return Number;
	}
	public void setNumber(int number) {
		Number = number;
	}
	public String getHistoryDate() {
		return HistoryDate;
	}
	public void setHistoryDate(String historyDate) {
		HistoryDate = historyDate;
	}
	public int getHistoryWeightKg() {
		return HistoryWeightKg;
	}
	public void setHistoryWeightKg(int historyWeightKg) {
		HistoryWeightKg = historyWeightKg;
	}
	public int getHistoryWeightLb() {
		return HistoryWeightLb;
	}
	public void setHistoryWeightLb(int historyWeightLb) {
		HistoryWeightLb = historyWeightLb;
	}
	public int getHistoryFat() {
		return HistoryFat;
	}
	public void setHistoryFat(int historyFat) {
		HistoryFat = historyFat;
	}
	public int getHistoryHydration() {
		return HistoryHydration;
	}
	public void setHistoryHydration(int historyHydration) {
		HistoryHydration = historyHydration;
	}
	public int getHistoryMuscle() {
		return HistoryMuscle;
	}
	public void setHistoryMuscle(int historyMuscle) {
		HistoryMuscle = historyMuscle;
	}
	public int getHistoryBone() {
		return HistoryBone;
	}
	public void setHistoryBone(int historyBone) {
		HistoryBone = historyBone;
	}
	public int getHistoryKcal() {
		return HistoryKcal;
	}
	public void setHistoryKcal(int historyKcal) {
		HistoryKcal = historyKcal;
	}
	public int getProtein() {
		return Protein;
	}
	public void setProtein(int protein) {
		Protein = protein;
	}
	public int getHealthGrade() {
		return HealthGrade;
	}
	public void setHealthGrade(int healthGrade) {
		HealthGrade = healthGrade;
	}
	public int getBodyAge() {
		return BodyAge;
	}
	public void setBodyAge(int bodyAge) {
		BodyAge = bodyAge;
	}
	public int getLBM() {
		return LBM;
	}
	public void setLBM(int lBM) {
		LBM = lBM;
	}
	public int getBMI() {
		return BMI;
	}
	public void setBMI(int bMI) {
		BMI = bMI;
	}
	public int getUVI() {
		return UVI;
	}
	public void setUVI(int uVI) {
		UVI = uVI;
	}
	 
}
