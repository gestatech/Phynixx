package org.csc.phynixx.loggersystem.logrecord;

/*
 * #%L
 * phynixx-common
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


class OrdinalGenerator {

    private Integer current = 1;

    public OrdinalGenerator() {
        this(0);
    }

    public OrdinalGenerator(int start) {
        super();
        this.current = Integer.valueOf(start);
    }

    public int getCurrent() {
        return this.current.intValue();
    }


    /* (non-Javadoc)
     * @see de.csc.xaresource.sample.IResourceIDGenerator#generate()
     */
    public int generate() {
        int cc = current.intValue();
        this.current =++cc;
        return this.getCurrent();
    }


}
