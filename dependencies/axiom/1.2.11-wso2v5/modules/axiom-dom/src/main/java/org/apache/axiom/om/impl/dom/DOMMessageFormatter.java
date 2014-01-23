/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axiom.om.impl.dom;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/** Used to format DOM error messages, using the system locale. */
public class DOMMessageFormatter {
    public static final String DOM_DOMAIN = "http://www.w3.org/dom/DOMTR";

    public static final String XML_DOMAIN =
            "http://www.w3.org/TR/1998/REC-xml-19980210";

    public static final String SERIALIZER_DOMAIN =
            "http://apache.org/xml/serializer";

    private static ResourceBundle domResourceBundle = null;

    private static ResourceBundle xmlResourceBundle = null;

    private static ResourceBundle serResourceBundle = null;

    private static Locale locale = null;

    public static final String LEVEL3_NOT_SUPPORTED =
            "DOM Level 3 operations are not supported";

    public static final String NOT_REQUIRED_FOR_XMLSEC_OR_WSS4J =
            "This method is not required by Apache XML-Security Impl or WSS4J";

    private static final String[] exceptionKeys = {
        // Note: order is determined by the constants defined in DOMException
        "INDEX_SIZE_ERR",
        "DOMSTRING_SIZE_ERR",
        "HIERARCHY_REQUEST_ERR",
        "WRONG_DOCUMENT_ERR",
        "INVALID_CHARACTER_ERR",
        "NO_DATA_ALLOWED_ERR",
        "NO_MODIFICATION_ALLOWED_ERR",
        "NOT_FOUND_ERR",
        "NOT_SUPPORTED_ERR",
        "INUSE_ATTRIBUTE_ERR",
        "INVALID_STATE_ERR",
        "SYNTAX_ERR",
        "INVALID_MODIFICATION_ERR",
        "NAMESPACE_ERR",
        "INVALID_ACCESS_ERR",
        "VALIDATION_ERR",
        "TYPE_MISMATCH_ERR",
    };

    DOMMessageFormatter() {
        locale = Locale.getDefault();
    }

    /**
     * Formats a message with the specified arguments using the given locale information.
     *
     * @param domain    domain from which error string is to come.
     * @param key       The message key.
     * @param arguments The message replacement text arguments. The order of the arguments must
     *                  match that of the placeholders in the actual message.
     * @return Returns the formatted message.
     * @throws MissingResourceException Thrown if the message with the specified key cannot be
     *                                  found.
     */
    public static String formatMessage(String domain, String key,
                                       Object[] arguments) throws MissingResourceException {
        ResourceBundle resourceBundle = getResourceBundle(domain);
        if (resourceBundle == null) {
            init();
            resourceBundle = getResourceBundle(domain);
            if (resourceBundle == null)
                throw new MissingResourceException("Unknown domain" + domain,
                                                   null, key);
        }
        // format message
        String msg;
        try {
            msg = key + ": " + resourceBundle.getString(key);
            if (arguments != null) {
                try {
                    msg = java.text.MessageFormat.format(msg, arguments);
                } catch (Exception e) {
                    msg = resourceBundle.getString("FormatFailed");
                    msg += " " + resourceBundle.getString(key);
                }
            }
        } // error
        catch (MissingResourceException e) {
            msg = resourceBundle.getString("BadMessageKey");
            throw new MissingResourceException(key, msg, key);
        }

        // no message
        if (msg == null) {
            msg = key;
            if (arguments.length > 0) {
                StringBuffer str = new StringBuffer(msg);
                str.append('?');
                for (int i = 0; i < arguments.length; i++) {
                    if (i > 0) {
                        str.append('&');
                    }
                    str.append(String.valueOf(arguments[i]));
                }
            }
        }

        return msg;
    }

    public static String formatMessage(String domain, int exceptionId,
                                       Object[] arguments) throws MissingResourceException {
        return formatMessage(domain, exceptionKeys[exceptionId-1], arguments);
    }
    
    static ResourceBundle getResourceBundle(String domain) {
        if (domain == DOM_DOMAIN || domain.equals(DOM_DOMAIN))
            return domResourceBundle;
        else if (domain == XML_DOMAIN || domain.equals(XML_DOMAIN))
            return xmlResourceBundle;
        else if (domain == SERIALIZER_DOMAIN
                || domain.equals(SERIALIZER_DOMAIN))
            return serResourceBundle;
        return null;
    }

    /** Initializes Message Formatter. */
    public static void init() {
        if (locale != null) {
            domResourceBundle = PropertyResourceBundle.getBundle(
                    "org.apache.axiom.om.impl.dom.msg.DOMMessages", locale);
            serResourceBundle = PropertyResourceBundle.getBundle(
                    "org.apache.axiom.om.impl.dom.msg.XMLSerializerMessages",
                    locale);
            xmlResourceBundle = PropertyResourceBundle.getBundle(
                    "org.apache.axiom.om.impl.dom.msg.XMLMessages", locale);
        } else {
            domResourceBundle = PropertyResourceBundle
                    .getBundle("org.apache.axiom.om.impl.dom.msg.DOMMessages");
            serResourceBundle = PropertyResourceBundle
                    .getBundle("org.apache.axiom.om.impl.dom.msg.XMLSerializerMessages");
            xmlResourceBundle = PropertyResourceBundle
                    .getBundle("org.apache.axiom.om.impl.dom.msg.XMLMessages");
        }
    }

    /**
     * Sets Locale to be used by the formatter.
     *
     * @param dlocale
     */
    public static void setLocale(Locale dlocale) {
        locale = dlocale;
    }
}