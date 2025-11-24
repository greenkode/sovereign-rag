package ai.sovereignrag.accounting;/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2024 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import ai.sovereignrag.commons.enumeration.ResponseCode;

/**
 * Base class for MiniGL especific exceptions
 *
 * @author <a href="mailto:apr@jpos.org">Alejandro Revilla</a>
 */
public class GLException extends Exception {

    private final ResponseCode responseCode;

    /**
     * Constructs a new GLException
     */
    public GLException(ResponseCode code) {
        super();
        responseCode = code;
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param s the detail message.
     */
    public GLException(String s, ResponseCode code) {
        super(s);
        responseCode = code;
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause the cause
     */
    public GLException(Exception cause, ResponseCode code) {
        super(cause);
        responseCode = code;
    }

    /**
     * Constructs a new exception with the specified detail and cause.
     *
     * @param s     the detail message.
     * @param cause the cause
     */
    public GLException(String s, Exception cause, ResponseCode code) {
        super(s, cause);
        responseCode = code;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}

