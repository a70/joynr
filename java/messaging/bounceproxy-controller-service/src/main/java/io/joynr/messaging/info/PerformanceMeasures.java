package io.joynr.messaging.info;

/*
 * #%L
 * joynr::java::messaging::bounceproxy-controller-service
 * %%
 * Copyright (C) 2011 - 2013 BMW Car IT GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.HashMap;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Holds a map of performance measures for bounce proxies.
 * 
 * Performance measures are meant to be simple integer values that represent
 * some kind of load.
 * 
 * @author christina.strobel
 * 
 */
public class PerformanceMeasures {

    public enum Key {

        /**
         * The number of long polls that are handled by the bounce proxy.
         */
        ACTIVE_LONGPOLL_COUNT("activeLongPolls"),

        /**
         * The number of channels the bounce proxy has assigned.
         */
        ASSIGNED_CHANNELS_COUNT("assignedChannels");

        private String name;

        private Key(String name) {
            this.name = name;
        }

        /**
         * Creates a {@link Key} object from a string. This is used for
         * performance measure keys that are handed over e.g. in the query
         * string of a URL where a {@link Key} object as parameter is not
         * possible or not handy.
         * 
         * @param name
         *            the name of the {@link Key} object
         * @return the matching {@link Key} object or <code>null</code> if
         *         there's no such key.
         */
        @SuppressWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "Ignore unknown keys until it is specified which performance measures are used and how stable their definitions are")
        public static Key fromString(String name) {
            for (Key key : values()) {
                if (key.name.equals(name)) {
                    return key;
                }
            }
            return null;
        }
    }

    private HashMap<Key, Integer> measures = new HashMap<Key, Integer>();

    /**
     * Adds a measure. This is a convenient method for
     * {@code addMeasure(k.toString(), i)}. If the key does not exist, for now
     * it is simply ignored without any warning.
     * 
     * @param key
     *            one of the available keys {@link Key} as string.
     * @param value
     */
    public void addMeasure(String key, int value) {

        Key k = Key.fromString(key);

        if (k != null) {
            measures.put(k, value);
        } else {
            // TODO for now, we just ignore the value
        }
    }

    /**
     * Adds a measure. If the key is <code>null</code>, the measure is simply
     * ignored without any warning.
     * 
     * @param key
     * @param value
     */
    public void addMeasure(Key key, int value) {

        if (key != null) {
            measures.put(key, value);
        } else {
            // TODO for now, we just ignore the value
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof PerformanceMeasures)) {
            return false;
        }

        PerformanceMeasures p = (PerformanceMeasures) obj;

        return p.measures.equals(measures);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((measures == null) ? 0 : measures.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "PerformanceMeasures [measures=" + measures + "]";
    }

}
