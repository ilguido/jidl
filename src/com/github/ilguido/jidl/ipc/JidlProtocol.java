/**
 * JidlProtocol.java
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

package com.github.ilguido.jidl.ipc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;

import com.github.cliftonlabs.json_simple.Jsoner;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.ilguido.jidl.ipc.JidlProtocolStatusCode;

import com.github.ilguido.jidl.ipc.JidlProtocolException;

/**
 * JidlProtocol
 * A class to read and write data encapsulated in a Jidl protocol.  It is based
 * on ProcBribdge protocol.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public final class JidlProtocol {
  /**
   * The identification flag.
   */
  private static final byte[] FLAG = { 'j', 'i', 'd', 'l' };
  
  /**
   * Maximum size of the payload.
   */
  private static final int MAX_SIZE = 64 * 1024;

  /**
   * Reads a request from an input stream and returns a map with the request 
   * method as key and the payload as value.
   *
   * @param inStream an input stream
   * @return a <code>Map</code> with the method as a string as key and the 
   *         payload as a JsonObject as value
   * @throws IOException if reading the stream failed
   * @throws JidlProtocolException if the message is malformed
   */
  public static Map.Entry<String, JsonObject> readRequest(InputStream inStream)
    throws IOException, JidlProtocolException {
    Map.Entry<JidlProtocolStatusCode, JsonObject> entry = read(inStream);
    JidlProtocolStatusCode statusCode = entry.getKey();
    JsonObject body = entry.getValue();
    
    if (!JidlProtocolStatusCode.isRequest(statusCode)) {
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INVALID_STATUS_CODE);
    }
    
    String method = body.getString(Keys.METHOD);
    JsonObject payload = body.getMapOrDefault(Keys.PAYLOAD);
    return new AbstractMap.SimpleEntry<>(method, payload);
  }

  /**
   * Reads a response from an input stream and returns a map with the response 
   * status as key and the payload as value.
   *
   * @param inStream an input stream
   * @return a <code>Map</code> with the status as a <code>JidlProtocolStatusCode</code>
   *         as key and the payload as a JsonObject as value
   * @throws IOException if reading the stream failed
   * @throws JidlProtocolException if the message is malformed
   */
  public static Map.Entry<JidlProtocolStatusCode, JsonObject>
                                              readResponse(InputStream inStream)
    throws IOException, JidlProtocolException {
    Map.Entry<JidlProtocolStatusCode, JsonObject> entry = read(inStream);
    JidlProtocolStatusCode statusCode = entry.getKey();
    JsonObject body = entry.getValue();
    
    if (JidlProtocolStatusCode.isGood(statusCode)) {
      return new AbstractMap.SimpleEntry<>(statusCode,
                                           body.getMapOrDefault(Keys.PAYLOAD));
    } else if (JidlProtocolStatusCode.isBad(statusCode)) {
      return new AbstractMap.SimpleEntry<>(statusCode, body);
    } else {
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INVALID_STATUS_CODE);
    }
  }
  
  /**
   * Writes a response with just the status code and a message to an output 
   * stream.  The body of the response is the message, usually an error message.
   *
   * @param inStream the stream to write
   * @param inStatusCode the status code
   * @throws IOException if the writing to stream failed
   */
  public static void writeResponse(OutputStream inStream, 
                                   JidlProtocolStatusCode inStatusCode) 
    throws IOException {
    JsonObject body = new JsonObject();
    if (inStatusCode != null) {
      /* This can be either an error message or another message for a response
       * without payload. */
      body.put(Keys.MESSAGE.getKey(), inStatusCode.textMessage);
      write(inStream, inStatusCode, body);
    } else {
      /* Something went bad and we do not know more. */
      body.put(Keys.MESSAGE.getKey(), Keys.MESSAGE.getValue());
      write(inStream, JidlProtocolStatusCode.BAD_RESPONSE, body);
    }
  }
  
  /**
   * Writes a response with the requested data to an output stream.
   *
   * @param inStream the stream to write
   * @param inPayload the requested data
   * @throws IOException if the writing to stream failed
   */
  public static void writeResponse(OutputStream inStream, 
                                   JsonObject inPayload) 
    throws IOException {
    JsonObject body = new JsonObject();
    if (inPayload != null) {
      /* All okay so far: we have a payload to provide. */
      body.put(Keys.PAYLOAD.getKey(), inPayload);
      try {
        write(inStream, JidlProtocolStatusCode.GOOD_RESPONSE_WITH_PAYLOAD, body);
      } catch (JidlProtocolException jpe) {
        /* If there is an error preparing the response, write a bad response. */
        writeResponse(inStream, jpe.getStatusCode());
      }
    } else {
      /* The operation is done, otherwise there should be an exception, just 
       * there is not a payload.  */
      writeResponse(inStream, JidlProtocolStatusCode.GOOD_RESPONSE);
    }
  }
  
  /**
   * Writes a request to an output stream.
   *
   * @param inOutputStream the stream to write
   * @param inMethod the invoked method
   * @param inPayload the data of the request
   * @throws IOException if writing the data failed 
   */
  public static void writeRequest(OutputStream inOutputStream, 
                                  String inMethod, 
                                  JsonObject inPayload) 
    throws IOException {
    /* Request codes: 0 request, 1 without method, 2 without payload, 3 without
     * method and payload. */
    int rc = 0;
    JsonObject body = new JsonObject();
    
    if (inMethod != null) {
      body.put(Keys.METHOD.getKey(), inMethod);
      rc += 1;
    }
    if (inPayload != null) {
      /* FIXME: is this correct put(key, JsonObject)? */
      body.put(Keys.PAYLOAD.getKey(), inPayload);
      rc += 2;
    }
    
    write(inOutputStream, JidlProtocolStatusCode.fromRawValue(rc), body);
  }
  
  /**
   * Reads an input stream and returns a map with the status code of the
   * protocol and the body of the request as a <code>JsonObject</code>.
   *
   * @param inStream the input stream
   * @return a <code>Map</code> with the status code as key and Json data as 
   *         value
   * @throws IOException if reading the stream failed
   * @throws JidlProtocolException if the message is malformed
   */
  private static Map.Entry<JidlProtocolStatusCode, JsonObject> 
                                                      read(InputStream inStream) 
    throws IOException, JidlProtocolException {
    int b;

    // Bytes 1-4: FLAG
    b = inStream.read();
    if (b == -1 || b != FLAG[0]) 
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_UNRECOGNIZED_PROTOCOL);
    b = inStream.read();
    if (b == -1 || b != FLAG[1]) 
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_UNRECOGNIZED_PROTOCOL);
    b = inStream.read();
    if (b == -1 || b != FLAG[2]) 
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_UNRECOGNIZED_PROTOCOL);
    b = inStream.read();
    if (b == -1 || b != FLAG[3]) 
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_UNRECOGNIZED_PROTOCOL);
      
    // Byte 5: STATUS CODE
    b = inStream.read();
    if (b == -1)
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INCOMPLETE_DATA);
    JidlProtocolStatusCode statusCode = JidlProtocolStatusCode.fromRawValue(b);
    if (statusCode == null) 
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INVALID_STATUS_CODE);

    // Bytes 6-7: LENGTH (little endian)
    int bodyLen;
    b = inStream.read();
    if (b == -1) 
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INCOMPLETE_DATA);
    bodyLen = b;
    b = inStream.read();
    if (b == -1)
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INCOMPLETE_DATA);
    bodyLen |= (b << 8);
    
    // Bytes 8-XXX: JSON OBJECT
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int readCount;
    int restCount = bodyLen;
    byte[] buf = new byte[Math.min(bodyLen, MAX_SIZE)];
    while ((readCount = inStream.read(buf, 0, 
                                      Math.min(buf.length, restCount))) != -1) {
      buffer.write(buf, 0, readCount);
      restCount -= readCount;
      if (restCount == 0) {
        break;
      }
    }

    /* If there is a mismatch between the expected size and the real size, 
     * throw an exception. */
    if (buffer.size() != bodyLen) {
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INCOMPLETE_DATA);
    }

    buffer.flush();
    buf = buffer.toByteArray();

    try {
      String jsonText = new String(buf, StandardCharsets.UTF_8);
      JsonObject body = Jsoner.deserialize(jsonText, new JsonObject());
      return new AbstractMap.SimpleEntry<>(statusCode, body);
    } catch (Exception e) {
      /* Probably the body is not valid Json data. */
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_INVALID_BODY);
    }
  }

  /**
   * Writes to an output stream.
   *
   * @param inOutputStream the stream to write
   * @param inStatusCode the status code of the packet
   * @param inBody the payload of the packet
   * @throws IOException if there is an error writing the data
   * @throws JidlProtocolException if the outgoing packet is malformed
   */
  private static void write(OutputStream inOutputStream, 
                            JidlProtocolStatusCode inStatusCode, 
                            JsonObject inBody)
    throws IOException, JidlProtocolException {
    // Bytes 1-4: FLAG
    inOutputStream.write(FLAG);

    // Byte 5: STATUS CODE
    inOutputStream.write(inStatusCode.rawValue);

    // make json object
    byte[] buf = Jsoner.serialize(inBody).getBytes(StandardCharsets.UTF_8);

    // Bytes 6-7: LENGTH (little endian)
    int len = buf.length;
    /* If the data exceeds the maximum size allowed by the protocol, 
     * throw an exception. */
    if (len > MAX_SIZE)
      throw new JidlProtocolException(JidlProtocolStatusCode.BAD_RESPONSE_BUFFER_OVERFLOW);
    int b0 = len & 0xff;
    int b1 = (len & 0xff00) >> 8;
    inOutputStream.write(b0);
    inOutputStream.write(b1);

    // Bytes 8-XXX: JSON OBJECT
    inOutputStream.write(buf);

    inOutputStream.flush();
  }
  
  /**
   * An enumeration of standard keys.
   */
  private enum Keys implements JsonKey {
    METHOD(null),
    PAYLOAD(null),
    MESSAGE("error");
    
    /**
     * The default value of each key.
     */
    private final Object value;

    /** 
     * Instantiates a JsonKey with the provided value.
     *
     * @param value represents a valid default for the key. 
     */
    Keys(final Object value){
      this.value = value;
    }

    /**
     * Provides the key name as lower case.
     */
    @Override
    public String getKey(){
      return this.name().toLowerCase();
    }

    /**
     * Provides the default value for the key.
     */
    @Override
    public Object getValue(){
      return this.value;
    }
  }
}
