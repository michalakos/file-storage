package com.mvasilakos.filestorage.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Password validator.
 */
public class PasswordValidator {

  private static final Pattern passwordPattern;

  static {
    String regEx = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&+=])(?=\\S+$).{8,20}$";
    passwordPattern = Pattern.compile(
        regEx); // Removed Pattern.CASE_INSENSITIVE as it's not in your regex
  }

  /**
   * Ensures the password has at least one number, one lowercase letter, one uppercase letter, one
   * special character and a length from 8 to 20 characters.
   *
   * @param password password to validate
   * @return true if password conforms to the requirements
   */
  public static boolean validatePassword(String password) {
    Matcher matcher = passwordPattern.matcher(password);
    return matcher.matches();
  }
}