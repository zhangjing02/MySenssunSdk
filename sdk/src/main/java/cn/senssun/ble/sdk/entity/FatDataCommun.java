package cn.senssun.ble.sdk.entity;

import java.io.Serializable;

public class FatDataCommun implements Serializable{
	private static final long serialVersionUID = 1L;
	
	/*12项数据 
	 体重、	 脂肪、骨骼、水分、
	 肌肉、	 蛋白质、基础代谢、健康评分、
	 身体年龄、BMI（通过身高体重计算）、内脏脂肪指数
	 */
	
	 private float WeightKg=-1;
	 private float WeightLb=-1;
	 private boolean IfStable=false;
	 private int serial=-1;//历史序号
	 private float Fat=-1;//脂肪
	 private float Hydration=-1;//水分
	 private float VisceralFat=-1;//内脏脂肪指数
	 private float SubcutaneousFat=-1;//皮下脂肪
	 private float Protein=-1;//蛋白质
	 private int BodyAge=-1;//身体年龄
	 private int HealthGrade=-1;//健康评分 //水分
	 
	 private float Muscle=-1;//肌肉
	 private float Bone=-1;//骨骼
	 
	 private int Kcal=-1;// 基础代谢
	 private float BMI=-1;//BMI（通过身高体重计算）
	 
	 private int Sex=-1;//0：女 1：男
	 private int Age=-1;//年龄
	 private int Height=-1;//身高
	 
	 private int Year=-1;
	 private int Month=-1;
	 private int Day=-1;
	 private int Hour=-1;
	 private int Minutes=-1;
	 private int Seconds=-1;
	 
	public float getWeightKg() {
		return WeightKg;
	}
	public void setWeightKg(float weightKg) {
		WeightKg = weightKg;
	}
	public float getWeightLb() {
		return WeightLb;
	}
	public void setWeightLb(float weightLb) {
		WeightLb = weightLb;
	}
	public boolean isIfStable() {
		return IfStable;
	}
	public void setIfStable(boolean ifStable) {
		IfStable = ifStable;
	}
	public int getSerial() {
		return serial;
	}
	public void setSerial(int serial) {
		this.serial = serial;
	}
	public float getFat() {
		return Fat;
	}
	public void setFat(float fat) {
		Fat = fat;
	}
	public float getHydration() {
		return Hydration;
	}
	public void setHydration(float hydration) {
		Hydration = hydration;
	}
	public float getSubcutaneousFat() {
		return SubcutaneousFat;
	}
	public void setSubcutaneousFat(float subcutaneousFat) {
		SubcutaneousFat = subcutaneousFat;
	}
	public float getVisceralFat() {
		return VisceralFat;
	}
	public void setVisceralFat(float visceralFat) {
		VisceralFat = visceralFat;
	}
	public float getProtein() {
		return Protein;
	}
	public void setProtein(float protein) {
		Protein = protein;
	}
	public int getBodyAge() {
		return BodyAge;
	}
	public void setBodyAge(int bodyAge) {
		BodyAge = bodyAge;
	}
	public int getHealthGrade() {
		return HealthGrade;
	}
	public void setHealthGrade(int healthGrade) {
		HealthGrade = healthGrade;
	}
	public float getMuscle() {
		return Muscle;
	}
	public void setMuscle(float muscle) {
		Muscle = muscle;
	}
	public float getBone() {
		return Bone;
	}
	public void setBone(float bone) {
		Bone = bone;
	}
	public int getKcal() {
		return Kcal;
	}
	public void setKcal(int kcal) {
		Kcal = kcal;
	}
	public float getBMI() {
		return BMI;
	}
	public void setBMI(float bMI) {
		BMI = bMI;
	}
	public int getSex() {
		return Sex;
	}
	public void setSex(int sex) {
		Sex = sex;
	}
	public int getAge() {
		return Age;
	}
	public void setAge(int age) {
		Age = age;
	}
	public int getHeight() {
		return Height;
	}
	public void setHeight(int height) {
		Height = height;
	}
	public int getYear() {
		return Year;
	}
	public void setYear(int year) {
		Year = year;
	}
	public int getMonth() {
		return Month;
	}
	public void setMonth(int month) {
		Month = month;
	}
	public int getDay() {
		return Day;
	}
	public void setDay(int day) {
		Day = day;
	}
	public int getHour() {
		return Hour;
	}
	public void setHour(int hour) {
		Hour = hour;
	}
	public int getMinutes() {
		return Minutes;
	}
	public void setMinutes(int minutes) {
		Minutes = minutes;
	}
	public int getSeconds() {
		return Seconds;
	}
	public void setSeconds(int seconds) {
		Seconds = seconds;
	}
	 
	
	 
}
