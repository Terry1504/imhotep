/*
 * Copyright (C) 2015 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indeed.imhotep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
    Intended for course-grained instrumentation of Imhotep components. Note that this class is built
    for comfort, not for speed.
 */
public class Instrumentation {

    public static class Event {
        private final String TYPE_KEY = "type"; // Note that this property key is reserved.

        private final TreeMap<String, Object> properties = new TreeMap<String, Object>();

        public Event(final String type) { properties.put(TYPE_KEY, type); }

        public String getType() { return properties.get(TYPE_KEY).toString(); }

        public Map<String, Object> getProperties() { return properties; }

        public String toString() { return new JSON().format(getProperties()); }

        private final static class JSON {

            static final String BEGIN_OBJ    = "{ ";
            static final String END_OBJ      = " }";
            static final String QUOTE        = "\"";
            static final String SEPARATOR    = ", ";
            static final String KV_SEPARATOR = " : ";

            String format(Object value) {
                StringBuilder result = new StringBuilder();
                result.append(QUOTE);
                result.append(value != null ? value.toString() : "(null)");
                result.append(QUOTE);
                return result.toString();
            }

            String format(Map.Entry<String, Object> entry) {
                StringBuilder result = new StringBuilder();
                result.append(format(entry.getKey()));
                result.append(KV_SEPARATOR);
                result.append(format(entry.getValue()));
                return result.toString();
            }

            String format(Map<String, Object> map) {
                StringBuilder result = new StringBuilder();
                result.append(BEGIN_OBJ);
                final Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    result.append(format(it.next()));
                    if (it.hasNext()) result.append(SEPARATOR);
                }
                result.append(END_OBJ);
                return result.toString();
            }
        }
    }

    public interface Observer {
        void onEvent(final Event event);
    }

    public interface Provider {
        /** Note: You can add the same observer multiple times, but
            you will receive multiple events when they fire. */
        void addObserver(final Observer observer);

        /** Note: Attempting to remove a non-existent observer will fail
            silently.
            Note: Removing a multiply registered observer will only eliminate
            one copy. */
        void removeObserver(final Observer observer);
    }

    public static class ProviderSupport implements Provider {

        private final ArrayList<Observer> observers = new ArrayList<Observer>();

        public synchronized void    addObserver(Observer observer) { observers.add(observer);    }
        public synchronized void removeObserver(Observer observer) { observers.remove(observer); }

        public synchronized void fire(final Event event) {
            for (Observer observer: observers) {
                observer.onEvent(event);
            }
        }
    }
}