package org.csc.phynixx.connection.reference.scenarios;

/*
 * #%L
 * phynixx-connection
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


import junit.framework.TestCase;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.csc.phynixx.common.TmpDirectory;
import org.csc.phynixx.common.logger.PhynixxLogManager;
import org.csc.phynixx.common.logger.PrintLogManager;
import org.csc.phynixx.common.logger.PrintLogger;
import org.csc.phynixx.connection.PhynixxManagedConnectionFactory;
import org.csc.phynixx.connection.PooledPhynixxManagedConnectionFactory;
import org.csc.phynixx.connection.loggersystem.LoggerPerTransactionStrategy;
import org.csc.phynixx.connection.reference.IReferenceConnection;
import org.csc.phynixx.connection.reference.ReferenceConnection;
import org.csc.phynixx.connection.reference.ReferenceConnectionFactory;
import org.csc.phynixx.loggersystem.logger.IDataLoggerFactory;
import org.csc.phynixx.loggersystem.logger.channellogger.FileChannelDataLoggerFactory;
import org.junit.Assert;

import java.io.File;
import java.util.Properties;

public class IntegrationScenarios extends TestCase {

    protected void setUp() throws Exception {
        this.tmpDirectory = new TmpDirectory();
        this.tmpDirectory.clear();

        PhynixxLogManager.setLogManager(new PrintLogManager(PrintLogger.ERROR));
    }

    protected void tearDown() throws Exception {
        this.tmpDirectory.clear();
    }

    TmpDirectory tmpDirectory = null;

    public void testSampleConnectionFactory() throws Exception {
        // instanciate a connection pool
        PhynixxManagedConnectionFactory factory = this.createConnectionFactory();

        IReferenceConnection con = null;
        try {
            // get a connection ....
            con = (IReferenceConnection) factory.getConnection();

            con.setInitialCounter(43);

            con.incCounter(6);
            con.incCounter(-3);

            // increments are performed during commit
            Assert.assertEquals(43, con.getCounter());

            // rollback the connection ....
            con.rollback();
            Assert.assertEquals(43, con.getCounter());

        } finally {
            // release the connection to the pool ....
            if (con != null) {
                con.close();
            }
        }
    }


    public void testSampleConnectionPool() throws Exception {
        // instanciate a connection pool

        PooledPhynixxManagedConnectionFactory factory = createConnectionFactory(5);

        IReferenceConnection con = null;
        try {
            // get a connection ....
            con = (IReferenceConnection) factory.getConnection();

            con.setInitialCounter(43);

            con.incCounter(6);
            con.incCounter(-3);

            // increments are performed during commit
            Assert.assertEquals(43, con.getCounter());

            // rollback the connection ....
            con.rollback();

            Assert.assertEquals(43, con.getCounter());


        } finally {
            // release the connection to the pool ....
            if (con != null) {
                con.close();
            }
        }
    }

    public void testDecorationConnections() throws Exception {
        // instanciate a connection pool
        PooledPhynixxManagedConnectionFactory factory = createConnectionFactory(5);
        EventListenerPhynixx eventListener = new EventListenerPhynixx();
        factory.addConnectionProxyDecorator(eventListener);

        IReferenceConnection con = null;
        try {
            // get a connection ....
            con = (IReferenceConnection) factory.getConnection();

            con.setInitialCounter(43);

            con.incCounter(6);
            con.incCounter(-3);

            // increments are performed during commit
            Assert.assertEquals(43, con.getCounter());

            // rollback the connection ....
            con.rollback();
            Assert.assertEquals(43, con.getCounter());

            Assert.assertEquals(1, eventListener.getRollbackedConnections());
            Assert.assertEquals(0, eventListener.getCommittedConnections());
            Assert.assertEquals(0, eventListener.getRecoveredConnections());


        } finally {
            // release the connection to the pool ....
            if (con != null) {
                con.close();
            }
        }
    }

    public void testRecovery() throws Exception {
        // instanciate a connection pool
        PooledPhynixxManagedConnectionFactory factory = createConnectionFactory(5);
        IReferenceConnection con = null;
        try {
            // get a connection ....
            con = (IReferenceConnection) factory.getConnection();

            con.setInitialCounter(43);

            con.incCounter(6);
            con.incCounter(-3);
            con.incCounter(ReferenceConnection.ERRONEOUS_INC);
            con.incCounter(7);

            // increments are performed during commit
            Assert.assertEquals(43, con.getCounter());

            // close the factory and leave the connection in a recoverable state
            // rollback the connection ....
            factory.close();
        } catch (Exception e) {
            throw e;
        }

        // instanciate a new connection pool

        factory = createConnectionFactory(5);
        EventListenerPhynixx eventListener = new EventListenerPhynixx();
        factory.addConnectionProxyDecorator(eventListener);

        factory.recover(null);

        Assert.assertEquals(0, eventListener.getRollbackedConnections());
        Assert.assertEquals(0, eventListener.getCommittedConnections());
        Assert.assertEquals(1, eventListener.getRecoveredConnections());

    }

    private PooledPhynixxManagedConnectionFactory createConnectionFactory(int maxActiveConnections) throws Exception {

        GenericObjectPoolConfig cfg = new GenericObjectPoolConfig();
        cfg.setMaxTotal(maxActiveConnections);
        PooledPhynixxManagedConnectionFactory factory = new PooledPhynixxManagedConnectionFactory(new ReferenceConnectionFactory(), cfg);

        IDataLoggerFactory loggerFactory = new FileChannelDataLoggerFactory("reference", this.tmpDirectory.getDirectory());
        //IDataLoggerFactory loggerFactory= new HowlLoggerFactory("reference", this.loadHowlConfig(tmpDirectory.getDirectory()));

        factory.setLoggerSystemStrategy(new LoggerPerTransactionStrategy(loggerFactory));
        return factory;
    }

    private PhynixxManagedConnectionFactory createConnectionFactory() throws Exception {

        PhynixxManagedConnectionFactory factory = new PhynixxManagedConnectionFactory(new ReferenceConnectionFactory());
        IDataLoggerFactory loggerFactory = new FileChannelDataLoggerFactory("reference", this.tmpDirectory.getDirectory());
        factory.setLoggerSystemStrategy(new LoggerPerTransactionStrategy(loggerFactory));


        return factory;
    }


    /**
     * this setting for the logger system
     *
     * @return
     * @throws Exception
     */
    private Properties loadHowlConfig(File loggingDirectory) throws Exception {
        Properties howlprop = new Properties();
        howlprop.put("listConfig", "false");
        howlprop.put("bufferSize", "32");
        howlprop.put("minBuffers", "1");
        howlprop.put("maxBuffers", "1");
        howlprop.put("maxBlocksPerFile", "100");
        howlprop.put("logFileDir", loggingDirectory.getAbsolutePath());
        howlprop.put("logFileName", "test1");
        howlprop.put("maxLogFiles", "2");

        return howlprop;
    }


}
