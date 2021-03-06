package org.csc.phynixx.watchdog.objectref;

/*
 * #%L
 * phynixx-watchdog
 * %%
 * Copyright (C) 2014 csc
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


import java.lang.ref.WeakReference;


/**
 * the watchdog references the conditions weakly. If any condition is not referenced
 * by anybody but the watchdog it shut be handed to gc.
 *
 * @author Christoph Schmidt-Casdorff
 */

public class WeakObjectReference<T>  extends WeakReference<T> implements IObjectReference<T> {

    private String description = null;

    public WeakObjectReference(T objectRef) {
        super(objectRef);
        this.description = (objectRef == null) ? "NULL" : objectRef.toString();
    }


    public String getObjectDescription() {
        return this.description;
    }


    public boolean isWeakReference() {
        return true;
    }


    public boolean isStale() {
        return this.get() == null;
    }

    public boolean equals(Object obj) {
        Object objRef = this.get();
        if (objRef == null) {
            return obj == null;
        }
        return objRef.equals(obj);
    }

    public int hashCode() {
        Object objRef = this.get();
        if (objRef == null) {
            return "".hashCode();
        }
        return objRef.hashCode();
    }

    public String toString() {
        Object objRef = this.get();
        if (objRef == null) {
            return "";
        }
        return objRef.toString();
    }

}
