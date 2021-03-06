package org.csc.phynixx.watchdog;

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


public interface IWatchedCondition {

    /**
     *
     * @return true if and if the condition is NOT violated
     */
    public boolean checkCondition();

    /**
     * callback if the condition is vioalated
     */
    public void conditionViolated();

    /**
     * activate/deactivate the condition
     * @param active state to be set
     */
    public void setActive(boolean active);

    public boolean isActive();


    /**
     * indicates that the condition isn't needed any longer ...
     *
     * @return
     */
    public boolean isUseless();


}
