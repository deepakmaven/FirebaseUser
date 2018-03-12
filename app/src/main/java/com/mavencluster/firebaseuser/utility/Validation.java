package com.mavencluster.firebaseuser.utility;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Created by Deepak on 21/02/2018.
 * Class to define all the validations used in the project.
 */
public class Validation {

    /**
     * Method to validate email addresses
     * This method will return true if entered string is valid email.
     *
     * @param email String parameter to validate as valid email.
     * @return boolean
     */
    public static boolean isValidEmail(String email) {
        return (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }

    /**
     * Method to validate mobile number.
     * This method will return true if entered string is valid mobile number.     *
     *
     * @param mobile String parameter to validate as valid mobile.
     * @return boolean
     */
    public static boolean isValidMobile(String mobile) {
        return (!TextUtils.isEmpty(mobile) && mobile.length() == 10);
    }

    /**
     * Method to validate password
     * This method will return true if entered string is valid password.
     *
     * @param password String parameter to validate as valid email.
     * @return boolean
     */
    public static boolean isValidPassword(String password) {
        return (!TextUtils.isEmpty(password) && password.length() > 6);
    }
}
