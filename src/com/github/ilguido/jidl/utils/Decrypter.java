/**
 * Decrypter.java
 *
 * Copyright (c) 2024 Stefano Guidoni
 *
 * This file is part of jidl.
 *
 * jidl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jidl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jidl.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ilguido.jidl.utils;

import java.util.Base64;
import java.util.concurrent.ExecutionException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypter
 * A class of static functions used to decrypt a AES-128/CBC encrypted string.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class Decrypter {
  /**
   * The cryptographic password.
   */
  private static String pass;

  /**
   * Returns the status of the decrypter.
   *
   * @return <code>true</code> if a valid key is set
   */
  public static boolean isReady() {
    if (pass == null || pass.equals("")) {
      return false;
    }
    
    return true;
  }
  
  /**
   * Sets the password used to generate the secret key.
   *
   * @param inKey a plain text password
   */
  public static void setSecretKey(String inKey) {
    pass = inKey;
  }
  
  /**
   * Decrypts a text encoded with AES-128, with the stored password.  The key is
   * derived from the password and the salt with the PBKDF2 algorithm, 128 
   * iterations and a SHA1 digest. The encoded text is then decrypted with an
   * AES 128 algorithm in CBC mode with PKCS5 padding.
   * When there is not salt, nor the initialization vector, this function
   * returns the plain text.
   *
   * @param inText the encoded text
   * @param inSalt the salt for the password
   * @param inIV the initialization vector
   * @return the decrypted plain text
   * @throws IllegalArgumentException when decryption data are incomplete
   * @throws ExecutionException when the decryption fails
   */
  public static String decrypt(String inText,
                               String inSalt,
                               String inIV) throws IllegalArgumentException,
                                                   ExecutionException {
    if (inSalt == null && inIV == null)
      // no decryption required
      return inText;
     
    if ((inSalt != null && inIV == null) ||
        (inSalt == null && inIV != null) ||
        (inSalt != null && inIV != null && !isReady()))
      // there is probably some mistake here
      throw new IllegalArgumentException("Ambiguous decryption settings");
      
    try {
      // openssl enc -aes-128-cbc -pbkdf2 -iter 128 -md sha1 -k $pass -P
      SecretKeyFactory 
        factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      PBEKeySpec 
        spec = new PBEKeySpec(pass.toCharArray(), 
                              Base64.getDecoder().decode(inSalt), 128, 128);
      SecretKey 
        secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), 
                                  "AES");

      // echo -n $inText | openssl enc -aes-128-cbc -K $secret -iv $inIV -a 
      IvParameterSpec 
        iv = new IvParameterSpec(Base64.getDecoder().decode(inIV));
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secret, iv);
      byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(inText));
      return new String(plainText);
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }
}
