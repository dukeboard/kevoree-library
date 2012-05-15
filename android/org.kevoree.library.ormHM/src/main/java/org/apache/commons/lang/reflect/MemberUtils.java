/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;

/**
 * Contains common code for working with Methods/Constructors, extracted and refactored from <code>MethodUtils</code> when it was imported from
 * Commons BeanUtils.
 * 
 * @author Apache Software Foundation
 * @author Steve Cohen
 * @author Matt Benson
 * @since 2.5
 * @version $Id: MemberUtils.java 905636 2010-02-02 14:03:32Z niallp $
 */
abstract class MemberUtils {
    // TODO extract an interface to implement compareParameterSets(...)?

    private static final int ACCESS_TEST = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    private static final Method IS_SYNTHETIC;
    static {
        Method isSynthetic = null;
        if (isJavaVersionAtLeast(1.5f)) {
            // cannot call synthetic methods:
            try {
                isSynthetic = Member.class.getMethod("isSynthetic", ArrayUtils.EMPTY_CLASS_ARRAY);
            } catch (Exception e) {
            }
        }
        IS_SYNTHETIC = isSynthetic;
    }

    /** Array of primitive number types ordered by "promotability" */
    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES = { Byte.TYPE, Short.TYPE, Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE,
            Double.TYPE };

    // -----------------------------------------------------------------------
    // SystemUtils
    // -----------------------------------------------------------------------
    /**
     * <p>
     * The <code>java.version</code> System Property. Java version number.
     * </p>
     * 
     * <p>
     * Defaults to <code>null</code> if the runtime does not have security access to read this property or the property does not exist.
     * </p>
     * 
     * <p>
     * This value is initialized when the class is loaded. If {@link System#setProperty(String,String)} or
     * {@link System#setProperties(java.util.Properties)} is called after this class is loaded, the value will be out of sync with that System
     * property.
     * </p>
     * 
     * @since Java 1.1
     */
    public static final String JAVA_VERSION = getSystemProperty("java.version");

    // Java version values
    //-----------------------------------------------------------------------
    // These MUST be declared after the trim above as they depend on the
    // value being set up

    /**
     * <p>
     * Gets the Java version as a <code>String</code> trimming leading letters.
     * </p>
     * 
     * <p>
     * The field will return <code>null</code> if {@link #JAVA_VERSION} is <code>null</code>.
     * </p>
     * 
     * @since 2.1
     */
    public static final String JAVA_VERSION_TRIMMED = getJavaVersionTrimmed();

    // Java version values
    //-----------------------------------------------------------------------
    // These MUST be declared after the trim above as they depend on the
    // value being set up

    /**
     * <p>
     * Gets the Java version as a <code>float</code>.
     * </p>
     * 
     * <p>
     * Example return values:
     * </p>
     * <ul>
     * <li><code>1.2f</code> for JDK 1.2
     * <li><code>1.31f</code> for JDK 1.3.1
     * </ul>
     * 
     * <p>
     * The field will return zero if {@link #JAVA_VERSION} is <code>null</code>.
     * </p>
     * 
     * @since 2.0
     */
    public static final float JAVA_VERSION_FLOAT = getJavaVersionAsFloat();

    /**
     * <p>
     * Gets a System property, defaulting to <code>null</code> if the property cannot be read.
     * </p>
     * 
     * <p>
     * If a <code>SecurityException</code> is caught, the return value is <code>null</code> and a message is written to <code>System.err</code>.
     * </p>
     * 
     * @param property
     *            the system property name
     * @return the system property value or <code>null</code> if a security problem occurs
     */
    private static String getSystemProperty(String property) {
        try {
            return System.getProperty(property);
        } catch (SecurityException ex) {
            // we are not allowed to look at this property
            System.err.println("Caught a SecurityException reading the system property '" + property
                    + "'; the SystemUtils property value will default to null.");
            return null;
        }
    }

    /**
     * Trims the text of the java version to start with numbers.
     * 
     * @return the trimmed java version
     */
    private static String getJavaVersionTrimmed() {
        if (JAVA_VERSION != null) {
            for (int i = 0; i < JAVA_VERSION.length(); i++) {
                char ch = JAVA_VERSION.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    return JAVA_VERSION.substring(i);
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Gets the Java version number as a <code>float</code>.
     * </p>
     * 
     * <p>
     * Example return values:
     * </p>
     * <ul>
     * <li><code>1.2f</code> for JDK 1.2
     * <li><code>1.31f</code> for JDK 1.3.1
     * </ul>
     * 
     * <p>
     * Patch releases are not reported. Zero is returned if {@link #JAVA_VERSION_TRIMMED} is <code>null</code>.
     * </p>
     * 
     * @return the version, for example 1.31f for JDK 1.3.1
     */
    private static float getJavaVersionAsFloat() {
        if (JAVA_VERSION_TRIMMED == null || JAVA_VERSION_TRIMMED.equals("0")) {
            return 0f;
        }
        String str = JAVA_VERSION_TRIMMED.substring(0, 3);
        if (JAVA_VERSION_TRIMMED.length() >= 5) {
            str = str + JAVA_VERSION_TRIMMED.substring(4, 5);
        }
        try {
            return Float.parseFloat(str);
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * <p>
     * Is the Java version at least the requested version.
     * </p>
     * 
     * <p>
     * Example input:
     * </p>
     * <ul>
     * <li><code>1.2f</code> to test for JDK 1.2</li>
     * <li><code>1.31f</code> to test for JDK 1.3.1</li>
     * </ul>
     * 
     * @param requiredVersion
     *            the required version, for example 1.31f
     * @return <code>true</code> if the actual version is equal or greater than the required version
     */
    public static boolean isJavaVersionAtLeast(float requiredVersion) {
        return JAVA_VERSION_FLOAT >= requiredVersion;
    }

    // -----------------------------------------------------------------------

    /**
     * XXX Default access superclass workaround
     * 
     * When a public class has a default access superclass with public members, these members are accessible. Calling them from compiled code works
     * fine. Unfortunately, on some JVMs, using reflection to invoke these members seems to (wrongly) to prevent access even when the modifer is
     * public. Calling setAccessible(true) solves the problem but will only work from sufficiently privileged code. Better workarounds would be
     * gratefully accepted.
     * 
     * @param o
     *            the AccessibleObject to set as accessible
     */
    static void setAccessibleWorkaround(AccessibleObject o) {
        if (o == null || o.isAccessible()) {
            return;
        }
        Member m = (Member) o;
        if (Modifier.isPublic(m.getModifiers()) && isPackageAccess(m.getDeclaringClass().getModifiers())) {
            try {
                o.setAccessible(true);
            } catch (SecurityException e) {
                // ignore in favor of subsequent IllegalAccessException
            }
        }
    }

    /**
     * Learn whether a given set of modifiers implies package access.
     * 
     * @param modifiers
     *            to test
     * @return true unless package/protected/private modifier detected
     */
    static boolean isPackageAccess(int modifiers) {
        return (modifiers & ACCESS_TEST) == 0;
    }

    /**
     * Check a Member for basic accessibility.
     * 
     * @param m
     *            Member to check
     * @return true if <code>m</code> is accessible
     */
    static boolean isAccessible(Member m) {
        return m != null && Modifier.isPublic(m.getModifiers()) && !isSynthetic(m);
    }

    /**
     * Try to learn whether a given member, on JDK >= 1.5, is synthetic.
     * 
     * @param m
     *            Member to check
     * @return true if <code>m</code> was introduced by the compiler.
     */
    static boolean isSynthetic(Member m) {
        if (IS_SYNTHETIC != null) {
            try {
                return ((Boolean) IS_SYNTHETIC.invoke(m, (Object[]) null)).booleanValue();
            } catch (Exception e) {
            }
        }
        return false;
    }

    /**
     * Compare the relative fitness of two sets of parameter types in terms of matching a third set of runtime parameter types, such that a list
     * ordered by the results of the comparison would return the best match first (least).
     * 
     * @param left
     *            the "left" parameter set
     * @param right
     *            the "right" parameter set
     * @param actual
     *            the runtime parameter types to match against <code>left</code>/<code>right</code>
     * @return int consistent with <code>compare</code> semantics
     */
    static int compareParameterTypes(Class<?>[] left, Class<?>[] right, Class<?>[] actual) {
        float leftCost = getTotalTransformationCost(actual, left);
        float rightCost = getTotalTransformationCost(actual, right);
        return leftCost < rightCost ? -1 : rightCost < leftCost ? 1 : 0;
    }

    /**
     * Returns the sum of the object transformation cost for each class in the source argument list.
     * 
     * @param srcArgs
     *            The source arguments
     * @param destArgs
     *            The destination arguments
     * @return The total transformation cost
     */
    private static float getTotalTransformationCost(Class<?>[] srcArgs, Class<?>[] destArgs) {
        float totalCost = 0.0f;
        for (int i = 0; i < srcArgs.length; i++) {
            Class<?> srcClass, destClass;
            srcClass = srcArgs[i];
            destClass = destArgs[i];
            totalCost += getObjectTransformationCost(srcClass, destClass);
        }
        return totalCost;
    }

    /**
     * Gets the number of steps required needed to turn the source class into the destination class. This represents the number of steps in the object
     * hierarchy graph.
     * 
     * @param srcClass
     *            The source class
     * @param destClass
     *            The destination class
     * @return The cost of transforming an object
     */
    private static float getObjectTransformationCost(Class<?> srcClass, Class<?> destClass) {
        if (destClass.isPrimitive()) {
            return getPrimitivePromotionCost(srcClass, destClass);
        }
        float cost = 0.0f;
        while (destClass != null && !destClass.equals(srcClass)) {
            if (destClass.isInterface() && ClassUtils.isAssignable(srcClass, destClass)) {
                // slight penalty for interface match.
                // we still want an exact match to override an interface match,
                // but
                // an interface match should override anything where we have to
                // get a superclass.
                cost += 0.25f;
                break;
            }
            cost++;
            destClass = destClass.getSuperclass();
        }
        /*
         * If the destination class is null, we've travelled all the way up to
         * an Object match. We'll penalize this by adding 1.5 to the cost.
         */
        if (destClass == null) {
            cost += 1.5f;
        }
        return cost;
    }

    /**
     * Get the number of steps required to promote a primitive number to another type.
     * 
     * @param srcClass
     *            the (primitive) source class
     * @param destClass
     *            the (primitive) destination class
     * @return The cost of promoting the primitive
     */
    private static float getPrimitivePromotionCost(final Class<?> srcClass, final Class<?> destClass) {
        float cost = 0.0f;
        Class<?> cls = srcClass;
        if (!cls.isPrimitive()) {
            // slight unwrapping penalty
            cost += 0.1f;
            cls = ClassUtils.wrapperToPrimitive(cls);
        }
        for (int i = 0; cls != destClass && i < ORDERED_PRIMITIVE_TYPES.length; i++) {
            if (cls == ORDERED_PRIMITIVE_TYPES[i]) {
                cost += 0.1f;
                if (i < ORDERED_PRIMITIVE_TYPES.length - 1) {
                    cls = ORDERED_PRIMITIVE_TYPES[i + 1];
                }
            }
        }
        return cost;
    }

}
