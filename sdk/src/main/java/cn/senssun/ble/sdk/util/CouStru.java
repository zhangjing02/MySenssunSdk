//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package cn.senssun.ble.sdk.util;

import java.math.BigDecimal;

public class CouStru {
    public CouStru() {
    }

    public static int GetAMR(int activity, int sex, int bmr) {
        switch(activity) {
            case 0:
            case 1:
            default:
                return (new BigDecimal((double)bmr * 1.15D)).setScale(0, 4).intValue();
            case 2:
                return sex == 2?(new BigDecimal((double)bmr * 1.35D)).setScale(0, 4).intValue():(new BigDecimal((double)bmr * 1.4D)).setScale(0, 4).intValue();
            case 3:
                return sex == 2?(new BigDecimal((double)bmr * 1.45D)).setScale(0, 4).intValue():(new BigDecimal((double)bmr * 1.5D)).setScale(0, 4).intValue();
            case 4:
                return sex == 2?(new BigDecimal((double)bmr * 1.7D)).setScale(0, 4).intValue():(new BigDecimal((double)bmr * 1.85D)).setScale(0, 4).intValue();
            case 5:
                return sex == 2?(new BigDecimal(bmr * 2)).setScale(0, 4).intValue():(new BigDecimal((double)bmr * 2.1D)).setScale(0, 4).intValue();
        }
    }

    public static String GetProtein(float fat) {
        return String.format("%.1f", new Object[]{Float.valueOf((100.0F - fat) * 3.0F / 16.0F)}) + "%";
    }

    public static String GetViscera(int sex, String weightKG, String height, int age) {
        return String.valueOf((int)(sex == 1?0.224D * (double)Float.valueOf(weightKG).floatValue() - 25.58D * (double)Float.valueOf(height).floatValue() / 100.0D + 0.151D * (double)age + 31.8D:0.183D * (double)Float.valueOf(weightKG).floatValue() - 15.34D * (double)Float.valueOf(height).floatValue() / 100.0D + 0.063D * (double)age + 16.9D));
    }

    public static int GetBodyAge(int age, float bmi) {
        return (int)((double)age * (1.0D + 0.00313D * (double)age * ((double)bmi - 21.5D)));
    }
}
