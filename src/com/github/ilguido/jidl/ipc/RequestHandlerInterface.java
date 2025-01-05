/**
 * RequestHandlerInterface.java
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

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * RequestHandlerInterface
 * An interface that defines how server handles requests.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */
public interface RequestHandlerInterface {
    /**
     * An interface that defines how server handles requests.
     *
     * @param method the requested method
     * @param payload the requested payload, must be a JSON value
     * @return the result, must be a JSON value
     */
    JsonObject handleRequest(String method, JsonObject payload);
}
