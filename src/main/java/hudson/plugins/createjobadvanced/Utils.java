/*
 * The MIT License
 *
 * Copyright (c) 2012, Dominik Bartholdi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.createjobadvanced;

import java.lang.reflect.Field;

/**
 *
 * @author Dominik Bartholdi (imod)
 *
 */
public class Utils {

    /**
     * Sets the fields value via reflection, this is useful in case there is no setter defined on the target object.
     *
     * @param targetObject
     *            the object to set a field on
     * @param fieldName
     *            the name of the field to be set
     * @param value
     *            the new value for the field
     */
    public static void setField(Object targetObject, String fieldName, Object value, boolean failIfError) {
        try {
            Field f = targetObject.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(targetObject, value);
        } catch (Exception e) {
            if (failIfError) {
                throw new RuntimeException("failed to set field", e);
            } else {
                System.err.println("WARN: failed to set field [" + fieldName + "] on [" + targetObject + "] "
                        + e.getClass() + ": " + e.getMessage());
            }
        }
    }
}
